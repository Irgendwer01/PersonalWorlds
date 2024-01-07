package personalworlds.world;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

public class PWChunkGenerator implements IChunkGenerator {

    private final World world;
    private final Random random;
    private final DimensionConfig dimensionConfig;

    public PWChunkGenerator(World world) {
        this.world = world;
        this.random = world.rand;
        this.dimensionConfig = ((PWWorldProvider) world.provider).getConfig();
    }

    @Override
    public Chunk generateChunk(int x, int z) {
        ChunkPrimer chunkPrimer = new ChunkPrimer();
        int y = 0;
        if (!dimensionConfig.getLayers().isEmpty()) {
            for (FlatLayerInfo fli : dimensionConfig.getLayers()) {
                Block block = fli.getLayerMaterial().getBlock();
                if (block == null || block == Blocks.AIR) {
                    continue;
                }
                for (; y < fli.getMinY() + fli.getLayerCount() && y < world.getHeight(); ++y) {
                    for (int i = 0; i < 16; i++) {
                        for (int j = 0; j < 16; j++) {
                            chunkPrimer.setBlockState(i, y, j, fli.getLayerMaterial());
                        }
                    }
                }
            }
        }
        Chunk chunk = new Chunk(world, chunkPrimer, x, z);
        chunk.generateSkylightMap();
        return chunk;
    }

    @Override
    public void populate(int x, int z) {
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        this.random.setSeed(this.world.getSeed());
        long k = this.random.nextLong() / 2L * 2L + 1L;
        long l = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long)x * k + (long)z * l ^ this.world.getSeed());

        ForgeEventFactory.onChunkPopulate(true, this, this.world, this.random, x, z, false);
        if (dimensionConfig.generateVegetation()) {
            world.provider.getBiomeProvider().getBiome(new BlockPos(i+16, 0, j+16)).decorate(world, random,
                    blockpos);
        }
        ForgeEventFactory.onChunkPopulate(false, this, this.world, this.random, x, z, false);
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        if (dimensionConfig.passiveSpawn()) {
            return world.getBiome(pos).getSpawnableList(creatureType);
        }
        return null;
    }



    @Nullable
    @Override
    public BlockPos getNearestStructurePos(World worldIn, String structureName, BlockPos position,
                                           boolean findUnexplored) {
        return null;
    }

    @Override
    public void recreateStructures(Chunk chunkIn, int x, int z) {}

    @Override
    public boolean isInsideStructure(World worldIn, String structureName, BlockPos pos) {
        return false;
    }
}
