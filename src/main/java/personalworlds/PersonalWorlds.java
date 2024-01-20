package personalworlds;

import net.minecraft.world.DimensionType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import personalworlds.compat.TheOneProbeCompat;
import personalworlds.proxy.CommonProxy;
import personalworlds.world.PWWorldProvider;

@Mod(name = Values.ModName,
     modid = Values.ModID,
     version = Values.Version,
     dependencies = "required-after:codechickenlib;" + "required-after:modularui@[2.4,);" + "after:theoneprobe;" +
             "after:hwyla;")
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
        if (Loader.isModLoaded("theoneprobe")) {
            new TheOneProbeCompat().init();
        }
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
