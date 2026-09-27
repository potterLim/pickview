const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const { test } = require("node:test");
const ts = require("typescript");

function loadCore(fetch) {
    const cache = new Map();
    function load(name) {
        if (cache.has(name)) {
            return cache.get(name);
        }
        const file = path.join(__dirname, "../src/core", `${name}.ts`);
        const source = ts.transpileModule(readFileSync(file, "utf8"), {
            compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
        }).outputText;
        const exports = {};
        cache.set(name, exports);
        vm.runInNewContext(source, {
            exports,
            require: (moduleName) => moduleName === "react-native" ? { Platform: { OS: "web" } } : load(moduleName.slice(2)),
            fetch,
            process: { env: {} },
            Response,
            AbortController,
        }, { filename: file });
        return exports;
    }
    return load;
}

function product() {
    return {
        id: "video", sellerId: "seller", sellerName: "Seller", title: "Video", description: "Demo",
        category: "EDUCATION", priceWon: 3000, termDays: 30, status: "DRAFT", thumbnail: "studio",
        durationSeconds: 30, kind: "VIDEO", videoIds: ["video"], rating: 0, reviewCount: 0,
        sales: 0, createdAt: 1, blocked: false, tags: "", isDemo: false,
    };
}

test("API rejects malformed contracts and retains status for null error bodies", async () => {
    const load = loadCore(async () => new Response("null", { status: 401 }));
    await assert.rejects(load("api").request("/me", "token"), (error) => error.status === 401);
    const contracts = load("contracts");
    assert.throws(() => contracts.decodeProduct({ ...product(), id: 17 }));
    assert.throws(() => contracts.decodeProduct({ ...product(), kind: "UNKNOWN" }));
    assert.equal(contracts.decodeProduct(product()).id, "video");
    assert.equal(contracts.decodeTicket({
        id: "ticket", userId: "buyer", targetId: "video", recipientId: "seller", kind: "INQUIRY",
        message: "Question", status: "RESOLVED", reply: "Answer", createdAt: 1,
    }).status, "RESOLVED");
});

test("logout clears private state even when persistent storage fails", async () => {
    const load = loadCore();
    let signedIn = true;
    await assert.rejects(load("session").clearLocalSession(
        () => { signedIn = false; },
        async () => { throw new Error("Storage unavailable"); },
    ));
    assert.equal(signedIn, false);
});

test("progress writes remain ordered across callers and recover after failure", async () => {
    const positions = [];
    let releaseFirst;
    const firstResponse = new Promise((resolve) => { releaseFirst = resolve; });
    const load = loadCore(async (_url, options) => {
        positions.push(JSON.parse(options.body).numberValue);
        return positions.length === 1 ? firstResponse : new Response(null, { status: 204 });
    });
    const save = load("progress").savePlaybackProgress;
    const first = save("token", "video", 12);
    const second = save("token", "video", 24);
    await new Promise((resolve) => setImmediate(resolve));
    assert.deepEqual(positions, [12]);
    releaseFirst(new Response("null", { status: 503 }));
    await assert.rejects(first);
    await second;
    assert.deepEqual(positions, [12, 24]);
});

test("retry after media failure updates the already-created product", async () => {
    const methods = [];
    const load = loadCore(async (_url, options) => {
        methods.push(options.method);
        return options.method === "POST" ? Response.json(product()) : new Response(null, { status: 204 });
    });
    const draft = new (load("ProductDraft").ProductDraft)(null);
    assert.equal(await draft.save(product(), "token"), "video");
    // An independently failed upload does not discard the server-side product identity.
    assert.equal(await draft.save(product(), "token"), "video");
    assert.deepEqual(methods, ["POST", "PUT"]);
});
