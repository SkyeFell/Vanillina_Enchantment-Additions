package site.vheart.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class AbstractContainerMenuMixin {

    @Inject(method="removed", at=@At("TAIL"))
    public void removed(final Player player, CallbackInfo info){
        // If enchantSlots was changed
        if((Object) this instanceof EnchantmentMenu){
            if(((EnchantmentMenu)(Object) this).getEnchantmentSeed() != player.getEnchantmentSeed()){
                ((PlayerMixin) player).setEnchantmentSeed( ((EnchantmentMenu)(Object) this).getEnchantmentSeed() );
            }
        }
    }
}
