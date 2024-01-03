package personalworlds.proxy;

import gnu.trove.map.hash.TIntObjectHashMap;
import personalworlds.PersonalWorlds;
import personalworlds.world.DimensionConfig;

public class CommonProxy {

    final TIntObjectHashMap<DimensionConfig> clientDimensionConfigs = new TIntObjectHashMap<>();
    final TIntObjectHashMap<DimensionConfig> serverDimensionConfigs = new TIntObjectHashMap<>();

    public static TIntObjectHashMap<DimensionConfig> getDimensionConfigs(boolean isClient) {
        if (isClient)
            return PersonalWorlds.proxy.clientDimensionConfigs;
        return PersonalWorlds.proxy.serverDimensionConfigs;
    }
}
