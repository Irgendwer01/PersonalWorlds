package personalworlds.mixin.gregtech;

import net.minecraft.world.WorldProvider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import gregtech.common.metatileentities.electric.MetaTileEntityGasCollector;
import personalworlds.world.PWWorldProvider;

@Mixin(value = MetaTileEntityGasCollector.class, remap = false)
public class MetaTileEntityGasCollectorMixin {

    @Redirect(method = "checkRecipe",
              at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldProvider;getDimension()I"))
    public int getDimension(WorldProvider instance) {
        if (instance instanceof PWWorldProvider) {
            return 0;
        }
        return instance.getDimension();
    }
}
