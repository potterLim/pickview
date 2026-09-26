import { mkdirSync, existsSync, readdirSync, copyFileSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "..");
const media = resolve(root, "backend/.local/media");
const ffmpeg = process.env.FFMPEG_PATH ?? "ffmpeg";
mkdirSync(media, { recursive: true });
const originals = resolve(root, "content/media");
if (existsSync(originals)) {
    for (const file of readdirSync(originals).filter(name => /^sample-video-\d(-preview)?\.mp4$/.test(name))) {
        const destination = resolve(media, file);
        if (!existsSync(destination)) { copyFileSync(resolve(originals, file), destination); }
    }
}

function generate(filename, duration) {
    const path = resolve(media, filename);
    if (existsSync(path)) { console.log(`Already prepared: ${filename}`); return; }
    const result = spawnSync(ffmpeg, ["-hide_banner", "-loglevel", "error", "-f", "lavfi", "-i",
        "testsrc2=size=960x540:rate=24", "-f", "lavfi", "-i", "sine=frequency=440:sample_rate=44100",
        "-t", String(duration), "-c:v", "libx264", "-pix_fmt", "yuv420p", "-c:a", "aac", "-movflags", "+faststart", path],
    { stdio: "inherit", windowsHide: true });
    if (result.error || result.status !== 0) { throw result.error ?? new Error(`FFmpeg exited ${result.status}`); }
    console.log(`Prepared: ${filename}`);
}

generate("demo.mp4", 30);
generate("demo-preview.mp4", 6);
