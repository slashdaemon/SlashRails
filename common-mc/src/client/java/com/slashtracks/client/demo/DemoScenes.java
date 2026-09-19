package com.slashtracks.client.demo;

import com.slashtracks.SlashTracks;
import com.slashtracks.run.RunService;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dev-only scripted demo shots for store galleries, enabled with {@code -Dslashtracks.demo=<scene>}
 * and a world opened by quick play. Each scene sets up its track, positions the camera, records a
 * fixed-length take to {@code demo/<scene>.mp4} (plus stills), then quits.
 *
 * <p>Scenes (all on the same 1:3 staircase, vanilla look, noon):
 * <ul>
 *   <li>{@code click} — elevated view; the zigzag turns into a curve.</li>
 *   <li>{@code ride-vanilla} / {@code ride-smooth} — first-person ride over the track.</li>
 *   <li>{@code preview} — over the shoulder: preview line, smooth, revert.</li>
 * </ul>
 */
public final class DemoScenes {

    private static final String SCENE = System.getProperty("slashtracks.demo", "");
    private static final Path FFMPEG = Path.of(System.getProperty("slashtracks.ffmpeg", "ffmpeg"));
    private static final int FPS = Integer.getInteger("slashtracks.demo.fps", 60);

    /** The fixture: a 1:3 staircase starting at (10, -60, 10) heading east. */
    private static final BlockPos TRACK_ORIGIN = new BlockPos(7, -60, 10);
    private static final BlockPos TRACK_FIRST = new BlockPos(10, -60, 10);
    private static final BlockPos TOOL_TARGET = new BlockPos(15, -60, 10);

    private record Event(double at, Runnable action) {
    }

    /** Camera pose at time t (seconds into the take); null keeps the camera as the game sets it. */
    private interface CameraPath {
        double[] at(double t);
    }

    private static int setupTicks = -1;
    private static final List<Runnable> SETUP = new ArrayList<>();
    private static final List<Event> EVENTS = new ArrayList<>();
    private static CameraPath camera;
    private static double duration;
    private static DemoRecorder recorder;
    private static long takeStart;
    private static int nextEvent;
    private static boolean done;

    private DemoScenes() {
    }

    public static boolean active() {
        return !SCENE.isEmpty();
    }

    // ---- hooks ----------------------------------------------------------------------------

    /** Client tick: run the setup steps, one every few ticks, then start the take. */
    public static void tick(Minecraft mc) {
        if (!active() || done || mc.player == null || mc.getSingleplayerServer() == null) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (setupTicks < 0) {
            define(mc);
            setupTicks = 0;
        }
        if (recorder != null) return;
        setupTicks++;
        int step = setupTicks / 10;
        if (setupTicks % 10 == 0 && step - 1 < SETUP.size()) {
            SETUP.get(step - 1).run();
        } else if (step - 1 >= SETUP.size() + 8) { // let chunk meshes settle
            startTake(mc);
        }
    }

    /** Start of each rendered frame: place the camera exactly on its path. */
    public static void beforeFrame(Minecraft mc) {
        if (recorder == null) return;
        holdLook(mc);
        if (camera == null || mc.player == null) return;
        double[] c = camera.at(elapsed());
        if (c == null) return;
        LocalPlayer p = mc.player;
        p.setPos(c[0], c[1], c[2]);
        p.xo = p.xOld = c[0];
        p.yo = p.yOld = c[1];
        p.zo = p.zOld = c[2];
        p.setYRot((float) c[3]);
        p.setXRot((float) c[4]);
        p.yRotO = p.getYRot();
        p.xRotO = p.getXRot();
        p.setYHeadRot(p.getYRot());
        p.yHeadRotO = p.getYRot();
    }

    /** End of each rendered frame: capture, fire due events, finish the take. */
    public static void afterFrame(Minecraft mc) {
        if (recorder == null) return;
        recorder.onFrame();
        double t = elapsed();
        while (nextEvent < EVENTS.size() && EVENTS.get(nextEvent).at() <= t) {
            EVENTS.get(nextEvent++).action().run();
        }
        if (t >= duration) {
            recorder.stop();
            recorder = null;
            done = true;
            SlashTracks.LOG.info("[demo] scene {} done", SCENE);
            mc.stop();
        }
    }

    private static double elapsed() {
        return (System.nanoTime() - takeStart) / 1e9;
    }

    private static void startTake(Minecraft mc) {
        try {
            nextEvent = 0;
            takeStart = System.nanoTime();
            recorder = DemoRecorder.start(FFMPEG, mc.gameDirectory.toPath().resolve("demo").resolve(SCENE + ".mp4"), FPS);
        } catch (Exception e) {
            SlashTracks.LOG.error("[demo] could not start recording", e);
            done = true;
            mc.stop();
        }
    }

    // ---- scenes ---------------------------------------------------------------------------

