package personalworlds.world;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.event.ForgeEventFactory;

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
                    y += fli.getLayerCount();
                    continue;
                }
                for (; y < fli.getMinY() + fli.getLayerCount() && y < world.getHeight(); ++y) {
                    for (int localX = 0; localX < 16; localX++) {
                        for (int localZ = 0; localZ < 16; localZ++) {
                            chunkPrimer.setBlockState(localX, y, localZ, fli.getLayerMaterial());
                        }
                    }
                }
            }
        }

        int groundLevel = dimensionConfig.getGroundLevel() - 1;
        if (groundLevel >= 0 && groundLevel < world.getHeight()) {
            applyGridSettings(chunkPrimer, x, z, groundLevel);
        }

        Chunk chunk = new Chunk(world, chunkPrimer, x, z);
        byte biomeId = (byte) Biome.getIdForBiome(dimensionConfig.getBiome());
        byte[] biomeArray = chunk.getBiomeArray();
        for (int i = 0; i < biomeArray.length; i++) {
            biomeArray[i] = biomeId;
        }
        chunk.generateSkylightMap();
        return chunk;
    }

    private void applyGridSettings(ChunkPrimer chunkPrimer, int chunkX, int chunkZ, int groundLevel) {
        int intervalX = MathHelper.clamp(dimensionConfig.getBoundaryChunkIntervalX(), 0, 20);
        int intervalZ = MathHelper.clamp(dimensionConfig.getBoundaryChunkIntervalZ(), 0, 20);

        int gapWidth = MathHelper.clamp(dimensionConfig.getGapWidth(), 0, 5);
        int periodX = intervalX + gapWidth;
        int periodZ = intervalZ + gapWidth;
        boolean isGapChunkX = gapWidth > 0 && intervalX > 0 && mod(chunkX, periodX) >= intervalX;
        boolean isGapChunkZ = gapWidth > 0 && intervalZ > 0 && mod(chunkZ, periodZ) >= intervalZ;

        if (isGapChunkX || isGapChunkZ) {
            generateGapInChunk(chunkPrimer, chunkX, chunkZ, groundLevel, isGapChunkX, isGapChunkZ, periodX, periodZ,
                    gapWidth, intervalX, intervalZ);
        }

        boolean isBoundaryX;
        boolean prevBoundaryX;
        boolean isBoundaryZ;
        boolean prevBoundaryZ;
        if (gapWidth > 0) {
            isBoundaryX = intervalX > 0 && mod(chunkX, periodX) == 0;
            prevBoundaryX = intervalX > 0 && mod(chunkX, periodX) == intervalX - 1;
            isBoundaryZ = intervalZ > 0 && mod(chunkZ, periodZ) == 0;
            prevBoundaryZ = intervalZ > 0 && mod(chunkZ, periodZ) == intervalZ - 1;
        } else {
            isBoundaryX = intervalX > 0 && mod(chunkX, intervalX) == 0;
            isBoundaryZ = intervalZ > 0 && mod(chunkZ, intervalZ) == 0;
            prevBoundaryX = intervalX > 0 && mod(chunkX + 1, intervalX) == 0;
            prevBoundaryZ = intervalZ > 0 && mod(chunkZ + 1, intervalZ) == 0;
        }

        Block boundaryBlockA = dimensionConfig.getBoundaryBlockAResolved();
        int boundaryMetaA = dimensionConfig.getBoundaryMetaA();
        Block boundaryBlockB = dimensionConfig.getBoundaryBlockBResolved();
        int boundaryMetaB = dimensionConfig.getBoundaryMetaB();

        boolean hasA = boundaryBlockA != null && boundaryBlockA != Blocks.AIR;
        boolean hasB = boundaryBlockB != null && boundaryBlockB != Blocks.AIR;

        if ((hasA || hasB) && !isGapChunkX && !isGapChunkZ &&
                (isBoundaryX || prevBoundaryX || isBoundaryZ || prevBoundaryZ)) {
            for (int localZ = 0; localZ < 16; localZ++) {
                if (isBoundaryX) {
                    setStripeBlock(chunkPrimer, groundLevel, 0, localZ, chunkX, chunkZ, boundaryBlockA, boundaryMetaA,
                            boundaryBlockB, boundaryMetaB);
                }
                if (prevBoundaryX) {
                    setStripeBlock(chunkPrimer, groundLevel, 15, localZ, chunkX, chunkZ, boundaryBlockA,
                            boundaryMetaA, boundaryBlockB, boundaryMetaB);
                }
            }
            for (int localX = 0; localX < 16; localX++) {
                if (isBoundaryZ) {
                    setStripeBlock(chunkPrimer, groundLevel, localX, 0, chunkX, chunkZ, boundaryBlockA, boundaryMetaA,
                            boundaryBlockB, boundaryMetaB);
                }
                if (prevBoundaryZ) {
                    setStripeBlock(chunkPrimer, groundLevel, localX, 15, chunkX, chunkZ, boundaryBlockA,
                            boundaryMetaA, boundaryBlockB, boundaryMetaB);
                }
            }
        }

        if (dimensionConfig.isCenterEnabled() && intervalX > 0 && intervalZ > 0) {
            Block centerBlock = dimensionConfig.getCenterBlockResolved();
            if (centerBlock != null && centerBlock != Blocks.AIR) {
                DimensionConfig.CenterDirection dir = dimensionConfig.getCenterDirection();
                int dirOffX = (dir == DimensionConfig.CenterDirection.SW || dir == DimensionConfig.CenterDirection.NW) ?
                        -1 : 0;
                int dirOffZ = (dir == DimensionConfig.CenterDirection.NE || dir == DimensionConfig.CenterDirection.NW) ?
                        -1 : 0;
                int centerLocalX = intervalX * 8 + dirOffX;
                int centerLocalZ = intervalZ * 8 + dirOffZ;

                int modCX = mod(chunkX, periodX);
                int modCZ = mod(chunkZ, periodZ);
                if (modCX < intervalX && modCZ < intervalZ) {
                    int blockStartX = modCX * 16;
                    int blockStartZ = modCZ * 16;
                    if (centerLocalX >= blockStartX && centerLocalX < blockStartX + 16 && centerLocalZ >= blockStartZ &&
                            centerLocalZ < blockStartZ + 16) {
                        int localX = centerLocalX - blockStartX;
                        int localZ = centerLocalZ - blockStartZ;
                        IBlockState state = dimensionConfig.stateFromSelection(dimensionConfig.getCenterBlock(),
                                dimensionConfig.getCenterMeta());
                        if (state != null) {
                            chunkPrimer.setBlockState(localX, groundLevel, localZ, state);
                        }
                    }
                }
            }
        }
    }

    private void setStripeBlock(ChunkPrimer chunkPrimer, int groundLevel, int localX, int localZ, int chunkX,
                                int chunkZ, Block blockA, int metaA, Block blockB, int metaB) {
        int worldX = (chunkX << 4) + localX;
        int worldZ = (chunkZ << 4) + localZ;
        StripeBlock stripe = getStripeBlock(worldX, worldZ, blockA, metaA, blockB, metaB);
        if (stripe.state != null) {
            chunkPrimer.setBlockState(localX, groundLevel, localZ, stripe.state);
        }
    }

    private void generateGapInChunk(ChunkPrimer chunkPrimer, int chunkX, int chunkZ, int groundLevel,
                                    boolean isGapX, boolean isGapZ, int periodX, int periodZ, int gapWidth,
                                    int intervalX, int intervalZ) {
        IBlockState gapStateA = dimensionConfig.stateFromSelection(dimensionConfig.getGapBlockA(),
                dimensionConfig.getGapMetaA());
        IBlockState gapStateB = dimensionConfig.stateFromSelection(dimensionConfig.getGapBlockB(),
                dimensionConfig.getGapMetaB());
        IBlockState gapStateC = dimensionConfig.stateFromSelection(dimensionConfig.getGapBlockC(),
                dimensionConfig.getGapMetaC());

        if (gapStateA == null || gapStateA.getBlock() == Blocks.AIR) {
            return;
        }

        int gapWidthBlocks = gapWidth * 16;
        for (int localZ = 0; localZ < 16; localZ++) {
            for (int localX = 0; localX < 16; localX++) {
                int worldX = (chunkX << 4) + localX;
                int worldZ = (chunkZ << 4) + localZ;
                IBlockState state = gapStateA;

                if (dimensionConfig.getGapPreset() == DimensionConfig.GapPreset.ROAD) {
                    boolean isIntersection = isGapX && isGapZ;
                    boolean hasStripe = gapStateB != null && gapStateB.getBlock() != Blocks.AIR;
                    if (isIntersection) {
                        if (hasStripe) {
                            int gapOffsetX = mod(chunkX, periodX) - intervalX;
                            int offsetX = gapOffsetX * 16 + localX;
                            int gapOffsetZ = mod(chunkZ, periodZ) - intervalZ;
                            int offsetZ = gapOffsetZ * 16 + localZ;
                            boolean onEdgeX = offsetX == 0 || offsetX == gapWidthBlocks - 1;
                            boolean onEdgeZ = offsetZ == 0 || offsetZ == gapWidthBlocks - 1;
                            if (onEdgeX && onEdgeZ) {
                                state = gapStateB;
                            }
                        }
                    } else {
                        if (isGapX && periodX > 0) {
                            int gapChunkOffset = mod(chunkX, periodX) - intervalX;
                            int offsetInGap = gapChunkOffset * 16 + localX;
                            state = getRoadState(offsetInGap, worldZ, gapWidthBlocks, gapStateA, gapStateB, gapStateC);
                        } else if (isGapZ && periodZ > 0) {
                            int gapChunkOffset = mod(chunkZ, periodZ) - intervalZ;
                            int offsetInGap = gapChunkOffset * 16 + localZ;
                            state = getRoadState(offsetInGap, worldX, gapWidthBlocks, gapStateA, gapStateB, gapStateC);
                        }
                    }
                }

                if (state != null && state.getBlock() != Blocks.AIR) {
                    chunkPrimer.setBlockState(localX, groundLevel, localZ, state);
                }
            }
        }
    }

    private IBlockState getRoadState(int offsetInGap, int alongRoad, int gapWidthBlocks, IBlockState baseState,
                                     IBlockState stripeState, IBlockState dashState) {
        boolean hasStripe = stripeState != null && stripeState.getBlock() != Blocks.AIR;
        boolean hasDash = dashState != null && dashState.getBlock() != Blocks.AIR;
        if (hasStripe && (offsetInGap == 0 || offsetInGap == gapWidthBlocks - 1)) {
            return stripeState;
        }
        if (hasDash && gapWidthBlocks >= 4) {
            int center = gapWidthBlocks / 2;
            if ((offsetInGap == center || offsetInGap == center - 1) && mod(alongRoad + 2, 8) < 4) {
                return dashState;
            }
        }
        return baseState;
    }

    private StripeBlock getStripeBlock(int worldX, int worldZ, Block blockA, int metaA, Block blockB, int metaB) {
        boolean useA = ((worldX + worldZ) & 1) == 0;
        if (useA) {
            IBlockState stateA = blockState(blockA, metaA);
            if (stateA != null && stateA.getBlock() != Blocks.AIR) {
                return new StripeBlock(stateA);
            }
            IBlockState stateB = blockState(blockB, metaB);
            if (stateB != null && stateB.getBlock() != Blocks.AIR) {
                return new StripeBlock(stateB);
            }
        } else {
            IBlockState stateB = blockState(blockB, metaB);
            if (stateB != null && stateB.getBlock() != Blocks.AIR) {
                return new StripeBlock(stateB);
            }
            IBlockState stateA = blockState(blockA, metaA);
            if (stateA != null && stateA.getBlock() != Blocks.AIR) {
                return new StripeBlock(stateA);
            }
        }
        return new StripeBlock(null);
    }

    private IBlockState blockState(Block block, int meta) {
        if (block == null) {
            return null;
        }
        return meta == 0 ? block.getDefaultState() : block.getStateFromMeta(meta);
    }

    private int mod(int a, int b) {
        int m = a % b;
        return m < 0 ? m + b : m;
    }

    private static class StripeBlock {

        private final IBlockState state;

        private StripeBlock(IBlockState state) {
            this.state = state;
        }
    }

    @Override
    public void populate(int x, int z) {
        net.minecraft.block.BlockFalling.fallInstantly = true;
        int i = x * 16;
        int j = z * 16;
        BlockPos blockpos = new BlockPos(i, 0, j);
        this.random.setSeed(this.world.getSeed());
        long k = this.random.nextLong() / 2L * 2L + 1L;
        long l = this.random.nextLong() / 2L * 2L + 1L;
        this.random.setSeed((long) x * k + (long) z * l ^ this.world.getSeed());
        Biome biome = world.provider.getBiomeProvider().getBiome(new BlockPos(i + 16, 0, j + 16));

        ForgeEventFactory.onChunkPopulate(true, this, this.world, this.random, x, z, false);
        biome.decorate(world, random, blockpos);
        ForgeEventFactory.onChunkPopulate(false, this, this.world, this.random, x, z, false);
        net.minecraft.block.BlockFalling.fallInstantly = false;
    }

    @Override
    public boolean generateStructures(Chunk chunkIn, int x, int z) {
        return false;
    }

    @Override
    public List<Biome.SpawnListEntry> getPossibleCreatures(EnumCreatureType creatureType, BlockPos pos) {
        return world.getBiome(pos).getSpawnableList(creatureType);
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
