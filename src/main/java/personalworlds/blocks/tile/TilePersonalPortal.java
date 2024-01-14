package personalworlds.blocks.tile;

import com.cleanroommc.modularui.api.value.IStringValue;
import com.cleanroommc.modularui.value.StringValue;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IWorldNameable;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import personalworlds.PersonalWorlds;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.DimensionConfig;
import personalworlds.world.PWTeleporter;
import personalworlds.world.PWWorldProvider;

public class TilePersonalPortal extends TileEntity implements IWorldNameable, ITickable {

    @Getter
    private boolean isActive = false;

    @Getter
    private int targetID = 0;

    @Getter
    private BlockPos targetPos = new BlockPos(8, 8, 8);

    public String customName = "";


    public float bookRot = 0.0f;
    public float bookRotPrev = 0.0f;

    @Override
    public void update() {
        this.bookRotPrev = this.bookRot;
        EntityPlayer player = this.world.getClosestPlayer((double)((float)this.pos.getX() + 0.5F), (double)((float)this.pos.getY() + 0.5F), (double)((float)this.pos.getZ() + 0.5F), 3.0D, false);
        if(player != null) {
            double d0 = player.posX - (double)((float)this.pos.getX() + 0.5F);
            double d1 = player.posZ - (double)((float)this.pos.getZ() + 0.5F);
            this.bookRot = (float) MathHelper.atan2(d0, d1);

        }
        if(bookRot < -Math.PI) {
            bookRot += Math.PI * 2.0f;
        }
        if(bookRot > Math.PI) {
            bookRot -= Math.PI * 2.0f;
        }

    }

    public void transport(EntityPlayerMP player) {
        if (world.isRemote || !this.isActive || player == null) {
            return;
        }

        PWTeleporter tp = new PWTeleporter((WorldServer) world, targetPos);

        player.changeDimension(this.targetID, tp);
    }

    public void linkOtherPortal(boolean spawnPortal, EntityPlayerMP player) {
        if (!this.isActive)
            return;
        if (world.isRemote)
            return;

        WorldServer otherWorld = DimensionManager.getWorld(this.targetID);
        if (otherWorld == null) {
            DimensionManager.initDimension(this.targetID);
            otherWorld = DimensionManager.getWorld(this.targetID);
        }
        if (otherWorld == null) {
            PersonalWorlds.log.fatal("Couldn't initialize world {}", this.targetID);
            return;
        }

        int otherX = pos.getX(), otherY = pos.getY(), otherZ = pos.getZ();
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(otherX, otherY, otherZ);
        searchloop:
        for (otherX = pos.getX() - 1; otherX < pos.getX() + 1; otherX++) {
            for (otherY = pos.getY() - 1; otherY < pos.getY() + 1; otherY++) {
                if (otherY < 0 || otherY > otherWorld.getHeight()) continue;

                for (otherZ = pos.getZ() - 1; otherZ < pos.getZ() + 1; otherZ++) {
                    if (!otherWorld.getChunkProvider().chunkExists(otherX, otherZ)) {
                        otherWorld.getChunkProvider().loadChunk(otherX >> 4, otherZ >> 4);
                    }
                    blockPos.setPos(otherX, otherY, otherZ);
                    if (otherWorld.getBlockState(blockPos).getBlock() instanceof BlockPersonalPortal) {
                        break searchloop;
                    }
                }
            }
        }
        DimensionConfig cfg = DimensionConfig.getForDimension(this.targetID, false);
        TilePersonalPortal otherPortal = null;
        if (otherWorld.getBlockState(blockPos).getBlock() instanceof BlockPersonalPortal) {
            TileEntity te = otherWorld.getTileEntity(blockPos.toImmutable());
            if (te instanceof TilePersonalPortal)
                otherPortal = (TilePersonalPortal) te;
        } else if (spawnPortal) {
            otherX = targetPos.getX();
            otherY = targetPos.getY();
            otherZ = targetPos.getZ();
            BlockPos newPos = new BlockPos(otherX, otherY, otherZ);
            otherWorld.setBlockState(newPos, CommonProxy.blockPersonalPortal.getDefaultState(), 3);
            otherPortal = (TilePersonalPortal) otherWorld.getTileEntity(newPos);
            if (cfg.getLayers().isEmpty()) {
                for (int x = 5; x < 12; x++) {
                    for (int z = 5; z < 12; z++) {
                        otherWorld.setBlockState(new BlockPos(x, newPos.getY()-1, z), Blocks.STONE.getDefaultState());
                    }
                }
            }
        }
        if (otherPortal != null) {
            otherPortal.isActive = true;
            DimensionConfig otherPortalCfg = DimensionConfig.getForDimension(otherPortal.targetID, false);
            if (otherPortal.targetID != world.provider.getDimension() && otherPortalCfg != null) {
                if (player != null) {
                    player.sendMessage(new TextComponentTranslation("chat.personalWorld.relinked.error"));
                }
                return;
            }
            otherPortal.targetID = world.provider.getDimension();
            otherPortal.targetPos = pos;
            otherPortal.markDirty();
            if (player != null) {
                player.sendMessage(new TextComponentTranslation("chat.personalWorld.relinked", targetID));
            }
            PersonalWorlds.log.info(
                    "Linked portal at {}:{},{},{} to {}:{},{},{}",
                    targetID,
                    otherX,
                    otherY,
                    otherZ,
                    world.provider.getDimension(),
                    pos.getX(),
                    pos.getY(),
                    pos.getZ());
        }
    }

