package personalworlds.world;

import static personalworlds.PersonalWorlds.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraftforge.common.DimensionManager;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import personalworlds.PersonalWorlds;
import personalworlds.proxy.CommonProxy;

public class DimensionConfig {

    private File config;

    private int dimID;
    private boolean allowGenerationChanges = true;
    private boolean generateTrees = false;
    private boolean clouds = true;
    private boolean weather = false;
    private int skyColor = 0xc0d8ff;
    private float starsVisibility = 1F;
    private List<FlatLayerInfo> layers = new ArrayList<>();
    private boolean needsSaving = true;
    private Biome biome = Biomes.PLAINS;
    private DaylightCycle daylightCycle = DaylightCycle.CYCLE;
    private boolean vegetation = false;
    private boolean spawnPassiveMobs = false;
    private boolean spawnMonsters = false;
    public static final String PRESET_FLAT = "Flat;minecraft:bedrock,3*minecraft:dirt,minecraft:grass";
    public static final String PRESET_MINING = "Mining;4*minecraft:bedrock,58*minecraft:stone,minecraft:dirt,minecraft:grass";

    public static final Pattern PRESET_VALIDATION_PATTERN = Pattern
            .compile(
                    "^(?:[1-9][0-9]*\\*)?[a-zA-Z+_]+:[a-zA-Z+_]+(?::[1-9][0-9]?)?(?:,(?:[1-9][0-9]*\\*)?[a-zA-Z+_]+:[a-zA-Z+_]+(?::[1-9][0-9]?)?+)*$");

    public DimensionConfig(int dimID) {
        this.dimID = dimID;
        this.config = new File(
                DimensionManager.getCurrentSaveRootDirectory() + "/" +
                        "personal_world_" + dimID + "/PWConfig.dat");
        if (config.exists()) {
            try {
                NBTTagCompound configNBT = CompressedStreamTools.readCompressed(Files.newInputStream(config.toPath()));
                this.skyColor = configNBT.getInteger("sky_color");
                this.starsVisibility = configNBT.getInteger("stars_visibility");
                this.spawnMonsters = configNBT.getBoolean("spawn_monsters");
                this.spawnPassiveMobs = configNBT.getBoolean("spawn_passive_mobs");
                this.generateTrees = configNBT.getBoolean("generate_trees");
                this.daylightCycle = DaylightCycle.fromOrdinal(configNBT.getInteger("daylightcycle"));
                this.clouds = configNBT.getBoolean("clouds");
                this.weather = configNBT.getBoolean("weather");
                this.biome = Biome.REGISTRY.getObject(new ResourceLocation(configNBT.getString("biome")));
                this.vegetation = configNBT.getBoolean("vegetation");
                this.allowGenerationChanges = configNBT.getBoolean("allow_generation_changes");
                if (configNBT.hasKey("blocks")) {
                    this.layers = LayersFromString(configNBT.getString("blocks"));
                }
            } catch (IOException e) {
                PersonalWorlds.log
                        .error(String.format("Could not load config in %s! Error:", config.getAbsolutePath()));
                throw new RuntimeException(e);
            }
        }
    }

    public DimensionConfig copy() {
        return copy(this.dimID);
    }

    public DimensionConfig copy(int dimID) {
        DimensionConfig copy = new DimensionConfig(dimID);
        copy.needsSaving = true;
        copy.skyColor = this.skyColor;
        copy.starsVisibility = this.starsVisibility;
        copy.daylightCycle = this.daylightCycle;
        copy.clouds = this.clouds;
        copy.weather = this.weather;
        copy.generateTrees = this.generateTrees;
        copy.layers = this.layers;
        copy.allowGenerationChanges = this.allowGenerationChanges;
        copy.biome = this.biome;
        copy.vegetation = this.vegetation;
        copy.spawnMonsters = this.spawnMonsters;
        copy.spawnPassiveMobs = this.spawnPassiveMobs;
        return copy;
    }

    public void copyFrom(DimensionConfig src) {
        this.needsSaving = true;
        this.skyColor = src.skyColor;
        this.starsVisibility = src.starsVisibility;
        this.daylightCycle = src.daylightCycle;
        this.clouds = src.clouds;
        this.weather = src.weather;
        this.spawnMonsters = src.spawnMonsters;
        this.spawnPassiveMobs = src.spawnPassiveMobs;
    }

