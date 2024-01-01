package personalworlds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.Config;
import personalworlds.world.PWWorldProvider;

@Mod(name = PWValues.modName, modid = PWValues.modID, version = PWValues.version)
public class PersonalWorlds {

    public final static String CHANNEL = PWValues.modID;

    public static final Logger log = LogManager.getLogger("personalworlds");
    public static BlockPersonalPortal blockPersonalPortal = new BlockPersonalPortal();
    public static MinecraftServer server;
    public static DimensionType dimType;

    @SidedProxy(
            clientSide = "personalworlds.proxy.ClientProxy",
            serverSide = "personalworlds.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        dimType = DimensionType.register("personal_world", "personalworlds", DimensionType.values().length,
                PWWorldProvider.class, false);
        MinecraftForge.EVENT_BUS.register(this);

        if(e.getSide().isClient())
            PacketCustom.assignHandler(CHANNEL, (ICustomPacketHandler.IClientPacketHandler) Packets.INSTANCE::handleClientPacket);
        PacketCustom.assignHandler(CHANNEL, (ICustomPacketHandler.IServerPacketHandler) Packets.INSTANCE::handleServerPacket);
    }

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent e) {
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        server = e.getServer();
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
    public void onWorldLoad(WorldEvent.Load e) {
        if (e.getWorld().provider.getDimension() == 0) {
            File file = new File(e.getWorld().getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
            if (file.exists()) {
                NBTTagCompound configNBT = null;
                try {
                    configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
                } catch (IOException ex) {
                    PersonalWorlds.log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
                }
                if (configNBT != null) {
                    int[] dimensions = configNBT.getIntArray("dimensions");
                    Arrays.stream(dimensions).forEach(dim -> DimensionManager.registerDimension(dim, dimType));
                }
            }
        }
    }

    @SubscribeEvent
    public void worldSave(WorldEvent.Save event) {
        try {
            if(!(event.getWorld().provider instanceof PWWorldProvider PWWP)) {
                return;
            }
            Config cfg = PWWP.getConfig();
            if(cfg == null || !cfg.isNeedsSaving()) {
                return;
            }
            // save the config
        } catch (Exception e) {
            log.fatal("couldnt save person dimension data for" + event.getWorld().provider.getDimension(), e);
        }
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent e) {
        File file = new File(server.getWorld(0).getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
        if (file.exists()) {
            NBTTagCompound configNBT = null;
            try {
                configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException ex) {
                PersonalWorlds.log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
            }
            if (configNBT != null) {
                int[] dimensions = configNBT.getIntArray("dimensions");
                Arrays.stream(dimensions).forEach(DimensionManager::unregisterDimension);
            }
        }
    }
}
