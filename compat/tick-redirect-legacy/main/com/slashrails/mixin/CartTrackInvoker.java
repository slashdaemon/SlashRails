package com.slashrails.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** The vanilla rail-movement calls the redirecting tick hook falls back to. */
@Mixin(AbstractMinecart.class)
public interface CartTrackInvoker {

    @Invoker("moveAlongTrack")
    void slashrails$moveAlongTrack(BlockPos pos, BlockState state);

    @Invoker("comeOffTrack")
    void slashrails$comeOffTrack();
}
