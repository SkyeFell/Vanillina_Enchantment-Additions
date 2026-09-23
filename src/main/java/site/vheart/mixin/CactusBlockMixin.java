package site.vheart.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CactusBlock.class)
public abstract class CactusBlockMixin extends Block {

    @Shadow
    protected abstract boolean canSurvive(final BlockState state, final LevelReader level, final BlockPos pos);

    public CactusBlockMixin(Properties properties) {
        super(properties);
    }

    @Inject(method="randomTick", at=@At("HEAD"))
    protected void randomTick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random, CallbackInfo info){
        // Does not care about age in Flower Production
        BlockPos _above = pos.above();
        if (level.isEmptyBlock(_above)) {
            int height = 1;

            while(level.getBlockState(pos.below(height)).is(this)) {
                ++height;
            }

            if (height >= 3 && this.canSurvive(this.defaultBlockState(), level, pos.above())) {
                // 1 Cactus Flower per Minute
                double chanceToGrowFlower = (double) 0.1F;
                if (random.nextDouble() <= chanceToGrowFlower) {
                    level.setBlockAndUpdate(_above, Blocks.CACTUS_FLOWER.defaultBlockState());
                }
            }
        }
    }
}
