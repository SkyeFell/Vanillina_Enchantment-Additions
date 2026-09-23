package site.vheart.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LecternBlockEntity.class)
public interface LecternBlockEntityMixin {
    @Accessor("bookAccess")
    abstract void setBookAccess(Container seed);
    @Accessor("bookAccess")
    abstract Container getBookAccess();
}
