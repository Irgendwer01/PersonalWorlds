package personalworlds.world;

import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;

public class PWWorldProvider extends WorldProvider {
    @Override
    public DimensionType getDimensionType() {
        return DimensionType.OVERWORLD;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new PWChunkGeneratorVoid(this.world);
    }
}