    public void update() {
        if (!config.exists()) {
            try {
                config.createNewFile();
            } catch (IOException e) {
                PersonalWorlds.log
                        .error(String.format("Could not create config in %s! Error:", config.getAbsolutePath()));
                throw new RuntimeException(e);
            }
        }
        NBTTagCompound configNBT = new NBTTagCompound();
        configNBT.setInteger("sky_color", this.skyColor);
        configNBT.setFloat("stars_visibility", this.starsVisibility);
        configNBT.setBoolean("spawn_monsters", this.spawnMonsters);
        configNBT.setBoolean("spawn_passive_mobs", this.spawnPassiveMobs);
        configNBT.setBoolean("vegetation", this.vegetation);
        configNBT.setBoolean("clouds", this.clouds);
        configNBT.setBoolean("generate_trees", this.generateTrees);
        configNBT.setInteger("daylightcycle", this.daylightCycle.ordinal());
        configNBT.setBoolean("weather", this.weather);
        configNBT.setString("biome", this.biome.getRegistryName().toString());
        configNBT.setBoolean("allow_configuration_changes", this.allowGenerationChanges);
        if (!layers.isEmpty()) {
            configNBT.setString("blocks", LayersToString(layers));
        }
        try {
            CompressedStreamTools.writeCompressed(configNBT, Files.newOutputStream(config.toPath()));
        } catch (IOException e) {
            PersonalWorlds.log
                    .error(String.format("Could not save config in %s! Error:", config.getAbsolutePath()));
            throw new RuntimeException(e);
        }
    }

    public static DimensionConfig getConfig(int dimId, boolean isClient) {
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            return CommonProxy.getDimensionConfigs(isClient).get(dimId);
        }
    }

    public void registerWithDimManager(boolean isClient) {
        this.config = new File(
                DimensionManager.getCurrentSaveRootDirectory() + "/" +
                        "personal_world_" + dimID + "/PWConfig.dat");
        if (!DimensionManager.isDimensionRegistered(dimID)) {
            if (!isClient) {
                if (!registerDimension(dimID)) {
                    log.fatal("Failed to register dimension {} in PWWorlds.dat!", dimID);
                    return;
                }
            }
            DimensionManager.registerDimension(dimID, dimType);
            PersonalWorlds.log.info("DimensionConfig registered for dim {}, client {}", dimID, isClient);
        }
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            if (!CommonProxy.getDimensionConfigs(isClient).containsKey(dimID)) {
                CommonProxy.getDimensionConfigs(isClient).put(dimID, this);
            } else {
                CommonProxy.getDimensionConfigs(isClient).get(dimID).copyFrom(this);
            }
        }
        if (!isClient) {
            DimensionManager.initDimension(dimID);
            this.needsSaving = false;
            this.allowGenerationChanges = false;
            this.update();
        }
    }

