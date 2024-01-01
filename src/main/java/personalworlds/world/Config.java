package personalworlds.world;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import codechicken.lib.packet.PacketCustom;
import net.minecraft.block.Block;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.FlatLayerInfo;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.DimensionManager;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;
import personalworlds.proxy.CommonProxy;

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
    private List<FlatLayerInfo> layers = new ArrayList<>();

    @Getter
    private boolean needsSaving = true;



    public Config() {
        this.config = new File(
                PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" +
                        "personal_world_" + 0 + "/PWConfig.dat");
    }

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
                this.generateTrees = configNBT.getBoolean("generate_trees");
                this.daylightCycle = Enums.DaylightCycle.fromOrdinal(configNBT.getInteger("daylightcycle"));
                this.clouds = configNBT.getBoolean("clouds");
                this.weather = configNBT.getBoolean("weather");
                if (configNBT.hasKey("blocks")) {
                    this.layers = LayersFromString(configNBT.getString("blocks"));
                }
            } catch (IOException e) {
                PersonalWorlds.log
                        .error(String.format("Could not load config in %s! Error: %s", config.getAbsolutePath(), e));
            }
        }
    }

    public static Config fromPacket(PacketCustom packet) {
        Config cfg = new Config();
        cfg.readFromPacket(packet);
        return cfg;
    }

    public void readFromPacket(PacketCustom packet) {
        this.needsSaving = true;
        this.setSkyColor(packet.readInt());
        this.setStarsVisibility(packet.readFloat());
        this.setDaylightCycle(Enums.DaylightCycle.fromOrdinal(packet.readVarInt()));
        this.setClouds(packet.readBoolean());
        this.setWeather(packet.readBoolean());
        this.setVegetation(packet.readBoolean());
        this.setGenerateTrees(packet.readBoolean());
    }

    public void writeToPacket(PacketCustom packet) {
        packet.writeInt(skyColor);
        packet.writeFloat(starsVisibility);
        packet.writeInt(daylightCycle.ordinal());
        packet.writeBoolean(clouds);
        packet.writeBoolean(weather);
        packet.writeBoolean(vegetation);
        packet.writeBoolean(generateTrees);
    }

    public boolean copyFrom(Config source, boolean copySaveInfo, boolean copyVisualInfo, boolean copyGenerationInfo) {
        this.needsSaving = false;
        if(copySaveInfo) {

        }
        if(copyVisualInfo) {
            this.setSkyColor(source.getSkyColor());
            this.setStarsVisibility(source.getStarsVisibility());
            this.setDaylightCycle(source.getDaylightCycle());
            this.setClouds(source.isClouds());
            this.setWeather(source.isWeather());
        }
        if(copyGenerationInfo) {
            this.setGenerateTrees(source.isGenerateTrees());
            this.setVegetation(source.isVegetation());
            this.layers = source.layers;
            this.needsSaving = true;
        }
        boolean modified = this.needsSaving;
        this.needsSaving = true;
        return modified;
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
        configNBT.setBoolean("clouds", this.clouds);
        configNBT.setBoolean("generate_trees", this.generateTrees);
        configNBT.setInteger("daylightcycle", this.daylightCycle.ordinal());
        configNBT.setBoolean("weather", this.weather);
        if (!layers.isEmpty()) {
            configNBT.setString("blocks", LayersToString(layers));
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

    public static Config getForDimension(int dimId, boolean isClient) {
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            return CommonProxy.getDimensionConfigs(isClient).get(dimId);
        }
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

    public void registerWithDimManager(int dimID, boolean isClient) {
        if(!DimensionManager.isDimensionRegistered(dimID)) {
            DimensionType dimType = DimensionType.register("personalWorld", "personalWorld", dimID, PWWorldProvider.class, false);
            DimensionManager.registerDimension(dimID, dimType);
            PersonalWorlds.log.info("DimensionConfig registered for dim {}, client {}", dimID, isClient, new Throwable());
        }
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            if (!CommonProxy.getDimensionConfigs(isClient).containsKey(dimID)) {
                CommonProxy.getDimensionConfigs(isClient).put(dimID, this);
            } else {
                CommonProxy.getDimensionConfigs(isClient).get(dimID).copyFrom(this, true, true, true);
            }
        }
    }

}
