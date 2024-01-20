package personalworlds.compat;

import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import personalworlds.PWValues;
import personalworlds.blocks.tile.TilePersonalPortal;

public class TheOneProbeCompat implements IProbeInfoProvider {

    public void init() {
        ITheOneProbe theOneProbe = TheOneProbe.theOneProbeImp;
        theOneProbe.registerProvider(this);
    }

    @Override
    public String getID() {
        return PWValues.modID;
    }

    @Override
    public void addProbeInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer, World world, IBlockState iBlockState, IProbeHitData iProbeHitData) {
        BlockPos pos = iProbeHitData.getPos();
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TilePersonalPortal tpp) {
            if (tpp.isActive()) {
                iProbeInfo.text("Portal is activated");
                iProbeInfo.text(String.format("Dimension: %s", tpp.getTargetID()));
                iProbeInfo.text(String.format("Position: %s", tpp.getTargetPos()));
            } else {
                iProbeInfo.text("Portal is not yet activated");
            }
        }
    }
}
