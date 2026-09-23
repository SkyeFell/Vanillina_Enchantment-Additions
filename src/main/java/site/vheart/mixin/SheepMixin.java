package site.vheart.mixin;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Debug(export=true)
@Mixin(Sheep.class)
public abstract class SheepMixin extends LivingEntity {
    protected SheepMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "shear", at = @At(value = "INVOKE", target = "dropFromShearingLootTable"))
    public void shear(final ServerLevel level, final SoundSource soundSource, final ItemStack tool, CallbackInfo info){
        ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(tool);
        Holder<Enchantment> bonus = enchantments.keySet().stream().filter(x -> x.is(Enchantments.LOOTING) || x.is(Enchantments.FORTUNE)).findFirst().orElse(null);
        if(bonus != null){
            for(int j = 0; j < enchantments.getLevel(bonus); j++){
                this.dropFromShearingLootTable(level, BuiltInLootTables.SHEAR_SHEEP, tool, (l, drop) -> {
                    for(int i = 0; i < drop.getCount(); ++i) {
                        ItemEntity entity = this.spawnAtLocation(l, drop.copyWithCount(1), 1.0F);
                        if (entity != null) {
                            entity.setDeltaMovement(entity.getDeltaMovement().add((double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F), (double)(this.random.nextFloat() * 0.05F), (double)((this.random.nextFloat() - this.random.nextFloat()) * 0.1F)));
                        }
                    }
                });
            }
        }
    }
}
