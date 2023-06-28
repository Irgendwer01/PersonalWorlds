package personalworlds.world;

import net.minecraft.entity.Entity;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class PWTeleporter extends Teleporter {
    public PWTeleporter(WorldServer worldIn) {
        super(worldIn);
    }

    @Override
    public boolean placeInExistingPortal(Entity entityIn, float rotationYaw) {
        return false;
    }

    @Override
    public boolean isVanilla() {
        return false;
    }

    @Override
    public boolean makePortal(Entity entityIn) {
        return false;
    }
}
