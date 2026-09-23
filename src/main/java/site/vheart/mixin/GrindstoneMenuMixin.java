package site.vheart.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenu {
    protected GrindstoneMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
        super(menuType, containerId);
    }

    @Shadow
    abstract ItemStack removeNonCursesFrom(ItemStack item);

    @Shadow
    private Container resultSlots;
    @Shadow
    private Container repairSlots;
    @Shadow
    private ContainerLevelAccess access;

    @Inject(method = "computeResult",
            at=@At("HEAD"), cancellable = true)
    private void computeResultPatch(ItemStack input, ItemStack additional, CallbackInfoReturnable<ItemStack> info){
        if (!input.isEmpty() && !additional.isEmpty()) {
            if (!input.is(Items.ENCHANTED_BOOK) && additional.is(Items.BOOK)) {
                ItemStack item = !input.isEmpty() ? input : additional;
                info.setReturnValue(!EnchantmentHelper.hasAnyEnchantments(item) ? ItemStack.EMPTY : removeNonCursesFrom(item.copy()));
            }
        }
    }

    @Inject(method="slotsChanged", at=@At(value="INVOKE", target="slotsChanged", shift= At.Shift.AFTER), cancellable = true)
    public void slotsChanged(final Container container, CallbackInfo info){
        if (container == this.repairSlots && this.resultSlots.getItem(0).is(Items.ENCHANTED_BOOK)) {
            info.cancel();
        }
    }

    @Redirect(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At(
                    value= "INVOKE",
                    target = "Lnet/minecraft/world/inventory/GrindstoneMenu;addSlot(Lnet/minecraft/world/inventory/Slot;)Lnet/minecraft/world/inventory/Slot;"
            )
    )
    private Slot redirectAddSlot(GrindstoneMenu handler, Slot slot) {
        if(slot.getContainerSlot() == 1){
            return addSlot(new Slot(this.repairSlots, 1, 49, 40) {
                {
                    Objects.requireNonNull(this);
                }

                public boolean mayPlace(final ItemStack itemStack) {
                    return itemStack.isDamageableItem() || EnchantmentHelper.hasAnyEnchantments(itemStack) || itemStack.is(Items.BOOK);
                }
            });
        }
        if(slot.getContainerSlot() == 2){
            return addSlot(new Slot(this.resultSlots, 2, 129, 34) {
                {
                    Objects.requireNonNull(this);
                }

                public void onTake(final Player player, final ItemStack carried) {
                    access.execute((level, pos) -> {
                        if (level instanceof ServerLevel serverLevel) {
                            ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), this.getExperienceAmount(level));
                        }

                        level.levelEvent(1042, pos, 0);
                    });
                    if(!repairSlots.getItem(0).isEmpty() && repairSlots.getItem(1).getItem() == Items.BOOK) {
                        ItemStack enchantedBook = repairSlots.getItem(1).transmuteCopy(Items.ENCHANTED_BOOK);
                        enchantedBook.setCount(1);
                        EnchantmentHelper.setEnchantments(enchantedBook, repairSlots.getItem(0).getEnchantments());
                        resultSlots.setItem(1, enchantedBook);
                    }
                    if(repairSlots.getItem(1).getItem() != Items.BOOK)
                        repairSlots.setItem(1, ItemStack.EMPTY);
                    else if(carried.getItem() == Items.ENCHANTED_BOOK){
                        ItemStack removedBooks = repairSlots.getItem(1);
                        removedBooks.setCount(removedBooks.getCount()-1);
                        repairSlots.setItem(1, removedBooks);
                    }

                    repairSlots.setItem(0, ItemStack.EMPTY);
                }

                private int getExperienceAmount(final Level level) {
                    int amount = 0;
                    amount += this.getExperienceFromItem(repairSlots.getItem(0));
                    amount += this.getExperienceFromItem(repairSlots.getItem(1));
                    if(repairSlots.getItem(1).getItem() == Items.BOOK) return 0;
                    if (amount > 0) {
                        int halfAmount = (int)Math.ceil((double)amount / (double)2.0F);
                        return halfAmount + level.getRandom().nextInt(halfAmount);
                    } else {
                        return 0;
                    }
                }

                private int getExperienceFromItem(final ItemStack item) {
                    int amount = 0;
                    ItemEnchantments enchantments = EnchantmentHelper.getEnchantmentsForCrafting(item);

                    for(Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
                        Holder<Enchantment> enchant = (Holder<Enchantment>) entry.getKey();
                        int lvl = entry.getIntValue();
                        if (!enchant.is(EnchantmentTags.CURSE)) {
                            amount += ((Enchantment)enchant.value()).getMinCost(lvl);
                        }
                    }

                    return amount;
                }
            });
        }
        return addSlot(slot);
    }
}
