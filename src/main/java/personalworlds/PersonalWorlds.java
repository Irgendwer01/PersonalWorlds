package personalworlds;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.*;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.versions.forge.ForgeVersion;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import personalworlds.proxy.ClientProxy;
import personalworlds.proxy.CommonProxy;

@Mod(PWValues.ModID)
public class PersonalWorlds {
    public static Logger log = LogManager.getLogger(PWValues.ModID);

    public PersonalWorlds() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        if (FMLEnvironment.dist.isClient()) {
            ClientProxy.init(bus);
        }
        CommonProxy.BLOCKS.register(bus);
        CommonProxy.TABS.register(bus);
    }
}
