package personalworlds.proxy;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import personalworlds.PWValues;
import personalworlds.PersonalWorlds;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.world.DimensionConfig;
import personalworlds.world.PWWorldProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;


@Mod.EventBusSubscriber(modid = PWValues.modID)
public class CommonProxy {

    public static final BlockPersonalPortal blockPersonalPortal = new BlockPersonalPortal();

    final TIntObjectHashMap<DimensionConfig> clientDimensionConfigs = new TIntObjectHashMap<>();
    final TIntObjectHashMap<DimensionConfig> serverDimensionConfigs = new TIntObjectHashMap<>();

    public static TIntObjectHashMap<DimensionConfig> getDimensionConfigs(boolean isClient) {
        if (isClient)
            return PersonalWorlds.proxy.clientDimensionConfigs;
        return PersonalWorlds.proxy.serverDimensionConfigs;
    }

    public void onPreInit(FMLPreInitializationEvent e) {
        PacketCustom.assignHandler(PWValues.modID, (ICustomPacketHandler.IServerPacketHandler) Packets.INSTANCE::handleServerPacket);
    }

    public void onServerStarting(FMLServerStartingEvent e) {
        loadDimensionConfigs(DimensionManager.getCurrentSaveRootDirectory());
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        TIntObjectHashMap<DimensionConfig> configs = CommonProxy.getDimensionConfigs(false);
        configs.forEachEntry((dimID, dimCFG) -> {
            if (dimCFG == null || !dimCFG.needsSaving()) {
                return true;
            }
            DimensionManager.unregisterDimension(dimID);
            dimCFG.update();
            return true;
        });
    }

    public void serverStopped(FMLServerStoppedEvent event) {
        unregisterDims(false);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> e) {
        e.getRegistry()
                .register(new ItemBlock(blockPersonalPortal).setRegistryName(blockPersonalPortal.getRegistryName()));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(blockPersonalPortal);
        GameRegistry.registerTileEntity(TilePersonalPortal.class,
                new ResourceLocation("personalworlds:tile_personal_portal"));
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Packets.INSTANCE.sendWorldList().sendToPlayer(event.player);
    }

    @SubscribeEvent
    public static void worldSave(WorldEvent.Save event) {
        if (!(event.getWorld().provider instanceof PWWorldProvider PWWP)) {
            return;
        }
        DimensionConfig cfg = PWWP.getConfig();
        if (cfg == null || !cfg.needsSaving()) {
            return;
        }
        cfg.update();
    }

    private void loadDimensionConfigs(File path) {
        unregisterDims(false);
        File file = new File(path + "/PWWorlds.dat");
        if (file.exists()) {
            NBTTagCompound configNBT = null;
            try {
                configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException ex) {
                PersonalWorlds.log.error(String.format("Could not read PWWorlds.dat! Error: %s", ex));
            }
            if (configNBT != null) {
                int[] dimensions = configNBT.getIntArray("dimensions");
                Arrays.stream(dimensions).forEach(dimID -> {
                    try {
                        DimensionConfig dimCFG = new DimensionConfig(dimID);
                        dimCFG.registerWithDimManager(dimID, false);
                    } catch (Exception e) {
                        PersonalWorlds.log.error("Couldn't load personal dimension data from ", e);
                    }
                });
            }
        }
    }

    public void unregisterDims(boolean isClient) {
        if (CommonProxy.getDimensionConfigs(isClient).isEmpty()) {
            return;
        }
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            CommonProxy.getDimensionConfigs(isClient).forEachEntry((dimID, dimCFG) -> {
                if (DimensionManager.isDimensionRegistered(dimID)) {
                    FMLLog.info("unregistering PersonalWorld dimension %d", dimID);
                    DimensionManager.unregisterDimension(dimID);
                }
                return true;
            });
            CommonProxy.getDimensionConfigs(isClient).clear();
        }
    }
}
