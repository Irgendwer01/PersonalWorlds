package personalworlds.proxy;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraftforge.client.ForgeClientHandler;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeInternalHandler;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.network.ForgeNetworkHandler;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.config.IConfigElement;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLModContainer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import personalworlds.PWConfig;
import personalworlds.PWValues;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.blocks.tile.TilePersonalPortalSpecialRender;
import personalworlds.packet.Packets;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void onPreInit(FMLPreInitializationEvent e) {
        super.onPreInit(e);
        PacketCustom.assignHandler(PWValues.modID,
                (ICustomPacketHandler.IClientPacketHandler) Packets.INSTANCE::handleClientPacket);
    }

    @Override
    public void onInit(FMLInitializationEvent e) {
        super.onInit(e);
        ClientRegistry.bindTileEntitySpecialRenderer(TilePersonalPortal.class, new TilePersonalPortalSpecialRender());
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ModelLoader.setCustomModelResourceLocation(itemBlockPersonalPortal, 0, new ModelResourceLocation(blockPersonalPortal.getRegistryName(), "normal"));
    }

    @SubscribeEvent
    public static void clientDisconnectionHandler(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        PWConfig.Values.presets = PWConfig.presets;
        PWConfig.Values.allowedBiomes = PWConfig.allowedBiomes;
        PWConfig.Values.allowedBlocks = PWConfig.allowedBlocks;
        unregisterDims(true);
    }

}
