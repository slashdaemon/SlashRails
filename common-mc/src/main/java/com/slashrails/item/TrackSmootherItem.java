package com.slashrails.item;

import com.slashrails.Config;
import com.slashrails.run.RailGeometry;
import com.slashrails.run.RunService;
import com.slashrails.run.SmoothRunRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;

/** Use on a rail to smooth the run it belongs to; use on a smoothed rail to revert it. */
public class TrackSmootherItem extends TooltipItem {

    public TrackSmootherItem(Properties properties) {
        super(properties, "item.slashrails.track_smoother.tooltip");
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!RailGeometry.isRail(ctx.getLevel().getBlockState(ctx.getClickedPos()))) {
            return InteractionResult.PASS;
        }
        if (!(ctx.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        if (player != null && Config.opOnlyTool && !com.slashrails.Perms.isGamemaster(player)) {
            player.displayClientMessage(Component.translatable("slashrails.op_only"), true);
            return InteractionResult.FAIL;
        }
        Component msg;
        if (SmoothRunRegistry.get(level).contains(ctx.getClickedPos())) {
            int n = RunService.revert(level, ctx.getClickedPos());
            msg = Component.translatable("slashrails.reverted", n);
            level.playSound(null, ctx.getClickedPos(), SoundEvents.CHAIN_BREAK, SoundSource.BLOCKS, 0.8f, 1.2f);
        } else {
            msg = RunService.smooth(level, ctx.getClickedPos());
            if (SmoothRunRegistry.get(level).contains(ctx.getClickedPos())) {
                level.playSound(null, ctx.getClickedPos(), SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.5f, 1.6f);
            }
        }
        if (player != null) player.displayClientMessage(msg, true);
        return InteractionResult.SUCCESS;
    }
}
