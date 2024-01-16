package personalworlds;

import net.minecraft.world.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import personalworlds.command.PWCommand;
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
    public void onInit(FMLInitializationEvent e) {
        proxy.onInit(e);
    }

    @Mod.EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        proxy.onServerStarting(e);
        e.registerServerCommand(new PWCommand());
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
