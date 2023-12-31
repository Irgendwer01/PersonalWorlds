package personalworlds.blocks;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.gui.PWGui;

public class BlockPersonalPortal extends Block implements ITileEntityProvider {

    public BlockPersonalPortal() {
        super(Material.ROCK, MapColor.BLUE);
        this.setRegistryName("personal_portal");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setTranslationKey("personal_portal");
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TilePersonalPortal();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        TilePersonalPortal tilePersonalPortal = (TilePersonalPortal) worldIn.getTileEntity(pos);
        if (worldIn.isRemote) {
            if (tilePersonalPortal != null) {
                if (tilePersonalPortal.getTileData().hasKey("dimID")) {
                    if (playerIn.isSneaking()) {
                        Minecraft.getMinecraft().displayGuiScreen(new PWGui(playerIn));
                    }
                } else {
                    Minecraft.getMinecraft().displayGuiScreen(new PWGui(playerIn));
                }
            }
        }
        return true;
    }
}
