package site.vheart.mixin;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import site.vheart.VanillinaEnchantmentAdditions;

import java.util.Objects;

@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {
	@Shadow
	private ContainerLevelAccess access;

	protected EnchantmentMenuMixin(@Nullable MenuType<?> menuType, int containerId) {
		super(menuType, containerId);
	}

	@Shadow
	public abstract void slotsChanged(final Container container);

	@Shadow
	@Mutable
	private Container enchantSlots;

	@Shadow
	private RandomSource random;

	@Shadow
	public DataSlot enchantmentSeed;

	@Inject(at = @At("HEAD"), method = "clickMenuButton")
	public void clickMenuButton(final Player player, final int buttonId, CallbackInfoReturnable<Boolean> info) {
		IO.println("menu button clicked");
	}

	@Inject(at = @At("HEAD"), method = "slotsChanged")
	public void slotsChanged(Container container, CallbackInfo info) {
		if (container == this.enchantSlots) {
			if(!container.getItem(2).isEmpty()){
				container.setItem(2, ItemStack.EMPTY);
				this.enchantmentSeed.set(this.random.nextInt());
			}
		}
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At(value="FIELD", target="enchantSlots", ordinal = 0, shift = At.Shift.AFTER))
	private void replaceEnchantSlot(CallbackInfo info){
		EnchantmentMenu self = (EnchantmentMenu) (Object) this;

		this.enchantSlots = new SimpleContainer(3) {
			{
				Objects.requireNonNull(self);
			}

			public void setChanged() {
				super.setChanged();
				self.slotsChanged(this);
			}
		};
	}

	@Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
			at = @At("TAIL"))
	public void EnchantmentMenuTail(final int containerId, final Inventory inventory, final ContainerLevelAccess access, CallbackInfo info){
		addSlot(new Slot(this.enchantSlots, 2, 5, 22) {
			public boolean mayPlace(final ItemStack itemStack) {
				return itemStack.is(Items.GOLD_NUGGET);
			}
			public int getMaxStackSize() {
				return 1;
			}
			public boolean isHighlightable() {
				return false;
			}

			public Identifier getNoItemIcon() {
				return Identifier.fromNamespaceAndPath(VanillinaEnchantmentAdditions.MOD_ID,"container/slot/gold_nugget");
			}
		});
	}
}