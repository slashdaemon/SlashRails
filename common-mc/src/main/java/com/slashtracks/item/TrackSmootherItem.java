package com.slashtracks.item;

import com.slashtracks.Config;
import com.slashtracks.run.RailGeometry;
import com.slashtracks.run.RunService;
import com.slashtracks.run.SmoothRunRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** Use on a rail to smooth the run it belongs to; use on a smoothed rail to revert it. */
public class TrackSmootherItem extends Item {

    public TrackSmootherItem(Properties properties) {
        super(properties);
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
        if (player != null && Config.opOnlyTool && !player.hasPermissions(2)) {
            player.displayClientMessage(Component.translatable("slashtracks.op_only"), true);
            return InteractionResult.FAIL;
        }
        Component msg;
        if (SmoothRunRegistry.get(level).contains(ctx.getClickedPos())) {
            int n = RunService.revert(level, ctx.getClickedPos());
            msg = Component.translatable("slashtracks.reverted", n);
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.slashtracks.track_smoother.tooltip"));
    }
}
