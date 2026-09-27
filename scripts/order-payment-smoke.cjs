const { chromium } = require(process.env.PLAYWRIGHT_MODULE ?? "playwright");
const assert = require("node:assert/strict");
const { randomUUID } = require("node:crypto");
const { mkdirSync } = require("node:fs");
const { resolve } = require("node:path");

const apiUrl = process.env.API_URL ?? "http://localhost:8080/api";
const password = process.env.DEMO_PASSWORD ?? "PickView-demo-2026!";
const output = resolve(process.env.QA_OUTPUT_DIR ?? require("node:os").tmpdir(), "pickview-order-qa");

async function call(path, token, body) {
    const response = await fetch(apiUrl + path, {
        method: body === undefined ? "GET" : "POST",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    const bodyText = await response.text();
    assert.equal(response.status, 200, `${path}: ${bodyText}`);
    return bodyText ? JSON.parse(bodyText) : null;
}

function orderCard(page, order) {
    return page.locator("div")
        .filter({ has: page.getByText(new RegExp(`주문 ${order.id.slice(0, 8)} ·`)) })
        .filter({ has: page.getByText("주문 당시 결제 금액", { exact: true }) })
        .last();
}

async function approveRefund(buyer, support, line) {
    await call("/tickets", buyer, { kind: "REFUND", targetId: line.id, message: "Order summary regression check" });
    const ticket = (await call("/tickets", buyer)).find((item) => item.targetId === line.id);
    await call(`/admin/tickets/${ticket.id}`, support, { approve: true, reply: "Approved for local demo QA" });
}

async function reloadOrders(page) {
    await page.getByRole("button", { name: "내 라이브러리", exact: true }).click();
    await page.getByRole("button", { name: "주문 내역", exact: true }).click();
}

(async () => {
    mkdirSync(output, { recursive: true });
    const email = `order-qa-${randomUUID()}@example.test`;
    const buyer = (await call("/auth/register", "", { email, password, name: "Order QA", isAdult: true })).token;
    const support = (await call("/auth/login", "", { email: "support@pickview.demo", password })).token;
    for (const outcome of ["FAILED", "CANCELED"]) {
        await call("/checkout", buyer, { productIds: ["video-1"], requestKey: randomUUID(), channel: "CARD", outcome });
    }
    const order = await call("/checkout", buyer, {
        productIds: ["video-1", "video-4"],
        requestKey: randomUUID(),
        channel: "CARD",
        outcome: "SUCCESS",
    });
    const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH, headless: true });
    try {
        const page = await browser.newPage({ viewport: { width: 1920, height: 1080 } });
        const errors = [];
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
            if (message.type() === "error") {
                errors.push(message.text());
            }
        });
        const webUrl = process.env.WEB_URL ?? "http://localhost:8081";
        await page.goto(webUrl, { waitUntil: "networkidle" });
        assert.equal(new URL(page.url()).origin, new URL(webUrl).origin);
        assert.ok(await page.title());
        await page.getByRole("button", { name: "로그인", exact: true }).click();
        await page.getByRole("textbox", { name: "이메일", exact: true }).fill(email);
        await page.getByRole("button", { name: "로그인", exact: true }).last().click();
        await page.getByRole("button", { name: "주문 내역", exact: true }).click();
        await page.getByText("결제에 실패하여 결제된 금액이 없습니다.", { exact: true }).waitFor();
        await page.getByText("결제가 취소되어 결제된 금액이 없습니다.", { exact: true }).waitFor();
        assert.equal(await page.getByText("결제된 금액", { exact: true }).count(), 2);
        const card = orderCard(page, order);
        await card.getByText("구매 완료", { exact: true }).waitFor();
        assert.equal(await card.getByText("환불 완료 금액", { exact: true }).count(), 0);
        await page.screenshot({ path: resolve(output, "01-payment-states.png") });

        await approveRefund(buyer, support, order.lines[0]);
        await reloadOrders(page);
        await card.getByText("일부 환불", { exact: true }).waitFor();
        const refundedRow = card.getByText("환불 완료 금액", { exact: true }).locator("..");
        assert.ok((await refundedRow.innerText()).includes(order.lines[0].priceWon.toLocaleString("en-US")));
        assert.equal(await card.getByRole("button", { name: "환불 요청", exact: true }).count(), 1);
        await page.screenshot({ path: resolve(output, "02-partial-refund.png") });

        await approveRefund(buyer, support, order.lines[1]);
        await reloadOrders(page);
        await card.getByText("환불 완료", { exact: true }).first().waitFor();
        assert.ok((await refundedRow.innerText()).includes(order.totalWon.toLocaleString("en-US")));
        assert.equal(await card.getByRole("button", { name: "환불 요청", exact: true }).count(), 0);
        assert.equal(await card.getByText(/결제되지|결제된 금액이 없습니다/).count(), 0);
        await page.screenshot({ path: resolve(output, "03-full-refund.png") });
        for (const width of [390, 768]) {
            await page.setViewportSize({ width, height: width === 390 ? 844 : 1024 });
            await page.waitForFunction(() => document.documentElement.scrollWidth === innerWidth);
            await page.screenshot({ path: resolve(output, `04-refund-${width}.png`) });
        }
        await page.getByRole("button", { name: "Change language", exact: true }).click();
        await page.getByText("Original payment amount", { exact: true }).waitFor();
        await page.getByText("Refunded amount", { exact: true }).waitFor();
        await page.getByText("Payment failed. No payment was made.", { exact: true }).waitFor();
        await page.getByText("Payment was canceled. No payment was made.", { exact: true }).waitFor();
        assert.deepEqual(errors, []);
        console.log("PASS failed/canceled/successful payments, partial/full refunds, retained original totals, Korean/English and responsive order summaries");
    } finally {
        await browser.close();
    }
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
