package com.slashrails.client;

import com.slashrails.SlashRails;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only scripted screenshot run, enabled with {@code -Dslashrails.visualtest=true} and a world
 * opened by quick play. Builds fixtures, moves the camera to fixed viewpoints and saves screenshots
 * to {@code screenshots/slashrails-*.png}, then quits. Does nothing unless the property is set.
 */
public final class VisualTest {

    private static final boolean ENABLED = Boolean.getBoolean("slashrails.visualtest");
    /** Multiplayer mode: join a dedicated server, log the synced run count, snapshot on changes. */
    private static final boolean MP = Boolean.getBoolean("slashrails.mptest");
    private static int lastRuns = -1;
    private static int shotIn = -1;
    private static int shots;

    private record Step(int delay, Runnable action) {
    }

    private static final List<Step> STEPS = new ArrayList<>();
    private static int index;
    private static int wait = -1;

    private VisualTest() {
    }

    /** Client tick (end). */
    public static void tick(Minecraft mc) {
        com.slashrails.client.demo.DemoScenes.tick(mc);
        if (MP) {
            mpTick(mc);
            return;
        }
        if (!ENABLED || mc.player == null || mc.getSingleplayerServer() == null) return;
        // An unfocused window pauses singleplayer; keep the script running.
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (wait < 0) {
            build();
            wait = 100; // let the world settle
            return;
        }
        if (--wait > 0) return;
        if (index >= STEPS.size()) return;
        Step s = STEPS.get(index++);
        try {
            s.action().run();
        } catch (RuntimeException e) {
            SlashRails.LOG.error("[visualtest] step {} failed", index, e);
        }
        wait = index < STEPS.size() ? STEPS.get(index).delay() : 1;
    }

    private static void mpTick(Minecraft mc) {
        if (mc.player == null) return;
        mc.options.pauseOnLostFocus = false;
        mc.options.hideGui = true;
        int runs = ClientRuns.all().size();
        if (runs != lastRuns) {
            StringBuilder ids = new StringBuilder();
            for (var r : ClientRuns.all()) ids.append(' ').append(r.id()).append(':').append(r.size());
            SlashRails.LOG.info("[mptest] runs={} ids={}", runs, ids.toString().trim());
            lastRuns = runs;
            shotIn = 60;
        }
        if (shotIn > 0 && --shotIn == 0) {
            String name = "slashrails-mp-" + (shots++) + ".png";
            Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(),
                    msg -> SlashRails.LOG.info("[mptest] {}", msg.getString()));
        }
    }

    private static void build() {
        Minecraft mc = Minecraft.getInstance();
        STEPS.clear();
        cmd(1, "gamerule doDaylightCycle false", "time set 6000", "weather clear", "gamemode creative",
                "gamerule doMobSpawning false");
        // Vanilla staircase (A), the same smoothed (B), a smoothed arc (C), a smoothed loop (D).
        cmd(20, "execute positioned 7 -60 10 run slashrails testtrack staircase 3",
                "execute positioned 7 -60 40 run slashrails testtrack staircase 3 smooth",
                "execute positioned 7 -60 80 run slashrails testtrack arc 12 smooth",
                "execute positioned 67 -60 40 run slashrails testtrack loop 8 smooth",
                "kill @e[type=minecart]");
        step(10, () -> {
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        });
        cmd(1, "gamemode spectator");
        view(60, "a-vanilla-topdown", 30, -38, 8, 0, 90);
        view(60, "b-smoothed-topdown", 30, -38, 38, 0, 90);
        view(60, "b-smoothed-angled", 22, -54, 50, 200, 35);
        view(60, "b-smoothed-closeup", 26, -56.5, 40, 180, 60);
        view(60, "a-vanilla-closeup", 26, -56.5, 10, 180, 60);
        view(60, "c-arc-topdown", 20, -34, 72, 0, 90);
        view(60, "d-loop-topdown", 75, -40, 32, 0, 90);
        // Ride the smoothed staircase in third person.
        cmd(20, "gamemode creative", "tp @s 11.5 -59.9 40.5", "summon minecart 11.5 -59.9 40.5 {Motion:[0.4d,0d,0d]}");
        cmd(2, "ride @s mount @e[type=minecart,sort=nearest,limit=1]");
        step(2, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        shot(40, "e-ride-1");
        shot(12, "e-ride-2");
        shot(12, "e-ride-3");
        cmd(20, "ride @s dismount", "kill @e[type=minecart]", "gamemode creative");
        step(2, () -> mc.options.setCameraType(CameraType.FIRST_PERSON));
        // Tool preview: hold the Track Smoother and look at a vanilla rail of A.
        cmd(10, "give @s slashrails:track_smoother", "item replace entity @s weapon.mainhand with slashrails:track_smoother");
        // Eye 1.62 above the feet; aim at the rail at (14, -60, 10) on the straight approach.
        view(10, "f-tool-preview", 11.5, -57.5, 10.5, -90, 58);
        step(40, mc::stop);
    }

    private static void view(int delay, String name, double x, double y, double z, float yaw, float pitch) {
        cmd(delay, String.format(java.util.Locale.ROOT, "tp @s %.2f %.2f %.2f %.1f %.1f", x, y, z, yaw, pitch));
        fly(2);
        shot(40, name);
    }

    /** Hover where teleported instead of falling (creative flight, both sides). */
    private static void fly(int delay) {
        step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            mc.player.getAbilities().flying = true;
            MinecraftServer server = mc.getSingleplayerServer();
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p != null) {
                    p.getAbilities().flying = true;
                    p.onUpdateAbilities();
                }
            });
        });
    }

    private static void shot(int delay, String name) {
        step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            Screenshot.grab(mc.gameDirectory, "slashrails-" + name + ".png", mc.getMainRenderTarget(),
                    msg -> SlashRails.LOG.info("[visualtest] {}", msg.getString()));
        });
    }

    private static void cmd(int delay, String... commands) {
        step(delay, () -> {
            Minecraft mc = Minecraft.getInstance();
            MinecraftServer server = mc.getSingleplayerServer();
            java.util.UUID id = mc.player.getUUID();
            server.execute(() -> {
                ServerPlayer p = server.getPlayerList().getPlayer(id);
                if (p == null) return;
                for (String c : commands) {
                    server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4), c);
                }
            });
        });
    }

    private static void step(int delay, Runnable r) {
        STEPS.add(new Step(delay, r));
    }
}