    private static void define(Minecraft mc) {
        server("gamerule doDaylightCycle false", "gamerule doWeatherCycle false", "time set 6000", "weather clear",
                "gamerule doMobSpawning false", "kill @e[type=!player]");
        // A clean superflat stage: clear leftovers from earlier test runs (fill is capped at 32768 blocks).
        for (int x = -24; x < 72; x += 24) {
            server(String.format("fill %d -60 -24 %d -54 -1 air", x, x + 23),
                    String.format("fill %d -60 0 %d -54 40 air", x, x + 23),
                    String.format("fill %d -61 -24 %d -61 40 grass_block", x, x + 23));
        }
        client(m -> {
            m.options.hideGui = true;
            m.options.setCameraType(CameraType.FIRST_PERSON);
        });
        switch (SCENE) {
            case "click" -> click();
            case "ride-vanilla" -> ride(false);
            case "ride-smooth" -> ride(true);
            case "preview" -> preview();
            default -> {
                SlashTracks.LOG.error("[demo] unknown scene '{}'", SCENE);
                done = true;
            }
        }
    }

    /** Elevated, slowly drifting view of the staircase; smoothed 2.5 s in. */
    private static void click() {
        buildTrack(false);
        server("gamemode spectator");
        duration = 8.0;
        camera = t -> {
            double k = ease(t / duration);
            return new double[]{27 + 5 * k, -45.5, 29.5, 180 + 4 * (k - 0.5), 38};
        };
        EVENTS.add(new Event(1.9, () -> still("click-before")));
        EVENTS.add(new Event(2.5, () -> onServer(level -> RunService.smooth(level, TOOL_TARGET))));
        EVENTS.add(new Event(7.4, () -> still("click-after")));
    }

    /** First-person ride from the start of the track, looking down the line. */
    private static void ride(boolean smooth) {
        buildTrack(smooth);
        server("gamemode creative", "tp @s 11.5 -59.9 10.5 -98 12",
                "summon minecart 11.5 -59.9 10.5");
        server("ride @s mount @e[type=minecart,sort=nearest,limit=1]");
        // ~40 blocks at 0.4 blocks/tick, plus the start and a moment of run-out.
        duration = 6.2;
        camera = null; // the cart carries the camera; the look direction is held along the track
        lookLocked = true;
        EVENTS.add(new Event(0.6, () -> runCommands(
                "execute as @e[type=minecart,sort=nearest,limit=1] run data merge entity @s {Motion:[0.6d,0d,0d]}")));
    }

    /** Third-person over the shoulder: the tool appears, preview line, smooth, then revert. */
    private static void preview() {
        buildTrack(false);
        server("gamemode creative", "clear @s", "item replace entity @s hotbar.1 with slashtracks:track_smoother",
                "tp @s 12.5 -60 13.8 -137.7 19.3");
        client(m -> {
            m.player.getInventory().selected = 0;
            m.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        });
        duration = 9.0;
        camera = null;
        EVENTS.add(new Event(1.5, () -> Minecraft.getInstance().player.getInventory().selected = 1));
        EVENTS.add(new Event(3.8, () -> useTool(true)));
        EVENTS.add(new Event(6.6, () -> useTool(false)));
    }

    // ---- helpers --------------------------------------------------------------------------

    private static boolean lookLocked;

    /** Ride scenes: keep the rider looking down the whole line (yaw -98.5 = east, a little north). */
    private static void holdLook(Minecraft mc) {
        if (!lookLocked || mc.player == null) return;
        mc.player.setYRot(-98.5f);
        mc.player.setXRot(12f);
        mc.player.yRotO = -98.5f;
        mc.player.xRotO = 12f;
    }

    private static void buildTrack(boolean smooth) {
        String kind = "execute positioned " + TRACK_ORIGIN.getX() + " " + TRACK_ORIGIN.getY() + " " + TRACK_ORIGIN.getZ()
                + " run slashtracks testtrack staircase 3" + (smooth ? " smooth" : "");
        server(kind, "kill @e[type=minecart]");
    }

    private static void useTool(boolean smooth) {
        Minecraft mc = Minecraft.getInstance();
        mc.player.swing(InteractionHand.MAIN_HAND);
        onServer(level -> {
            if (smooth) RunService.smooth(level, TOOL_TARGET);
            else RunService.revert(level, TOOL_TARGET);
        });
    }

    private static void still(String name) {
        Minecraft mc = Minecraft.getInstance();
        Screenshot.grab(mc.gameDirectory, "demo-" + name + ".png", mc.getMainRenderTarget(),
                msg -> SlashTracks.LOG.info("[demo] {}", msg.getString()));
    }

    private static double ease(double x) {
        x = Math.max(0, Math.min(1, x));
        return x * x * (3 - 2 * x);
    }

    private static void server(String... commands) {
        SETUP.add(() -> runCommands(commands));
    }

    private static void client(Consumer<Minecraft> action) {
        SETUP.add(() -> action.accept(Minecraft.getInstance()));
    }

    private static void runCommands(String... commands) {
        Minecraft mc = Minecraft.getInstance();
        MinecraftServer server = mc.getSingleplayerServer();
        UUID id = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) return;
            for (String c : commands) {
                server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), c);
            }
        });
    }

    private static void onServer(Consumer<ServerLevel> action) {
        MinecraftServer server = Minecraft.getInstance().getSingleplayerServer();
        server.execute(() -> action.accept(server.overworld()));
    }
}
