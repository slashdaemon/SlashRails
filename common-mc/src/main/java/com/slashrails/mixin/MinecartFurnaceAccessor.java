package com.slashrails.mixin;

import net.minecraft.world.entity.vehicle.MinecartFurnace;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Self-test only: fuel a furnace cart without a player. */
@Mixin(MinecartFurnace.class)
public interface MinecartFurnaceAccessor {

    @Accessor("fuel")
    void slashrails$setFuel(int fuel);
}
