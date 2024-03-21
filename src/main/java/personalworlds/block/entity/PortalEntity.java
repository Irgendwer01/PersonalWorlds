
package personalworlds.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import personalworlds.proxy.CommonProxy;

public class PortalEntity extends BlockEntity {
    public PortalEntity(BlockPos p_155229_, BlockState p_155230_) {
        super(CommonProxy.portal_entity.get(), p_155229_, p_155230_);
    }
}
