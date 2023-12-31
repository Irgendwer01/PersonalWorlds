package personalworlds;

import codechicken.lib.packet.PacketCustom;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.world.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkHandshakeEstablished;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import personalworlds.packet.Packets;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.PWWorldProvider;

@Mod(name = PWValues.modName, modid = PWValues.modID, version = PWValues.version)
public class PersonalWorlds {

    public static final Logger log = LogManager.getLogger("personalworlds");
    public static DimensionType dimType;

    @SidedProxy(
                clientSide = "personalworlds.proxy.ClientProxy",
                serverSide = "personalworlds.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        MinecraftForge.ORE_GEN_BUS.register(new CommonProxy.oreGenBusListener());
        MinecraftForge.TERRAIN_GEN_BUS.register(new CommonProxy.BiomeBusListener());
        MinecraftForge.EVENT_BUS.register(new CommonProxy());
        proxy.onPreInit(e);
        dimType = DimensionType.register("personal_world",
                "pw", DimensionType.values().length, PWWorldProvider.class, false);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        proxy.onServerStarting(e);
    }

    @Mod.EventHandler
    public void onServerStopping(FMLServerStoppingEvent e) {
        proxy.onServerStopping(e);
    }

    @Mod.EventHandler
    public void onServerStopped(FMLServerStoppedEvent e) {
        proxy.serverStopped(e);
    }
}
