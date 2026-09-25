package com.slashrails.mixin;

import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spike-only: counts packets sent to players by type (/slashrails-netstat). */
@Mixin(ServerCommonPacketListenerImpl.class)
abstract class SpikeSendCounterMixin {

    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void slashrails$count(Packet<?> packet, CallbackInfo ci) {
        com.slashrails.fabric.SpikeNetStat.count(packet);
    }
}
