package com.slashtracks.mixin;

import com.slashtracks.run.RunService;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Notices when a smoothed rail is broken or re-shaped, so its run can revert to vanilla. */
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin {

    @Shadow
    @Final
    Level level;

    @Inject(method = "setBlockState", at = @At("RETURN"))
    private void slashtracks$onSetBlockState(BlockPos pos, BlockState state, boolean isMoving,
                                             CallbackInfoReturnable<BlockState> cir) {
        if (cir.getReturnValue() != null && level instanceof ServerLevel serverLevel) {
            RunService.onBlockChanged(serverLevel, pos);
        }
    }
}
