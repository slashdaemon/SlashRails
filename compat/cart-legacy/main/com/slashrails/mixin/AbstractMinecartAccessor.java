package com.slashrails.mixin;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface AbstractMinecartAccessor {

    @Accessor("onRails")
    void slashrails$setOnRails(boolean onRails);

    @Accessor("onRails")
    boolean slashrails$isOnRails();

    @Invoker("applyNaturalSlowdown")
    void slashrails$applyNaturalSlowdown();

    @Invoker("getMaxSpeed")
    double slashrails$getMaxSpeed();
}
