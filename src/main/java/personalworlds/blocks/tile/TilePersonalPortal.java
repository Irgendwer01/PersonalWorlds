package personalworlds.blocks.tile;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import lombok.Getter;
import lombok.Setter;
import personalworlds.PersonalWorlds;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.DimensionConfig;
import personalworlds.world.PWTeleporter;
import personalworlds.world.PWWorldProvider;

public class TilePersonalPortal extends TileEntity {

    @Getter
    private boolean isActive = false;

    @Getter
    private int targetID = 0;

    @Getter
    private BlockPos targetPos = new BlockPos(8, 8, 8);

    @Getter
    @Setter
    private EnumFacing facing;

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
            otherPortal.facing = facing;
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

    @Override
    public void markDirty() {
        super.markDirty();
        if (world != null) {
            world.markBlockRangeForRenderUpdate(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1,
                    pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("active", this.isActive);
        compound.setIntArray("target",
                new int[] { this.targetID, this.targetPos.getX(), this.targetPos.getY(), this.targetPos.getZ() });
        compound.setInteger("facing", this.facing.ordinal());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        if (compound.hasKey("active")) {
            this.isActive = compound.getBoolean("active");
        }
        if (compound.hasKey("target")) {
            int[] array = compound.getIntArray("target");
            this.targetID = array[0];
            this.targetPos = new BlockPos(array[1], array[2], array[3]);
        }
        if (compound.hasKey("facing")) {
            this.facing = EnumFacing.VALUES[compound.getInteger("facing")];
        }

        super.readFromNBT(compound);
    }
}
