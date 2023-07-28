package personalworlds.world;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import personalworlds.PersonalWorlds;

import java.io.*;
import java.nio.file.Files;
import java.util.UUID;

public class Config {

    @Getter
    @Setter
    private boolean passiveSpawn = false;

    @Getter
    @Setter
    private boolean populate = false;

    @Getter
    @Setter
    private int skyColor = 0xc0d8ff;

    @Getter
    @Setter
    private BlockPos spawnPos;

    @Getter
    @Setter
    private float starsVisibility = 1F;
    private final File config;

    @Getter
    @Setter
    private UUID owner;

    public Config(String path) {
        this.config = new File(path);
        if (config.exists()) {
            try {
                NBTTagCompound configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(config.toPath()));
                this.populate = configNBT.getBoolean("populate");
                this.skyColor = configNBT.getInteger("sky_color");
                this.starsVisibility = configNBT.getInteger("stars_visibility");
                this.passiveSpawn = configNBT.getBoolean("passive_spawn");
                this.owner = configNBT.getUniqueId("owner");
                this.spawnPos = new BlockPos(configNBT.getIntArray("spawn_pos")[0], configNBT.getIntArray("spawn_pos")[1], configNBT.getIntArray("spawn_pos")[2]);
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not load config in %s, error: %s", config.getAbsolutePath(), e.getMessage()));
            }
        }
    }

    public boolean update() {
        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not create config in %s, error: %s", config.getAbsolutePath(), e.getMessage()));
                return false;
            }
        }
        NBTTagCompound configNBT = new NBTTagCompound();
        configNBT.setInteger("sky_color", this.skyColor);
        configNBT.setBoolean("populate", this.populate);
        configNBT.setFloat("stars_visibility", this.starsVisibility);
        configNBT.setBoolean("passive_spawn", this.passiveSpawn);
        configNBT.setIntArray("spawn_pos", new int[]{this.spawnPos.getX(), this.spawnPos.getY(), this.spawnPos.getZ()});
        configNBT.setUniqueId("owner", this.owner);
        try {
            CompressedStreamTools.writeCompressed(configNBT, Files.newOutputStream(config.toPath()));
        } catch (IOException e) {
            PersonalWorlds.log.error(String.format("Could not save config in %s, error: %s", config.getAbsolutePath(), e.getMessage()));
            return false;
        }
        return true;
    }
}
