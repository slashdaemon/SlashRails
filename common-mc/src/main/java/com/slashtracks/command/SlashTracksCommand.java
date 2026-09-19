package com.slashtracks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.slashtracks.core.Dir;
import com.slashtracks.core.RailNode;
import com.slashtracks.run.RunService;
import com.slashtracks.run.SmoothRunRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Minecart;

import java.util.List;

/**
 * {@code /slashtracks} — operator tools for testing:
 * <ul>
 *   <li>{@code testtrack <kind> [size] [smooth]} builds a repeatable fixture next to you, with a
 *       powered rail and a detector rail (lamp beside it) on the straights, and a cart at the start.</li>
 *   <li>{@code probe [ticks]} measures the ride of the cart you're in: max heading change per tick.</li>
 *   <li>{@code selftest} rides every fixture vanilla and smoothed, unattended, and reports pass/fail.</li>
 *   <li>{@code list} counts smoothed runs in this dimension.</li>
 * </ul>
 */
public final class SlashTracksCommand {

    private SlashTracksCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> testtrack = Commands.literal("testtrack");
        sized(testtrack, "staircase", 1, 16, 4, n -> TestTracks.staircase(n, 6), false);
        sized(testtrack, "arc", 4, 64, 16, TestTracks::arc, false);
        sized(testtrack, "scurve", 4, 32, 10, TestTracks::sCurve, false);
        sized(testtrack, "loop", 4, 48, 10, TestTracks::loop, true);
        sized(testtrack, "diagonal", 2, 32, 10, TestTracks::diagonal, false);
        testtrack.then(Commands.literal("corner")
                .executes(c -> build(c, TestTracks.corner(), false, false))
                .then(Commands.literal("smooth").executes(c -> build(c, TestTracks.corner(), false, true))));

        dispatcher.register(Commands.literal("slashtracks")
                .requires(src -> src.hasPermission(2))
                .then(testtrack)
                .then(Commands.literal("probe")
                        .executes(c -> probe(c, 200))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(20, 6000))
                                .executes(c -> probe(c, IntegerArgumentType.getInteger(c, "ticks")))))
                .then(Commands.literal("selftest").executes(SlashTracksCommand::selftest))
                .then(Commands.literal("list").executes(SlashTracksCommand::list)));
    }

    private interface Walk {
        List<Dir> make(int size);
    }

    private static void sized(LiteralArgumentBuilder<CommandSourceStack> parent, String name, int min, int max,
                              int def, Walk walk, boolean closed) {
        parent.then(Commands.literal(name)
                .executes(c -> build(c, walk.make(def), closed, false))
                .then(Commands.argument("size", IntegerArgumentType.integer(min, max))
                        .executes(c -> build(c, walk.make(IntegerArgumentType.getInteger(c, "size")), closed, false))
                        .then(Commands.literal("smooth")
                                .executes(c -> build(c, walk.make(IntegerArgumentType.getInteger(c, "size")), closed, true)))));
    }

    private static int build(CommandContext<CommandSourceStack> c, List<Dir> steps, boolean closed, boolean smooth) {
        CommandSourceStack src = c.getSource();
        ServerLevel level = src.getLevel();
        BlockPos origin = BlockPos.containing(src.getPosition()).offset(3, 0, 0);
        TrackBuilder.Fixture f = TrackBuilder.build(level, origin, steps, closed);

        RailNode first = f.nodes().get(closed ? 0 : 1);
        level.addFreshEntity(new Minecart(level, first.x() + 0.5, first.y() + 0.0625, first.z() + 0.5));

        int rails = f.nodes().size();
        if (smooth) {
            Component result = RunService.smooth(level, new BlockPos(first.x(), first.y(), first.z()));
            src.sendSuccess(() -> Component.literal("Built " + rails + " rails. ").append(result), false);
        } else {
            src.sendSuccess(() -> Component.literal("Built " + rails
                    + " rails (vanilla). Use the Track Smoother on them to smooth."), false);
        }
        return rails;
    }

    private static int probe(CommandContext<CommandSourceStack> c, int ticks) {
        ServerPlayer player = c.getSource().getPlayer();
        if (player == null || !(player.getVehicle() instanceof AbstractMinecart)) {
            c.getSource().sendFailure(Component.literal("Ride a minecart first, then run /slashtracks probe."));
            return 0;
        }
        RideProbe.start(player, ticks);
        c.getSource().sendSuccess(() -> Component.literal("Probing the ride for " + ticks + " ticks..."), false);
        return 1;
    }

    private static int selftest(CommandContext<CommandSourceStack> c) {
        if (!SelfTest.start(c.getSource())) {
            c.getSource().sendFailure(Component.literal("A self-test is already running."));
            return 0;
        }
        c.getSource().sendSuccess(() -> Component.literal("SlashTracks self-test started."), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> c) {
        SmoothRunRegistry registry = SmoothRunRegistry.get(c.getSource().getLevel());
        int rails = registry.all().stream().mapToInt(r -> r.size()).sum();
        int runs = registry.all().size();
        c.getSource().sendSuccess(() -> Component.literal(runs + " smoothed runs, " + rails + " rails in this dimension."), false);
        return runs;
    }
}
