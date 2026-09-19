package com.slashtracks.client.demo;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.slashtracks.SlashTracks;
import net.minecraft.client.Minecraft;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Dev-only: records the game's own frames into an MP4 by piping raw RGBA to ffmpeg. Frames are
 * emitted on a fixed grid of scene time (a frame is repeated if the game renders slower), so the
 * video plays at true game speed regardless of screen scaling, window position or focus. Scenes run
 * the game in slow motion so that every output frame is a freshly rendered one.
 */
final class DemoRecorder {

    private static final int POOL = 6;

    private final int width;
    private final int height;
    private final long frameNanos;
    private final Process ffmpeg;
    private final BlockingQueue<ByteBuffer> free = new ArrayBlockingQueue<>(POOL);
    private final BlockingQueue<ByteBuffer> full = new ArrayBlockingQueue<>(POOL);
    private final Thread writer;
    private final long start;
    private long emitted;
    private volatile boolean closing;
    private int repeated;

    private DemoRecorder(int width, int height, int fps, Process ffmpeg) {
        this.width = width;
        this.height = height;
        this.frameNanos = 1_000_000_000L / fps;
        this.ffmpeg = ffmpeg;
        for (int i = 0; i < POOL; i++) free.add(BufferUtils.createByteBuffer(width * height * 4));
        OutputStream out = ffmpeg.getOutputStream();
        WritableByteChannel ch = Channels.newChannel(out);
        writer = new Thread(() -> {
            try {
                while (!closing || !full.isEmpty()) {
                    ByteBuffer b = full.poll(50, TimeUnit.MILLISECONDS);
                    if (b == null) continue;
                    b.rewind();
                    while (b.hasRemaining()) ch.write(b);
                    free.add(b);
                }
                out.close();
            } catch (IOException | InterruptedException e) {
                SlashTracks.LOG.error("[demo] recorder writer failed", e);
            }
        }, "SlashTracks demo recorder");
        writer.setDaemon(true);
        writer.start();
        start = System.nanoTime();
    }

    static DemoRecorder start(Path ffmpegExe, Path output, int fps) throws IOException {
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        int w = target.width, h = target.height;
        List<String> cmd = List.of(ffmpegExe.toString(), "-y", "-loglevel", "error",
                "-f", "rawvideo", "-pix_fmt", "rgba", "-s", w + "x" + h, "-framerate", Integer.toString(fps), "-i", "-",
                "-vf", "vflip" + (w != 1920 || h != 1080 ? ",scale=1920:1080:flags=lanczos" : ""),
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "14", "-pix_fmt", "yuv420p",
                "-movflags", "+faststart", output.toString());
        output.getParent().toFile().mkdirs();
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true)
                .redirectOutput(output.resolveSibling(output.getFileName() + ".ffmpeg.log").toFile()).start();
        SlashTracks.LOG.info("[demo] recording {}x{} @{} fps -> {}", w, h, fps, output);
        return new DemoRecorder(w, h, fps, p);
    }

    /**
     * End of a rendered frame (render thread): emit it into every frame slot that scene time
     * {@code t} (seconds of game time) has passed.
     */
    void onFrame(double t) {
        long due = (long) (t * 1_000_000_000L / frameNanos) + 1;
        if (due <= emitted) return;
        RenderTarget target = Minecraft.getInstance().getMainRenderTarget();
        if (target.width != width || target.height != height) return; // resized mid-take; skip
        ByteBuffer b = take();
        if (b == null) return;
        b.clear();
        RenderSystem.bindTexture(target.getColorTextureId());
        GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, b);
        b.rewind();
        while (emitted < due) {
            if (emitted < due - 1) repeated++;
            ByteBuffer frame = emitted == due - 1 ? b : copy(b);
            if (frame == null) break;
            full.add(frame);
            emitted++;
        }
    }

    private ByteBuffer copy(ByteBuffer src) {
        ByteBuffer c = take();
        if (c == null) return null;
        c.clear();
        src.rewind();
        c.put(src);
        src.rewind();
        c.flip();
        return c;
    }

    private ByteBuffer take() {
        try {
            return free.poll(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return null;
        }
    }

    void stop() {
        closing = true;
        try {
            writer.join(30_000);
            ffmpeg.waitFor(60, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
        }
        SlashTracks.LOG.info("[demo] recording finished: {} frames ({} repeated to keep real time), ffmpeg exit {}",
                emitted, repeated, ffmpeg.isAlive() ? "still running" : ffmpeg.exitValue());
    }
}
