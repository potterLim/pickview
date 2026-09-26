# Original demo content

The six bundled videos are original 48-second photo/motion-graphics shorts, each with a separate nine-second preview. They are demo content, not recordings of professional courses. Korean on-screen text and a procedurally synthesized ambient soundtrack are included; English descriptions are available in the app.

`demo-catalog.json` is the shared source for backend sample metadata, frontend descriptions, and the video renderer. Bundled MP4 files run without rendering dependencies. `node scripts/prepare-media.mjs` copies them into local private media storage. Existing uploads and purchase grants are preserved by the one-time sample upgrade.

To regenerate, run `node scripts/render-demo.cjs --force` from the repository root with `FFMPEG_PATH`, `CANVAS_MODULE` (the @napi-rs/canvas module), and `DEMO_FONT` (a Korean-capable font file) configured. Rendering scratch files stay in `backend/.local/render`. The separate test-pattern files created by prepare-media are API upload fixtures, not public catalog content.

Photos were generated for PickView using Image Gen: pottery workshop, recording studio, sage-toned finance desk, and plum-toned microphone stage. Text, composition, transitions and soundtrack were created for this demo. No third-party music or instructor identity is used.

ETF terminology was checked against the SEC's educational [ETF bulletin](https://www.investor.gov/introduction-investing/general-resources/news-alerts/alerts-bulletins/investor-bulletins-24). The short introduces ordinary portfolio ETFs; it does not cover every specialized ETF structure and makes no investment recommendations.
