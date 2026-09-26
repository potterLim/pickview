// Rebuild the original demo films. Shipped MP4s let normal startup avoid rendering dependencies.
const { createCanvas, loadImage, GlobalFonts } = require(process.env.CANVAS_MODULE ?? '@napi-rs/canvas');
const { readFileSync, writeFileSync, mkdirSync, existsSync } = require('node:fs');
const { resolve } = require('node:path');
const { spawnSync } = require('node:child_process');
const root = resolve(__dirname, '..');
const catalog = JSON.parse(readFileSync(resolve(root, 'content/demo-catalog.json'), 'utf8'));
const scratch = resolve(root, 'backend/.local/render');
const output = resolve(root, 'content/media');
const ffmpeg = process.env.FFMPEG_PATH ?? 'ffmpeg';
mkdirSync(scratch, { recursive: true });
mkdirSync(output, { recursive: true });
if (process.env.DEMO_FONT) { GlobalFonts.registerFromPath(process.env.DEMO_FONT, 'Demo'); }

function renderText(context, text, x, y, size, width, color, weight = 400, lineHeight = size * 1.5) {
    context.font = `${weight} ${size}px Demo, sans-serif`;
    context.fillStyle = color;
    let baseline = y;
    for (const paragraph of text.split('\n')) {
        let line = '';
        for (const character of paragraph) {
            if (context.measureText(line + character).width > width && line) {
                context.fillText(line, x, baseline);
                baseline += lineHeight;
                line = '';
            }
            line += character;
        }
        context.fillText(line, x, baseline);
        baseline += lineHeight;
    }
    return baseline;
}

function coverImage(context, image, x, y, width, height) {
    const scale = Math.max(width / image.width, height / image.height);
    const sourceWidth = width / scale;
    const sourceHeight = height / scale;
    context.drawImage(image, (image.width - sourceWidth) / 2, (image.height - sourceHeight) / 2, sourceWidth, sourceHeight, x, y, width, height);
}

function drawScene(item, index, image) {
    const canvas = createCanvas(1280, 720);
    const context = canvas.getContext('2d');
    const dark = item.category === 'COMEDY';
    const background = dark ? '#22182B' : item.category === 'FINANCE' ? '#EEF2E8' : '#F6F2ED';
    const ink = dark ? '#FFF3E3' : '#252631';
    const muted = dark ? '#C5B6CE' : '#626372';
    const accent = dark ? '#EABD85' : item.category === 'FINANCE' ? '#41644B' : '#7256E8';
    context.fillStyle = background;
    context.fillRect(0, 0, 1280, 720);
    coverImage(context, image, 770, 0, 510, 720);
    context.fillStyle = accent;
    context.fillRect(64, 57, 32, 4);
    renderText(context, 'PickView Originals', 112, 70, 22, 620, ink, 700);
    renderText(context, item.title, 64, 120, 18, 630, muted);
    const content = item.outcomes[index - 1];
    if (index === 0 || index === 4) {
        renderText(context, index === 0 ? item.intro : item.closing, 64, 282, 55, 650, ink, 800, 84);
        renderText(context, index === 0 ? '48초, 작은 발견을 위한 시간.' : '좋아하는 순간을 골라보세요.', 68, 504, 24, 620, muted);
    } else {
        renderText(context, `0${index}`, 64, 240, 66, 620, accent, 800);
        const next = renderText(context, content.title, 64, 325, 40, 650, ink, 800, 58);
        renderText(context, content.body, 64, Math.max(414, next + 28), 27, 650, muted, 400, 46);
    }
    context.fillStyle = dark ? '#524359' : '#DCD9DD';
    context.fillRect(64, 646, 628, 3);
    context.fillStyle = accent;
    context.fillRect(64, 646, 628 * (index + 1) / 5, 3);
    renderText(context, '스튜디오 온 · 오리지널 모션 쇼트', 64, 688, 16, 550, muted);
    renderText(context, `${index + 1} / 5`, 640, 688, 16, 110, muted);
    return canvas.toBuffer('image/png');
}

