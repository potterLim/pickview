const { chromium } = require(process.env.PLAYWRIGHT_MODULE ?? "playwright");
const { mkdirSync } = require("node:fs");
const { resolve } = require("node:path");
const assert = require("node:assert/strict");
const output = resolve(process.env.QA_OUTPUT_DIR ?? require("node:os").tmpdir(), "pickview-browser-qa");

async function login(page, email) {
    await page.getByRole("button", { name: "로그인", exact: true }).click();
    await page.getByRole("textbox", { name: "이메일", exact: true }).fill(email);
    await page.getByRole("button", { name: "로그인", exact: true }).last().click();
    await page.getByRole("button", { name: "내 계정", exact: true }).waitFor();
}

async function checkOverflow(page, width, height) {
    await page.setViewportSize({ width, height });
    // React Native updates responsive layout after the browser resize event.
    await page.waitForFunction(() => document.documentElement.scrollWidth === innerWidth);
    const dimensions = await page.evaluate(() => ({
        scroll: document.documentElement.scrollWidth,
        viewport: innerWidth,
    }));
    assert.equal(dimensions.scroll, dimensions.viewport, `Overflow at ${width}px`);
}

(async () => {
    mkdirSync(output, { recursive: true });
    const browser = await chromium.launch({ executablePath: process.env.CHROME_PATH, headless: true });
    try {
        const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
        const page = await context.newPage();
        const errors = [];
        page.on("pageerror", (error) => errors.push(error.message));
        await page.goto(process.env.WEB_URL ?? "http://localhost:8081", { waitUntil: "networkidle", timeout: 60000 });
        await page.getByRole("button", { name: "내 손으로 만드는 첫 번째 도자기", exact: true }).waitFor();
        await page.screenshot({ path: resolve(output, "desktop.png") });
        await page.getByRole("button", { name: "로그인", exact: true }).click();
        await page.getByRole("button", { name: "새 계정 만들기", exact: true }).click();
        await page.getByRole("textbox", { name: "이메일", exact: true }).fill(`browser-${Date.now()}@example.test`);
        await page.getByRole("textbox", { name: "이름", exact: true }).fill("Browser QA");
        await page.getByRole("button", { name: "성인임을 확인합니다 (데모)", exact: true }).click();
        await page.getByRole("button", { name: "회원가입", exact: true }).click();
        await page.getByRole("button", { name: "내 계정", exact: true }).waitFor();
        await page.getByRole("button", { name: "내 손으로 만드는 첫 번째 도자기", exact: true }).click();
        await page.getByRole("button", { name: "장바구니 담기", exact: true }).click();
        await page.getByRole("button", { name: "장바구니로 이동", exact: true }).waitFor();
        await page.getByRole("button", { name: "장바구니", exact: true }).click();
        await page.getByRole("button", { name: /모의 결제하기$/ }).click();
        await page.getByText("좋은 선택이에요. 이제 만나볼까요?", { exact: true }).waitFor();
        await page.getByRole("button", { name: "라이브러리로 이동", exact: true }).click();
        await page.getByText("나의 취향으로 채운 작은 세계", { exact: true }).waitFor();
        await page.getByRole("button", { name: "주문 내역", exact: true }).click();
        const refundReason = page.getByRole("textbox", { name: "환불 요청 사유", exact: true });
        await page.getByRole("button", { name: "환불 요청", exact: true }).waitFor();
        assert.equal(await refundReason.count(), 0, "Order history must not open the refund form");
        await page.getByRole("button", { name: "환불 요청", exact: true }).click();
        assert.equal(await page.getByRole("button", { name: "환불 요청 제출", exact: true }).isDisabled(), true);
        await refundReason.fill("Accidental demo purchase");
        await page.getByRole("button", { name: "취소", exact: true }).click();
        assert.equal(await refundReason.count(), 0);
        await page.getByRole("button", { name: "환불 요청", exact: true }).click();
        assert.equal(await refundReason.inputValue(), "", "Canceled reasons must not leak into a new request");
        await refundReason.fill("Accidental demo purchase");
        await page.getByRole("button", { name: "환불 요청 제출", exact: true }).click();
        await page.getByText("환불 요청 검토 중", { exact: true }).waitFor();
        assert.equal(await page.getByRole("button", { name: "환불 요청", exact: true }).count(), 0);
        await page.getByRole("button", { name: "내 라이브러리", exact: true }).click();
        await page.getByRole("button", { name: "내 손으로 만드는 첫 번째 도자기", exact: true }).click();
        await page.getByRole("button", { name: "본편 시청", exact: true }).click();
        const video = page.locator("video");
        await video.waitFor();
        await video.evaluate(async (element) => {
            element.muted = true;
            await element.play();
        });
        await page.waitForFunction(() => document.querySelector("video")?.currentTime > 2);
        await page.getByRole("button", { name: "+10s", exact: true }).click();
        await page.waitForFunction(() => document.querySelector("video")?.currentTime > 10);
        await page.getByRole("button", { name: "1×", exact: true }).click();
        assert.equal(await video.evaluate((element) => element.playbackRate), 1.25);
        await page.screenshot({ path: resolve(output, "playback.png") });
        await page.getByRole("button", { name: "탐색", exact: true }).click();
        await page.getByRole("button", { name: "Change language", exact: true }).click();
        await page.getByText("Your next discovery. No subscription.", { exact: true }).waitFor();
        await checkOverflow(page, 390, 844);
        await page.screenshot({ path: resolve(output, "mobile.png") });
        await checkOverflow(page, 768, 1024);
        await page.screenshot({ path: resolve(output, "tablet.png") });
        await checkOverflow(page, 1440, 1000);
        await page.getByRole("button", { name: "Settings", exact: true }).click();
        await page.getByRole("radio", { name: "한국어", exact: true }).click();
        await page.getByRole("checkbox", { name: "코미디", exact: true }).click();
        await page.getByRole("button", { name: "설정 저장", exact: true }).click();
        await page.getByText("설정을 저장했어요.", { exact: true }).waitFor();
        assert.equal(await page.getByRole("button", { name: "설정 저장", exact: true }).isDisabled(), true);
        await page.getByRole("button", { name: "탐색", exact: true }).click();
        await page.getByRole("button", { name: "설정", exact: true }).click();
        assert.equal(
            await page.getByRole("checkbox", { name: "코미디", exact: true }).getAttribute("aria-checked"),
            "true",
        );
        await page.getByRole("button", { name: "로그아웃", exact: true }).click();
        await login(page, "admin@pickview.demo");
        await page.getByRole("button", { name: "운영 관리", exact: true }).click();
        await page.getByRole("button", { name: "업로드 영상 확인", exact: true }).first().click();
        await page.locator("video").waitFor();
        await page.locator("video").evaluate(async (element) => {
            element.muted = true;
            await element.play();
        });
        await page.waitForFunction(() => document.querySelector("video")?.currentTime > 1);
        await page.screenshot({ path: resolve(output, "moderation.png") });
        assert.deepEqual(errors, []);
        console.log(
            "PASS browser registration, cart, purchase, explicit refund request, playback, seek, speed, locale, responsive layout, account switch and moderation",
        );
    } finally {
        await browser.close();
    }
})().catch((error) => {
    console.error(error);
    process.exitCode = 1;
});
