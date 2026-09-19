package com.slashtracks.mixin;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface AbstractMinecartAccessor {

    @Accessor("onRails")
    void slashtracks$setOnRails(boolean onRails);

    @Accessor("onRails")
    boolean slashtracks$isOnRails();

    @Invoker("applyNaturalSlowdown")
    void slashtracks$applyNaturalSlowdown();

    @Invoker("getMaxSpeed")
    double slashtracks$getMaxSpeed();
}
