package com.slashrails.mixin;

import com.slashrails.ride.CartRide;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Per-cart curve-ride state. The tick hooks that use it are version-specific (compat/cart-*). */
@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin implements CartRide {

    @Unique
    private int slashrails$runId;
    @Unique
    private double slashrails$s = Double.NaN;
    @Unique
    private int slashrails$sign = 1;

    @Override
    public int slashrails$runId() {
        return slashrails$runId;
    }

    @Override
    public double slashrails$s() {
        return slashrails$s;
    }

    @Override
    public int slashrails$sign() {
        return slashrails$sign;
    }

    @Override
    public void slashrails$set(int runId, double s, int sign) {
        slashrails$runId = runId;
        slashrails$s = s;
        slashrails$sign = sign;
    }
}
