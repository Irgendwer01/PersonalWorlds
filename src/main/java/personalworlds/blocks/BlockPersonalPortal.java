package personalworlds.blocks;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.cleanroommc.modularui.factory.ClientGUI;

import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.gui.PWGuiMUI;
import personalworlds.packet.Packets;

import java.util.Random;

public class BlockPersonalPortal extends Block implements ITileEntityProvider {

    public BlockPersonalPortal() {
        super(Material.ROCK, MapColor.BLUE);
        this.setRegistryName("personal_portal");
        this.setCreativeTab(CreativeTabs.MISC);
        this.setTranslationKey("personal_portal");
        this.blockHardness = 0.4F;
    }

    @Nullable
    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new TilePersonalPortal();
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn,
                                    EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!worldIn.isRemote) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof TilePersonalPortal tpp) {
                if (tpp.isActive() && !playerIn.isSneaking()) {
                    tpp.transport((EntityPlayerMP) playerIn);
                    return true;
                } else {
                    OpenGUI(worldIn, playerIn, tpp);
                }
            }
        }
        return true;
    }

    public void OpenGUI(World world, EntityPlayer player, TilePersonalPortal portal) {
        if (portal.isActive()) {
            if (player.isSneaking()) {
                Packets.INSTANCE.sendOpenGui(portal).sendToPlayer(player);
            }
        } else {
            if (world.provider.getDimension() != 0) {
                player.sendMessage(new TextComponentTranslation("chat.overworldPersonalDimension"));
                return;
            }
            Packets.INSTANCE.sendOpenGui(portal).sendToPlayer(player);
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        TileEntity te = world.getTileEntity(pos);
        if (te instanceof TilePersonalPortal tpp) {
            ItemStack itemStack = new ItemStack(state.getBlock());
            itemStack.deserializeNBT(tpp.getTileData());
            drops.add(itemStack);
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer,
                                ItemStack stack) {
        TileEntity te = world.getTileEntity(pos);
        if (!(te instanceof TilePersonalPortal tpp)) {
            return;
        }

        double dx = placer.posX - pos.getX();
        double dz = placer.posZ - pos.getZ();
        if (Math.abs(dx) > Math.abs(dz)) {
            tpp.setFacing((dx > 0) ? EnumFacing.EAST : EnumFacing.WEST);
        } else {
            tpp.setFacing((dz > 0) ? EnumFacing.SOUTH : EnumFacing.NORTH);
        }
        if (world.isRemote)
            return;

        if (stack.hasTagCompound()) {
            tpp.readFromNBT(stack.getTagCompound());
            EntityPlayerMP player = null;
            if (placer instanceof EntityPlayerMP) {
                player = (EntityPlayerMP) placer;
            }
            tpp.linkOtherPortal(false, player);
        }
        tpp.markDirty();
    }

}
