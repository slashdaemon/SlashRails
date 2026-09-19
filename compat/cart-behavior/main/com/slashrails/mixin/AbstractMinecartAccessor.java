package com.slashrails.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface AbstractMinecartAccessor {

    @Invoker("applyNaturalSlowdown")
    Vec3 slashrails$applyNaturalSlowdown(Vec3 velocity);

    @Invoker("getMaxSpeed")
    double slashrails$getMaxSpeed(ServerLevel level);
}
