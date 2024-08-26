package personalworlds.proxy;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.OreGenEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import gnu.trove.map.hash.TIntObjectHashMap;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;
import personalworlds.Values;
import personalworlds.blocks.BlockPersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.command.PWCommand;
import personalworlds.packet.Packets;
import personalworlds.world.DimensionConfig;
import personalworlds.world.PWWorldProvider;
import personalworlds.world.WeatherSyncHandler;

public class CommonProxy {

    public static final BlockPersonalPortal blockPersonalPortal = new BlockPersonalPortal();
    public static ItemBlock itemBlockPersonalPortal;

    private final TIntObjectHashMap<DimensionConfig> clientDimensionConfigs = new TIntObjectHashMap<>();
    private final TIntObjectHashMap<DimensionConfig> serverDimensionConfigs = new TIntObjectHashMap<>();

    public static TIntObjectHashMap<DimensionConfig> getDimensionConfigs(boolean isClient) {
        if (isClient)
            return PersonalWorlds.proxy.clientDimensionConfigs;
        return PersonalWorlds.proxy.serverDimensionConfigs;
    }

    public void onPreInit(FMLPreInitializationEvent e) {
        PacketCustom.assignHandler(Values.ModID,
                (ICustomPacketHandler.IServerPacketHandler) Packets.INSTANCE::handleServerPacket);
    }

    public void onInit(FMLInitializationEvent e) {
        PWConfig.Values.presets = PWConfig.presets;
        PWConfig.Values.allowedBiomes = PWConfig.allowedBiomes;
        PWConfig.Values.allowedBlocks = PWConfig.allowedBlocks;
        MinecraftForge.EVENT_BUS.register(new WeatherSyncHandler());
    }

    public void onServerStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new PWCommand());
        loadDimensionConfigs(DimensionManager.getCurrentSaveRootDirectory());
    }

    public void onServerStopping(FMLServerStoppingEvent event) {
        TIntObjectHashMap<DimensionConfig> configs = CommonProxy.getDimensionConfigs(false);
        configs.forEachEntry((dimID, dimCFG) -> {
            if (dimCFG == null || !dimCFG.needsSaving()) {
                return true;
            }
            dimCFG.update();
            return true;
        });
    }

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
    public void registerItems(RegistryEvent.Register<Item> e) {
        itemBlockPersonalPortal = new ItemBlock(blockPersonalPortal);
        e.getRegistry()
                .register(itemBlockPersonalPortal.setRegistryName(blockPersonalPortal.getRegistryName()));
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(blockPersonalPortal);
        GameRegistry.registerTileEntity(TilePersonalPortal.class,
                new ResourceLocation("personalworlds:tile_personal_portal"));
    }

    @SubscribeEvent
    public void initRecipes(RegistryEvent.Register<IRecipe> r) {
        GameRegistry.addShapedRecipe(new ResourceLocation("portal_block"), new ResourceLocation(Values.ModID),
                new ItemStack(CommonProxy.itemBlockPersonalPortal),
                "QBQ", "SQS", "OOO",
                'Q', Blocks.QUARTZ_BLOCK,
                'S', Blocks.QUARTZ_STAIRS,
                'O', Blocks.OBSIDIAN,
                'B', Ingredient.fromItems(Items.BOOK));
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
    public void netEventHandler(FMLNetworkEvent.ServerConnectionFromClientEvent event) {
        event.getManager().sendPacket(Packets.INSTANCE.sendWorldList().toPacket());
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
                        dimCFG.registerWithDimManager(false);
                    } catch (Exception e) {
                        PersonalWorlds.log.error("Couldn't load personal dimension data!", e);
                    }
                });
            }
        }
    }

    @SuppressWarnings("deprecation")
    public static void unregisterDims(boolean isClient) {
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
        }
        CommonProxy.getDimensionConfigs(isClient).clear();
    }

    public static class oreGenBusListener {

        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onOreGenerate(OreGenEvent.GenerateMinable event) {
            if (event.getWorld().provider instanceof PWWorldProvider) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    public static class BiomeBusListener {

        @SubscribeEvent(priority = EventPriority.HIGH)
        public void onBiomeDecorate(DecorateBiomeEvent.Decorate event) {
            if (event.getWorld().provider instanceof PWWorldProvider PWWP) {
                if (!event.getType().equals(DecorateBiomeEvent.Decorate.EventType.TREE)) {
                    if (event.getType().equals(DecorateBiomeEvent.Decorate.EventType.FOSSIL) ||
                            event.getType().equals(DecorateBiomeEvent.Decorate.EventType.CUSTOM) ||
                            !PWWP.getConfig().vegetationEnabled()) {
                        event.setResult(Event.Result.DENY);
                    }
                } else {
                    if (!PWWP.getConfig().generateTrees()) {
                        event.setResult(Event.Result.DENY);
                    }
                }
            }
        }
    }
}
