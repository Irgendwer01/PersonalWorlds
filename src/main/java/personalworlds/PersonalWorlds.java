package personalworlds;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import gnu.trove.map.hash.TIntObjectHashMap;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.DimensionConfig;
import personalworlds.world.PWWorldProvider;

@Mod(name = PWValues.modName, modid = PWValues.modID, version = PWValues.version)
public class PersonalWorlds {

    public final static String CHANNEL = PWValues.modID;

    public static final Logger log = LogManager.getLogger("personalworlds");
    public static BlockPersonalPortal blockPersonalPortal = new BlockPersonalPortal();

    @SidedProxy(
                clientSide = "personalworlds.proxy.ClientProxy",
                serverSide = "personalworlds.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);

        if (e.getSide().isClient())
            PacketCustom.assignHandler(CHANNEL,
                    (ICustomPacketHandler.IClientPacketHandler) Packets.INSTANCE::handleClientPacket);
        PacketCustom.assignHandler(CHANNEL,
                (ICustomPacketHandler.IServerPacketHandler) Packets.INSTANCE::handleServerPacket);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        loadDimensionConfigs(DimensionManager.getCurrentSaveRootDirectory());
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> e) {
        e.getRegistry()
                .register(new ItemBlock(blockPersonalPortal).setRegistryName(blockPersonalPortal.getRegistryName()));
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(blockPersonalPortal);
        GameRegistry.registerTileEntity(TilePersonalPortal.class,
                new ResourceLocation("personalworlds:tile_personal_portal"));
    }

    @SubscribeEvent
    public void worldSave(WorldEvent.Save event) {
        if (!(event.getWorld().provider instanceof PWWorldProvider PWWP)) {
            return;
        }
        DimensionConfig cfg = PWWP.getConfig();
        if (cfg == null || !cfg.needsSaving()) {
            return;
        }
        cfg.update();
    }

    @SubscribeEvent
    public void clientDisconnectionHandler(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ConfigManager.sync(PWValues.modID, Config.Type.INSTANCE);
        unregisterDims(true);
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
                        log.error("Couldn't load personal dimension data from ", e);
                    }
                });
            }
        }
    }

    @Mod.EventHandler
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

    private void unregisterDims(boolean isClient) {
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

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        unregisterDims(false);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            unregisterDims(true);
            synchronized (CommonProxy.getDimensionConfigs(true)) {
                CommonProxy.getDimensionConfigs(true).clear();
            }
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        Packets.INSTANCE.sendWorldList().sendToPlayer(event.player);
    }
}
