package personalworlds.packet;

import codechicken.lib.packet.PacketCustom;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import personalworlds.PWConfig;
import personalworlds.PWValues;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.DimensionConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum Packets {
    INSTANCE;

    public enum PacketIds {

        DUMMY,
        UPDATE_WORLDLIST,
        CONFIG_SYNC,
        CHANGE_WORLD_SETTINGS;
    }

    public PacketCustom sendChangeWorldSettings(TilePersonalPortal tile, DimensionConfig dimensionConfig) {
        PacketCustom pkt = new PacketCustom(PWValues.modID, PacketIds.CHANGE_WORLD_SETTINGS.ordinal());
        pkt.writeVarInt(tile.getWorld().provider.getDimension());
        pkt.writeVarInt(tile.getPos().getX());
        pkt.writeVarInt(tile.getPos().getY());
        pkt.writeVarInt(tile.getPos().getZ());
        dimensionConfig.writeToPacket(pkt);
        //dimensionConfig.update();
        return pkt;
    }

    public void handleClientPacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        int id = packet.getType();
        if(id >= PacketIds.values().length || id < 0)
            return;

        switch(PacketIds.values()[id]) {
            case UPDATE_WORLDLIST -> {
                handleWorldList(packet);
            }
            case CHANGE_WORLD_SETTINGS -> {}
        }
    }

    public void handleServerPacket(PacketCustom packet, EntityPlayerMP player, INetHandlerPlayServer handler) {
        int id = packet.getType();
        if(id >= PacketIds.values().length || id < 0)
            return;

        switch(PacketIds.values()[id]) {
            case UPDATE_WORLDLIST -> {}
            case CHANGE_WORLD_SETTINGS -> {
                int dim = packet.readVarInt();
                int x = packet.readVarInt();
                int y = packet.readVarInt();
                int z = packet.readVarInt();
                DimensionConfig conf = DimensionConfig.fromPacket(packet);
                if(player != null && player.getServerWorld() != null && player.getServerWorld().provider.getDimension() == dim) {
                    TileEntity te = player.getServerWorld().getTileEntity(new BlockPos(x, y, z));
                    if(te instanceof TilePersonalPortal tpp) {
                        tpp.updateSettings(player, conf);
                    }
                }
            }
        }
    }

    public PacketCustom sendWorldList() {
        PacketCustom pkt = new PacketCustom(PWValues.modID, PacketIds.UPDATE_WORLDLIST.ordinal());
        pkt.writeVarInt(PWConfig.allowedBlocks.length);
        Arrays.stream(PWConfig.allowedBlocks).forEach(pkt::writeString);
        pkt.writeVarInt(PWConfig.allowedBiomes.length);
        Arrays.stream(PWConfig.allowedBiomes).forEach(pkt::writeString);
        synchronized (CommonProxy.getDimensionConfigs(false)) {
            pkt.writeVarInt(CommonProxy.getDimensionConfigs(false).size());
            CommonProxy.getDimensionConfigs(false).forEachEntry((dimID, dimCfg) -> {
                pkt.writeVarInt(dimID);
                dimCfg.writeToPacket(pkt);
                return true;
            });
        }
        return pkt;
    }

    private static void handleWorldList(PacketCustom pkt) {
        int allowedBlocks = pkt.readVarInt();
        List<String> tmpList = new ArrayList<>(allowedBlocks);
        for (int i = 0; i < allowedBlocks; ++i) {
            tmpList.add(pkt.readString());
        }
        PWConfig.allowedBlocks = tmpList.toArray(new String[0]);
        int allowedBiomes = pkt.readVarInt();
        tmpList = new ArrayList<>(allowedBiomes);
        for (int i = 0; i < allowedBiomes; ++i) {
            tmpList.add(pkt.readString());
        }
        PWConfig.allowedBiomes = tmpList.toArray(new String[0]);

        int dimConfigs = pkt.readVarInt();
        for(int i = 0; i < dimConfigs; i++) {
            int dimID = pkt.readVarInt();
            DimensionConfig cfg = DimensionConfig.fromPacket(pkt);
            cfg.registerWithDimManager(dimID, true);
        }
    }

}
