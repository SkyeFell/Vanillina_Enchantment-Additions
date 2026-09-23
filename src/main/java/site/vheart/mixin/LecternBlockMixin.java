package site.vheart.mixin;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(LecternBlock.class)
public class LecternBlockMixin {
    @Inject(method = "useWithoutItem", at=@At("HEAD"), cancellable = true)
    protected void useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> info) {
        if((Boolean) state.getValue(LecternBlock.HAS_BOOK)){
            LecternBlockEntity entity = ((LecternBlockEntity) level.getBlockEntity(pos));
            ItemStack book = entity.getBook();
            if(book.getItem() == Items.ENCHANTED_BOOK){
                if(player.isCrouching()){
                    Container container = ((LecternBlockEntityMixin) entity).getBookAccess();
                    ItemStack removed = container.removeItemNoUpdate(0);
                    container.setChanged();
                    if (!player.getInventory().add(removed)) {
                        player.drop(removed, false, Prediction.PREDICTED);
                    }
                    info.setReturnValue(InteractionResult.SUCCESS);
                }
                else {
                    String content = book.get(DataComponents.STORED_ENCHANTMENTS)
                            .entrySet().stream()
                            .map((x) -> x.getKey().value().description().getString() + " " + x.getIntValue())
                            .reduce((acc, x) -> {
                                if(!acc.isEmpty())
                                    acc+=" - ";
                                acc += x;
                                return acc;
                            })
                            .orElse("...?");
                    player.sendOverlayMessage(Component.literal(content));
                    info.setReturnValue(InteractionResult.CONSUME);
                }
            }
        }
    }

    @Inject(method = "useItemOn", at=@At("HEAD"), cancellable = true)
    protected void useItemOn(final ItemStack itemStack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> info) {
        if ((Boolean) state.getValue(LecternBlock.HAS_BOOK)) {
            LecternBlockEntity entity = ((LecternBlockEntity) level.getBlockEntity(pos));
            ItemStack book = entity.getBook();
            if(itemStack.getItem() == Items.BOOK && book.getItem() == Items.ENCHANTED_BOOK){
                ItemStack enchantmentItem;
                enchantmentItem = itemStack.transmuteCopy(Items.ENCHANTED_BOOK);
                enchantmentItem.setCount(1);

                ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(book));
                EnchantmentHelper.setEnchantments(enchantmentItem, enchantments.toImmutable());

                // Check if can replace item
                if(player.getInventory().getSelectedItem().getItem() == Items.BOOK &&
                        player.getInventory().getSelectedItem().count() == 1
                ){
                    player.getInventory().setSelectedItem(enchantmentItem);
                }
                // If unsure, just give item to player or throw
                else {
                    itemStack.consume(1, player);
                    if (!player.getInventory().add(enchantmentItem)) {
                        player.drop(enchantmentItem, false,Prediction.PREDICTED);
                    }
                }

                // This doesn't work as it's not a result of an item interaction, but is here for clarity
                info.setReturnValue(InteractionResult.SUCCESS.heldItemTransformedTo(enchantmentItem));
            }
            else if(itemStack.getItem() == Items.GOLDEN_AXE &&
                    book.getItem() == Items.ENCHANTED_BOOK &&
                    player.getInventory().hasAnyOf(Set.of(Items.BOOK))
            ){
                int extraBookSlot = player.getInventory().findSlotMatchingItem(new ItemStack(Items.BOOK));
                if(extraBookSlot != -1){
                    player.getInventory().getItem(extraBookSlot).consume(1, player);

                    ItemStack enchantmentItem = ((LecternBlockEntityMixin) entity).getBookAccess().removeItemNoUpdate(0);
                    ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(EnchantmentHelper.getEnchantmentsForCrafting(enchantmentItem));
                    ItemEnchantments.Mutable leftEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    ItemEnchantments.Mutable rightEnchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    int i = 0;

                    for(Object2IntMap.Entry<Holder<Enchantment>> entry : enchantmentItem.get(DataComponents.STORED_ENCHANTMENTS).entrySet()) {
                        if(i < enchantments.keySet().size() / 2)
                            rightEnchantments.set(entry.getKey(), entry.getIntValue());
                        else
                            leftEnchantments.set(entry.getKey(), entry.getIntValue());
                        i++;
                    }

                    if (!level.isClientSide()) {
                        ItemStack leftBook = new ItemStack(Items.ENCHANTED_BOOK);
                        EnchantmentHelper.setEnchantments(leftBook, leftEnchantments.toImmutable());

                        Vec3 leftItemPos = Vec3.atLowerCornerWithOffset(pos, (double)0.5F, 1.01, (double)0.5F).offsetRandomXZ(level.getRandom(), 0.5F);
                        ItemEntity leftDrop = new ItemEntity(level, leftItemPos.x(), leftItemPos.y(), leftItemPos.z(), leftBook);
                        leftDrop.setDefaultPickUpDelay();
                        level.addFreshEntity(leftDrop);

                        if(enchantments.keySet().size() > 1){
                            ItemStack rightBook = new ItemStack(Items.ENCHANTED_BOOK);
                            EnchantmentHelper.setEnchantments(rightBook, rightEnchantments.toImmutable());

                            Vec3 rightItemPos = Vec3.atLowerCornerWithOffset(pos, (double)0.5F, 1.01, (double)0.5F).offsetRandomXZ(level.getRandom(), 0.5F);
                            ItemEntity rightDrop = new ItemEntity(level, rightItemPos.x(), rightItemPos.y(), rightItemPos.z(), rightBook);
                            rightDrop.setDefaultPickUpDelay();
                            level.addFreshEntity(rightDrop);
                        }
                    }
                }
                info.setReturnValue(InteractionResult.SUCCESS);
            }
        }
        else{
            if(itemStack.getItem() == Items.ENCHANTED_BOOK){
                info.setReturnValue((InteractionResult)(LecternBlock.tryPlaceBook(player, level, pos, state, itemStack) ? InteractionResult.SUCCESS : InteractionResult.PASS));
            }
        }
    }
}