//    private boolean registerDimension(int dimID) {
//        NBTTagCompound nbtTagCompound;
//        File file = new File(DimensionManager.getCurrentSaveRootDirectory() + "/PWWorlds.dat");
//        if (!file.exists()) {
//            try {
//                file.createNewFile();
//            } catch (IOException e) {
//                log.error("Could not create PWWorlds.dat! Error:");
//                throw new RuntimeException(e);
//            }
//            nbtTagCompound = new NBTTagCompound();
//        } else {
//            try {
//                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
//            } catch (IOException e) {
//                log.error("Could not read PWWorlds.dat! Error:");
//                throw new RuntimeException(e);
//            }
//        }
//        int[] intArray;
//        if (nbtTagCompound.hasKey("dimensions")) {
//            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
//            intArray = new int[dimensions.length + 1];
//            System.arraycopy(dimensions, 0, intArray, 0, dimensions.length);
//            intArray[dimensions.length] = dimID;
//            nbtTagCompound.setIntArray("dimensions", intArray);
//        } else {
//            intArray = new int[1];
//            intArray[0] = dimID;
//            nbtTagCompound.setIntArray("dimensions", intArray);
//        }
//        try {
//            CompressedStreamTools.writeCompressed(nbtTagCompound, Files.newOutputStream(file.toPath()));
//        } catch (IOException e) {
//            log.error("Could not save PWWorlds.dat! Error:");
//            throw new RuntimeException(e);
//        }
//        return true;
//    }
    private synchronized boolean registerDimension(int dimID) {
        Path filePath = Paths.get(DimensionManager.getCurrentSaveRootDirectory().getPath(), "PWWorlds.dat");
        NBTTagCompound nbtTagCompound;

        // 读取或创建文件
        if (!Files.exists(filePath)) {
            nbtTagCompound = new NBTTagCompound();
        } else {
            try (InputStream is = Files.newInputStream(filePath)) {
                nbtTagCompound = CompressedStreamTools.readCompressed(is);
            } catch (IOException e) {
                log.error("读取维度数据文件失败: {}", filePath, e);
                return false; // 改为返回false而不是抛出异常
            }
        }

        // 处理维度ID列表
        int[] existingIDs = nbtTagCompound.hasKey("dimensions") ?
                nbtTagCompound.getIntArray("dimensions") : new int[0];

        // 检查是否已存在
        if (Arrays.stream(existingIDs).anyMatch(id -> id == dimID)) {
            log.warn("维度ID {} 已存在，跳过注册", dimID);
            return false;
        }

        // 追加新ID
        int[] newIDs = Arrays.copyOf(existingIDs, existingIDs.length + 1);
        newIDs[existingIDs.length] = dimID;
        nbtTagCompound.setIntArray("dimensions", newIDs);

        // 保存文件
        try (OutputStream os = Files.newOutputStream(filePath)) {
            CompressedStreamTools.writeCompressed(nbtTagCompound, os);
            return true;
        } catch (IOException e) {
            log.error("保存维度数据文件失败: {}", filePath, e);
            return false;
        }
    }
    private boolean unregisterDimension(int dimID) {
        File file = new File(DimensionManager.getCurrentSaveRootDirectory() + "/PWWorlds.dat");
        if (file.exists()) {
            NBTTagCompound nbtTagCompound;
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                log.error("Could not read PWWorlds.dat! Error:");
                throw new RuntimeException(e);
            }
            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
            List<Integer> dimensionsList = Arrays.stream(dimensions).boxed().collect(Collectors.toList());
            dimensionsList.removeIf(Predicate.isEqual(dimID));
            nbtTagCompound.setIntArray("dimensions", dimensionsList.stream().mapToInt(Integer::intValue).toArray());
            try {
                CompressedStreamTools.writeCompressed(nbtTagCompound, Files.newOutputStream(file.toPath()));
            } catch (IOException e) {
                log.error("Could not save PWWorlds.dat! Error:");
                throw new RuntimeException(e);
            }
        }
        return true;
    }

    public String LayersToString(List<FlatLayerInfo> flatLayerInfos) {
        StringBuilder sb = new StringBuilder();
        for (int i = flatLayerInfos.size() - 1; i != -1; i--) {
            sb.append(flatLayerInfos.get(i).toString());
            if (i != 0) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public static List<FlatLayerInfo> LayersFromString(String string) {
        int currY = 0;
        ArrayList<FlatLayerInfo> flatLayerInfos = new ArrayList<>();
        String[] stringArray = string.split(",");
        for (int i = stringArray.length - 1; i > -1; i--) {
            String string1 = stringArray[i];
            FlatLayerInfo flatLayerInfo = LayerFromString(string1);
            flatLayerInfo.setMinY(currY);
            flatLayerInfos.add(flatLayerInfo);
            currY += flatLayerInfo.getLayerCount();
            if (currY > 255)
                break;
        }
        return flatLayerInfos;
    }

    public static FlatLayerInfo LayerFromString(String string) {
        int metadata = 0;
        Block block;
        String[] stringArray = string.split("\\*", 2);
        int layers = 1;
        if (stringArray.length == 2) {
            layers = Integer.parseInt(stringArray[0]);
            if (layers >= 256) {
                layers = 1;
            }
            if (layers < 0) {
                layers = 1;
            }
            string = stringArray[1];
        }
        String[] stringArray1 = string.split(":");
        if (stringArray1.length == 3) {
            metadata = Integer.parseInt(stringArray1[2]);
            string = stringArray1[0] + ":" + stringArray1[1];
        }
        block = Block.REGISTRY.getObject(new ResourceLocation(string));
        return new FlatLayerInfo(3, layers, block, metadata);
    }

    public static DimensionConfig readFromPacket(MCDataInput packet) {
        DimensionConfig cfg = new DimensionConfig(packet.readInt());
        cfg.setSkyColor(packet.readInt());
        cfg.setStarVisibility(packet.readFloat());
        cfg.setDaylightCycle(DaylightCycle.fromOrdinal(packet.readInt()));
        cfg.enableClouds(packet.readBoolean());
        cfg.enableWeather(packet.readBoolean());
        cfg.setGeneratingTrees(packet.readBoolean());
        cfg.setGeneratingVegetation(packet.readBoolean());
        cfg.setBiome(Biome.REGISTRY.getObject(new ResourceLocation(packet.readString())));
        cfg.allowGenerationChanges = packet.readBoolean();
        cfg.setSpawnMonsters(packet.readBoolean());
        cfg.setSpawnPassiveMobs(packet.readBoolean());
        int layers = packet.readVarInt();
        for (int i = 0; i < layers; i++) {
            int minY = packet.readInt();
            int layerCount = packet.readVarInt();
            Block block = Block.getBlockById(packet.readVarInt());
            byte meta = packet.readByte();
            if (block == null) {
                log.error("Block was missing");
                continue;
            }
            FlatLayerInfo fli = new FlatLayerInfo(3, layerCount, block, meta);
            fli.setMinY(minY);
            cfg.layers.add(fli);
        }
        return cfg;
    }

    public void writeToPacket(MCDataOutput packet) {
        packet.writeInt(dimID);
        packet.writeInt(skyColor);
        packet.writeFloat(starsVisibility);
        packet.writeInt(daylightCycle.ordinal());
        packet.writeBoolean(clouds);
        packet.writeBoolean(weather);
        packet.writeBoolean(generateTrees);
        packet.writeBoolean(vegetation);
        packet.writeString(biome.getRegistryName().toString());
        packet.writeBoolean(allowGenerationChanges);
        packet.writeBoolean(spawnMonsters);
        packet.writeBoolean(spawnPassiveMobs);
        packet.writeVarInt(layers.size());
        for (FlatLayerInfo fli : layers) {
            packet.writeInt(fli.getMinY());
            packet.writeVarInt(fli.getLayerCount());
            packet.writeVarInt(Block.getIdFromBlock(fli.getLayerMaterial().getBlock()));
            packet.writeByte((byte) fli.getLayerMaterial().getBlock().getMetaFromState(fli.getLayerMaterial()));
        }
    }

    public enum DaylightCycle {

        SUN,
        MOON,
        CYCLE;

        public static DaylightCycle fromOrdinal(int ordinal) {
            return (ordinal < 0 || ordinal >= values().length) ? DaylightCycle.CYCLE : values()[ordinal];
        }
    }

    public String getLayersAsString() {
        return LayersToString(this.layers);
    }

    public void setLayers(String preset) {
        this.layers = LayersFromString(preset);
    }

    public int getGroundLevel() {
        if (layers.isEmpty()) {
            return 128;
        }
        int y = 0;
        for (FlatLayerInfo info : layers) {
            y += info.getLayerCount();
        }
        return MathHelper.clamp(y, 0, 255);
    }

    public void setDimID(int dimID) {
        this.dimID = dimID;
    }

    public void setSpawnPassiveMobs(boolean spawnPassiveMobs) {
        if (this.spawnPassiveMobs != spawnPassiveMobs) {
            this.needsSaving = true;
            this.spawnPassiveMobs = spawnPassiveMobs;
        }
    }

    public void setSpawnMonsters(boolean spawnMonsters) {
        if (this.spawnMonsters != spawnMonsters) {
            this.needsSaving = true;
            this.spawnMonsters = spawnMonsters;
        }
    }

    public void setGeneratingVegetation(boolean generateVegetation) {
        if (this.vegetation != generateVegetation) {
            this.needsSaving = true;
            this.vegetation = generateVegetation;
        }
    }

    public void setBiome(Biome biome) {
        this.needsSaving = true;
        this.biome = biome;
    }

    public void setStarVisibility(float starVisibility) {
        if (this.starsVisibility != starVisibility) {
            this.needsSaving = true;
            this.starsVisibility = MathHelper.clamp(starVisibility, 0.0f, 1.0f);
        }
    }

    public void setSkyColor(int skyColor) {
        if (this.skyColor != skyColor) {
            this.needsSaving = true;
            this.skyColor = MathHelper.clamp(skyColor, 0, 0xFFFFFF);
        }
    }

    public void enableWeather(boolean enableWeather) {
        if (this.weather != enableWeather) {
            this.needsSaving = true;
            this.weather = enableWeather;
        }
    }

    public void setDaylightCycle(DaylightCycle cycle) {
        if (this.daylightCycle != cycle) {
            this.needsSaving = true;
            this.daylightCycle = cycle;
        }
    }

    public void enableClouds(boolean enableClouds) {
        if (this.clouds != enableClouds) {
            this.needsSaving = true;
            this.clouds = enableClouds;
        }
    }

    public void setGeneratingTrees(boolean generateTrees) {
        if (this.generateTrees != generateTrees) {
            this.needsSaving = true;
            this.generateTrees = generateTrees;
        }
    }

    public boolean needsSaving() {
        return needsSaving;
    }

    public boolean generateTrees() {
        return generateTrees;
    }

    public boolean cloudsEnabled() {
        return clouds;
    }

    public boolean weatherEnabled() {
        return weather;
    }

    public int getSkyColor() {
        return skyColor;
    }

    public float getStarsVisibility() {
        return starsVisibility;
    }

    public List<FlatLayerInfo> getLayers() {
        return layers;
    }

    public Biome getBiome() {
        return biome;
    }

    public boolean vegetationEnabled() {
        return vegetation;
    }

    public boolean spawnPassiveMobs() {
        return spawnPassiveMobs;
    }

    public boolean spawnMonsters() {
        return spawnMonsters;
    }

    public boolean allowGenerationChanges() {
        return allowGenerationChanges;
    }

    public DaylightCycle getDaylightCycle() {
        return daylightCycle;
    }


}