function createSoundtrack(path) {
    const sampleRate = 44100;
    const count = sampleRate * 48;
    const data = Buffer.alloc(44 + count * 2);
    data.write('RIFF', 0); data.writeUInt32LE(data.length - 8, 4); data.write('WAVEfmt ', 8);
    data.writeUInt32LE(16, 16); data.writeUInt16LE(1, 20); data.writeUInt16LE(1, 22);
    data.writeUInt32LE(sampleRate, 24); data.writeUInt32LE(sampleRate * 2, 28);
    data.writeUInt16LE(2, 32); data.writeUInt16LE(16, 34); data.write('data', 36); data.writeUInt32LE(count * 2, 40);
    const notes = [261.63, 329.63, 392, 493.88, 440, 392, 329.63, 293.66];
    for (let index = 0; index < count; index++) {
        const time = index / sampleRate;
        const note = notes[Math.floor(time / 1.5) % notes.length];
        const age = time % 1.5;
        const envelope = Math.min(age * 20, 1) * Math.exp(-age * 2.7);
        const fade = Math.min(time / 2, (48 - time) / 3, 1);
        const bell = (Math.sin(2 * Math.PI * note * time) + .18 * Math.sin(4 * Math.PI * note * time)) * envelope;
        const pad = .13 * Math.sin(2 * Math.PI * 130.815 * time) * Math.sin(Math.PI * time / 12) ** 2;
        data.writeInt16LE(Math.round((bell + pad) * fade * 1900), 44 + index * 2);
    }
    writeFileSync(path, data);
}

function encode(arguments_) {
    const result = spawnSync(ffmpeg, ['-hide_banner', '-loglevel', 'error', '-y', ...arguments_], { stdio: 'inherit', windowsHide: true });
    if (result.error || result.status !== 0) { throw result.error ?? new Error(`FFmpeg failed: ${result.status}`); }
}

(async () => {
    const soundtrack = resolve(scratch, 'original-ambient.wav');
    createSoundtrack(soundtrack);
    for (const item of catalog) {
        const destination = resolve(output, `sample-${item.id}.mp4`);
        if (existsSync(destination) && !process.argv.includes('--force')) { continue; }
        const picture = await loadImage(resolve(root, `frontend/assets/${item.thumbnail}.png`));
        const durations = [6, 12, 12, 12, 6];
        const segments = [];
        for (let index = 0; index < durations.length; index++) {
            const poster = resolve(scratch, `${item.id}-${index}.png`);
            const segment = resolve(scratch, `${item.id}-${index}.mp4`);
            writeFileSync(poster, drawScene(item, index, picture));
            encode(['-loop', '1', '-framerate', '24', '-i', poster, '-t', String(durations[index]), '-vf',
                `zoompan=z='min(zoom+0.00006,1.018)':x='iw/2-iw/zoom/2':y='ih/2-ih/zoom/2':d=1:s=1280x720:fps=24,fade=t=in:st=0:d=0.3,fade=t=out:st=${durations[index] - .3}:d=0.3`,
                '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '25', '-pix_fmt', 'yuv420p', '-threads', '3', segment]);
            segments.push(`file '${segment.replaceAll('\\', '/').replaceAll("'", "'\\''")}'`);
        }
        const concat = resolve(scratch, `${item.id}-concat.txt`);
        writeFileSync(concat, segments.join('\n'));
        encode(['-f', 'concat', '-safe', '0', '-i', concat, '-i', soundtrack, '-c:v', 'copy', '-c:a', 'aac', '-b:a', '96k', '-shortest', '-movflags', '+faststart', destination]);
        encode(['-i', destination, '-t', '9', '-c:v', 'libx264', '-preset', 'veryfast', '-crf', '25', '-c:a', 'aac', '-movflags', '+faststart', resolve(output, `sample-${item.id}-preview.mp4`)]);
        console.log(`Rendered ${item.id}: 48 seconds + separate 9-second preview`);
    }
})().catch(error => { console.error(error); process.exitCode = 1; });
