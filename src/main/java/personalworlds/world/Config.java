package personalworlds.world;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import personalworlds.PersonalWorlds;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class Config {

    private final File config;

    @Getter
    @Setter
    private boolean generateTrees = false;

    @Getter
    @Setter
    private Enums.DaylightCycle daylightCycle = Enums.DaylightCycle.CYCLE;

    @Getter
    @Setter
    private boolean passiveSpawn = false;

    @Getter
    @Setter
    private boolean populate = false;

    @Getter
    @Setter
    private boolean clouds = true;

    @Getter
    @Setter
    private boolean weather = false;

    @Getter
    @Setter
    private int skyColor = 0xc0d8ff;

    @Getter
    @Setter
    private BlockPos spawnPos;

    @Getter
    @Setter
    private float starsVisibility = 1F;

    @Getter
    @Setter
    private UUID owner;

    @Getter
    private final ArrayList<Enums.FlatBlock> blocks = new ArrayList<>();

    public Config(int dimID) {
        this.config = new File(PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" + "personal_world_" + dimID + "/PWConfig.dat");
        if (config.exists()) {
            try {
                NBTTagCompound configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(config.toPath()));
                this.populate = configNBT.getBoolean("populate");
                this.skyColor = configNBT.getInteger("sky_color");
                this.starsVisibility = configNBT.getInteger("stars_visibility");
                this.passiveSpawn = configNBT.getBoolean("passive_spawn");
                this.owner = configNBT.getUniqueId("owner");
                this.spawnPos = new BlockPos(configNBT.getInteger("spawn_pos_x"), configNBT.getInteger("spawn_pos_y"), configNBT.getInteger("spawn_pos_z"));
                if (configNBT.hasKey("blocks")) {
                    int[] intArray = configNBT.getIntArray("blocks");
                    for (int i : intArray) {
                        blocks.add(Enums.FlatBlock.fromOrdinal(i));
                    }
                }
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not load config in %s! Error: %s", config.getAbsolutePath(), e));
            }
        }
    }

    public boolean update() {
        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not create config in %s! Error: %s", config.getAbsolutePath(), e));
                return false;
            }
        }
        NBTTagCompound configNBT = new NBTTagCompound();
        configNBT.setInteger("sky_color", this.skyColor);
        configNBT.setBoolean("populate", this.populate);
        configNBT.setFloat("stars_visibility", this.starsVisibility);
        configNBT.setBoolean("passive_spawn", this.passiveSpawn);
        configNBT.setInteger("spawn_pos_x", this.spawnPos.getX());
        configNBT.setInteger("spawn_pos_y", this.spawnPos.getY());
        configNBT.setInteger("spawn_pos_z", this.spawnPos.getZ());
        configNBT.setUniqueId("owner", this.owner);
        if (!blocks.isEmpty()) {
            configNBT.setIntArray("blocks", blocks.stream().mapToInt(Enum::ordinal).toArray());
        }
        try {
            CompressedStreamTools.writeCompressed(configNBT, Files.newOutputStream(config.toPath()));
        } catch (IOException e) {
            PersonalWorlds.log.error(String.format("Could not save config in %s! Error: %s", config.getAbsolutePath(), e));
            return false;
        }
        return true;
    }
}
