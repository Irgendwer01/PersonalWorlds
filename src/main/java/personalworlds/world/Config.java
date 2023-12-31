package personalworlds.world;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.FlatLayerInfo;

import lombok.Getter;
import lombok.Setter;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;

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
    private boolean vegetation = false;

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
    private List<FlatLayerInfo> blocks = new ArrayList<>();

    public Config(int dimID) {
        this.config = new File(
                PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" +
                        "personal_world_" + dimID + "/PWConfig.dat");
        if (config.exists()) {
            try {
                NBTTagCompound configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(config.toPath()));
                this.vegetation = configNBT.getBoolean("vegetation");
                this.skyColor = configNBT.getInteger("sky_color");
                this.starsVisibility = configNBT.getInteger("stars_visibility");
                this.passiveSpawn = configNBT.getBoolean("passive_spawn");
                this.owner = configNBT.getUniqueId("owner");
                this.spawnPos = new BlockPos(configNBT.getInteger("spawn_pos_x"), configNBT.getInteger("spawn_pos_y"),
                        configNBT.getInteger("spawn_pos_z"));
                this.generateTrees = configNBT.getBoolean("generate_trees");
                this.daylightCycle = Enums.DaylightCycle.fromOrdinal(configNBT.getInteger("daylightcycle"));
                this.clouds = configNBT.getBoolean("clouds");
                this.weather = configNBT.getBoolean("weather");
                if (configNBT.hasKey("blocks")) {
                    this.blocks = LayersFromString(configNBT.getString("blocks"));
                }
            } catch (IOException e) {
                PersonalWorlds.log
                        .error(String.format("Could not load config in %s! Error: %s", config.getAbsolutePath(), e));
            }
        }
    }

    public boolean update() {
        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                PersonalWorlds.log
                        .error(String.format("Could not create config in %s! Error: %s", config.getAbsolutePath(), e));
                return false;
            }
        }
        NBTTagCompound configNBT = new NBTTagCompound();
        configNBT.setInteger("sky_color", this.skyColor);
        configNBT.setBoolean("vegetation", this.vegetation);
        configNBT.setFloat("stars_visibility", this.starsVisibility);
        configNBT.setBoolean("passive_spawn", this.passiveSpawn);
        configNBT.setInteger("spawn_pos_x", this.spawnPos.getX());
        configNBT.setInteger("spawn_pos_y", this.spawnPos.getY());
        configNBT.setInteger("spawn_pos_z", this.spawnPos.getZ());
        configNBT.setUniqueId("owner", this.owner);
        configNBT.setBoolean("clouds", this.clouds);
        configNBT.setBoolean("generate_trees", this.generateTrees);
        configNBT.setInteger("daylightcycle", this.daylightCycle.ordinal());
        configNBT.setBoolean("weather", this.weather);
        if (!blocks.isEmpty()) {
            configNBT.setString("blocks", LayersToString(blocks));
        }
        try {
            CompressedStreamTools.writeCompressed(configNBT, Files.newOutputStream(config.toPath()));
        } catch (IOException e) {
            PersonalWorlds.log
                    .error(String.format("Could not save config in %s! Error: %s", config.getAbsolutePath(), e));
            return false;
        }
        return true;
    }

    public String LayersToString(List<FlatLayerInfo> flatLayerInfos) {
        StringBuilder stringBuilder = new StringBuilder();
        String string = "";
        for (FlatLayerInfo flatLayerInfo : flatLayerInfos)
            string = stringBuilder.append(string).append(flatLayerInfo.toString()).append(",").toString();
        return string;
    }

    public List<FlatLayerInfo> LayersFromString(String string) {
        int currY = 0;
        ArrayList<FlatLayerInfo> flatLayerInfos = new ArrayList<>();
        String[] stringArray = string.split(",");
        for (String string1 : stringArray) {
            currY++;
            FlatLayerInfo flatLayerInfo = LayerFromString(string1);
            flatLayerInfo.setMinY(PWConfig.minY + currY);
        }
        return flatLayerInfos;
    }

    public FlatLayerInfo LayerFromString(String string) {
        int metadata = 0;
        Block block;
        String[] stringArray = string.split("\\*", 2);
        int layers = 1;
        if (stringArray.length == 2) {
            layers = Integer.parseInt(stringArray[0]);
            if (layers + PWConfig.minY >= 256) {
                layers = 256 - PWConfig.minY;
            }
            if (layers < 0) {
                layers = 1;
            }
            string = stringArray[1];
        }
        String[] stringArray1 = string.split(":", 2);
        if (stringArray1.length == 3) {
            metadata = Integer.parseInt(stringArray1[2]);
            string = stringArray1[0] + stringArray1[1];
        }
        block = Block.REGISTRY.getObject(new ResourceLocation(string));
        return new FlatLayerInfo(3, layers, block, metadata);
    }
}
