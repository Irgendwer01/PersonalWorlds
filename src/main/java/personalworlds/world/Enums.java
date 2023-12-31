package personalworlds.world;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;

public class Enums {

    public enum DaylightCycle {

        SUN,
        MOON,
        CYCLE;


        public static DaylightCycle fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? DaylightCycle.CYCLE : values()[ordinal];
        }
    }

    public enum FlatBlock {

        GRASS(Blocks.GRASS),
        DIRT(Blocks.DIRT),
        COBBLESTONE(Blocks.COBBLESTONE),
        STONE(Blocks.STONE),
        STONE_BRICK(Blocks.STONEBRICK),
        STONE_SLAB(Blocks.STONE_SLAB);

        private final Block block;


        FlatBlock(Block block) {
            this.block = block;
        }

        public Block getBlock() {
            return this.block;
        }

        public static FlatBlock fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? GRASS : values()[ordinal];
        }
    }



}
