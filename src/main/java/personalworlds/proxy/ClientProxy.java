package personalworlds.proxy;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.relauncher.Side;
import personalworlds.PWValues;
import personalworlds.packet.Packets;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void onPreInit(FMLPreInitializationEvent e) {
        super.onPreInit(e);
        PacketCustom.assignHandler(PWValues.modID,
                (ICustomPacketHandler.IClientPacketHandler) Packets.INSTANCE::handleClientPacket);
    }

    @SubscribeEvent
    public void clientDisconnectionHandler(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        ConfigManager.sync(PWValues.modID, Config.Type.INSTANCE);
        unregisterDims(true);
    }
}
