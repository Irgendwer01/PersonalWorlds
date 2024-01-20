package personalworlds.compat;

import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import org.jetbrains.annotations.NotNull;

import mcp.mobius.waila.api.*;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortal;

@WailaPlugin
public class HwylaCompat implements IWailaPlugin, IWailaDataProvider {

    @NotNull
    @Override
    public List<String> getWailaBody(ItemStack itemStack, List<String> tooltip, IWailaDataAccessor accessor,
                                     IWailaConfigHandler config) {
        if (accessor.getTileEntity() != null) {
            TileEntity te = accessor.getTileEntity();
            if (te instanceof TilePersonalPortal tpp) {
                if (tpp.isActive()) {
                    tooltip.add("Portal is active");
                    tooltip.add(String.format("Dimension: %s", tpp.getTargetID()));
                    tooltip.add(String.format("Position: %s", tpp.getTargetPos()));
                } else {
                    tooltip.add("Portal is not active");
                }
            }
        }
        return tooltip;
    }

    @Override
    public void register(IWailaRegistrar iWailaRegistrar) {
        iWailaRegistrar.registerBodyProvider(this, BlockPersonalPortal.class);
    }
}
