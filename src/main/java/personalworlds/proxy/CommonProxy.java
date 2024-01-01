package personalworlds.proxy;

import gnu.trove.map.hash.TIntObjectHashMap;
import personalworlds.PersonalWorlds;
import personalworlds.world.Config;

public class CommonProxy {

    final TIntObjectHashMap<Config> clientDimensionConfigs = new TIntObjectHashMap<>();
    final TIntObjectHashMap<Config> serverDimensionConfigs = new TIntObjectHashMap<>();

    public static TIntObjectHashMap<Config> getDimensionConfigs(boolean isClient) {
        if(isClient)
            return PersonalWorlds.proxy.clientDimensionConfigs;
        return PersonalWorlds.proxy.serverDimensionConfigs;
    }
}