    public void updateSettings(EntityPlayerMP player, DimensionConfig conf) {
        if (world.isRemote || player == null) {
            return;
        }

        if (!world.canMineBlockBody(player, pos)) {
            player.sendMessage(new TextComponentTranslation("chat.personalWorld.denied"));
            return;
        }

        DimensionConfig sanitized = new DimensionConfig(0);
        sanitized.copyFrom(conf, false, true, true);
        boolean createNewDim = false;
        int targetID = 0;
        if (this.world.provider instanceof PWWorldProvider) {
            targetID = this.world.provider.getDimension();
        } else if (this.isActive) {
            targetID = this.targetID;
        }
        boolean changed = true;
        if (targetID > 0) {
            DimensionConfig realConf = DimensionConfig.getForDimension(targetID, false);
            if (realConf == null) {
                return;
            }
            changed = realConf.copyFrom(sanitized, false, true, false);

        } else {
            if (this.world.provider.getDimension() != 0) {
                return;
            }
            targetID = DimensionManager.getNextFreeDimId();
            conf.registerWithDimManager(targetID, false);
            this.isActive = true;
            this.targetID = targetID;
            this.targetPos = new BlockPos(this.targetPos.getX(), sanitized.getGroundLevel(), this.targetPos.getZ());
            markDirty();
            createNewDim = true;

            linkOtherPortal(true, player);
        }
        Packets.INSTANCE.sendWorldList().sendToClients();
        if (createNewDim) {
            player.sendMessage(new TextComponentTranslation("chat.personalWorld.created"));
        } else if (changed) {
            player.sendMessage(new TextComponentTranslation("chat.personalWorld.updated"));
        }
    }

    public void sendToClient() {
        if (world != null) {
            world.markBlockRangeForRenderUpdate(pos, pos);
            world.notifyBlockUpdate(pos, this.getBlockType().getStateFromMeta(this.getBlockMetadata()), this.getBlockType().getStateFromMeta(this.getBlockMetadata()), 3);
            world.scheduleBlockUpdate(pos,this.getBlockType(),0,0);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setBoolean("active", this.isActive);
        compound.setIntArray("target",
                new int[] { this.targetID, this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ() });
        compound.setString("name", this.customName);
        super.writeToNBT(compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        if (compound.hasKey("active")) {
            this.isActive = compound.getBoolean("active");
        }
        if (compound.hasKey("target")) {
            int[] array = compound.getIntArray("target");
            this.targetID = array[0];
            this.targetPos = new BlockPos(array[1], array[2], array[3]);
        }
        if (compound.hasKey("name")) {
            this.customName = compound.getString("name");
        }
        this.markDirty();
        this.sendToClient();
    }

    @Override
    public String getName() {
        return customName.isEmpty() ? null : customName;
    }

    @Nullable
    @Override
    public ITextComponent getDisplayName() {
        return customName.isEmpty() ? null : new TextComponentString(customName);
    }

    @Override
    public boolean hasCustomName() {
        return !customName.isEmpty();
    }


    @Override
    @Nullable
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 3, this.getUpdateTag());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getNbtCompound());
    }


}
