package personalworlds;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.PWOreBusListener;

@Mod(name = PWValues.modName, modid = PWValues.modID, version = PWValues.version)
public class PersonalWorlds {

    public static final Logger log = LogManager.getLogger("personalworlds");

    @SidedProxy(
                clientSide = "personalworlds.proxy.ClientProxy",
                serverSide = "personalworlds.proxy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void onPreInit(FMLPreInitializationEvent e) {
        MinecraftForge.ORE_GEN_BUS.register(new PWOreBusListener());
        proxy.onPreInit(e);
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
