import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import { randomUUID } from "node:crypto";

const base = process.env.API_URL ?? "http://localhost:8080/api";
const password = process.env.DEMO_PASSWORD ?? "PickView-demo-2026!";
const results = [];
async function call(path, token = "", body, method = body === undefined ? "GET" : "POST", expected = 200) {
    const response = await fetch(base + path, { method, headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) }, body: body === undefined ? undefined : JSON.stringify(body) });
    const text = await response.text();
    assert.equal(response.status, expected, `${method} ${path}: ${response.status} ${text}`);
    return text ? JSON.parse(text) : null;
}
async function login(role) { return (await call("/auth/login", "", { email: `${role}@pickview.demo`, password })).token; }
async function register(name) { return (await call("/auth/register", "", { email: `${name.replaceAll(" ", "-")}-${randomUUID()}@example.test`, password, name, isAdult: true })).token; }
async function check(name, action) { await action(); results.push(name); console.log(`PASS ${name}`); }

const buyer = await register("QA buyer");
const outsider = await register("QA outsider");
const seller = await login("seller");
const admin = await login("admin");
const support = await login("support");
const finance = await login("finance");
let order;
let playback;
await check("anonymous access is restricted to public catalog", async () => {
    assert.ok((await call("/public/products")).length >= 6);
    await call("/library", "", undefined, "GET", 401);
    await call("/media/ticket/video-1", buyer, {}, "POST", 403);
});
await check("failed checkout does not grant access", async () => {
    await call("/checkout", buyer, { productIds: ["video-1"], requestKey: randomUUID(), channel: "CARD", outcome: "FAILED" });
    assert.equal((await call("/library", buyer)).length, 0);
});
await check("checkout is idempotent and creates a shared library", async () => {
    const input = { productIds: ["video-1"], requestKey: randomUUID(), channel: "CARD", outcome: "SUCCESS" };
    order = await call("/checkout", buyer, input);
    assert.equal((await call("/checkout", buyer, input)).id, order.id);
    assert.equal(order.totalWon, 3000);
    assert.equal((await call("/library", buyer)).filter(item => item.product.id === "video-1").length, 1);
    playback = (await call("/media/ticket/video-1", buyer, {})).path;
    const response = await fetch(base.replace(/\/api$/, "") + playback, { headers: { Range: "bytes=0-255" } });
    assert.equal(response.status, 206);
    assert.equal((await response.arrayBuffer()).byteLength, 256);
});
await check("duplicate content and outsider playback are rejected", async () => {
    await call("/checkout", buyer, { productIds: ["video-1"], requestKey: randomUUID(), channel: "CARD", outcome: "SUCCESS" }, "POST", 409);
    await call("/media/ticket/video-1", outsider, {}, "POST", 403);
    await call("/checkout", outsider, { productIds: ["bundle-1", "video-4"], requestKey: randomUUID(), channel: "CARD", outcome: "SUCCESS" }, "POST", 409);
});
await check("resume is persistent and reviews require purchase", async () => {
    await call("/activity", buyer, { kind: "PROGRESS", targetId: "video-1", content: "", numberValue: 12 }, "PUT");
    assert.equal((await call("/activity", buyer)).find(item => item.kind === "PROGRESS").numberValue, 12);
    await call("/activity", outsider, { kind: "REVIEW", targetId: "video-1", content: "Invalid", numberValue: 5 }, "PUT", 403);
    await call("/activity", buyer, { kind: "REVIEW", targetId: "video-1", content: "QA verified review", numberValue: 5 }, "PUT");
});
await check("withdrawal preserves an existing entitlement", async () => {
    try {
        await call("/seller/products/video-1/WITHDRAW", seller, {});
        assert.ok(!(await call("/public/products")).some(item => item.id === "video-1"));
        await call("/media/ticket/video-1", buyer, {});
    } finally { await call("/admin/products/video-1", admin, { decision: "APPROVE" }); }
});
await check("operator roles and seller ownership are enforced", async () => {
    await call("/admin/products/video-2", finance, { decision: "APPROVE" }, "POST", 403);
    await call("/admin/roles/buyer", support, { decision: "ADMIN" }, "POST", 403);
    await call("/seller/products/video-1/WITHDRAW", outsider, {}, "POST", 403);
    assert.equal((await call("/admin/dashboard", finance)).accounts.length, 0);
});
await check("refund revokes playback and updates seller ledger atomically", async () => {
    const lineId = order.lines[0].id;
    await call("/tickets", buyer, { kind: "REFUND", targetId: lineId, message: "QA refund" });
    const ticket = (await call("/tickets", buyer)).find(item => item.targetId === lineId);
    await call(`/admin/tickets/${ticket.id}`, support, { approve: true, reply: "Approved for demo test" });
    await call("/media/ticket/video-1", buyer, {}, "POST", 403);
    assert.equal((await fetch(base.replace(/\/api$/, "") + playback)).status, 403);
    assert.ok((await call("/seller/sales", seller)).find(line => line.id === lineId).refunded);
    assert.equal((await call("/orders", buyer)).find(item => item.id === order.id).status, "REFUNDED");
});
if (process.env.TEST_VIDEO) {
    await check("real upload creates a separate public preview and moderated product", async () => {
        const product = await call("/seller/products", seller, { title: "QA uploaded video", description: "Uploaded integration test", category: "EDUCATION", priceWon: 1000, termDays: 7, thumbnail: "studio", kind: "VIDEO", videoIds: [], hasRights: true });
        const body = new FormData();
        body.append("file", new Blob([await readFile(process.env.TEST_VIDEO)], { type: "video/mp4" }), "demo.mp4");
        body.append("previewSeconds", "5");
        const response = await fetch(`${base}/seller/products/${product.id}/upload`, { method: "POST", headers: { Authorization: `Bearer ${seller}` }, body });
        assert.equal(response.status, 200, await response.text());
        assert.ok(!(await call("/public/products")).some(item => item.id === product.id));
        await call(`/admin/products/${product.id}`, admin, { decision: "APPROVE" });
        assert.equal((await fetch(`${base}/public/preview/${product.id}`)).status, 200);
        await call(`/media/ticket/${product.id}`, outsider, {}, "POST", 403);
        await call(`/seller/products/${product.id}/WITHDRAW`, seller, {});
    });
}
console.log(JSON.stringify({ passed: results.length, checks: results }, null, 2));
