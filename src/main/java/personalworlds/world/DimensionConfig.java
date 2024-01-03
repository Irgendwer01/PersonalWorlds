package personalworlds.world;

import static personalworlds.PersonalWorlds.*;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.FlatLayerInfo;
import net.minecraftforge.common.DimensionManager;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;
import personalworlds.proxy.CommonProxy;

public class DimensionConfig {

    private File config;

    private boolean allowGenerationChanges = true;
    private boolean passiveSpawn = false;
    private boolean generateTrees = false;
    private boolean vegetation = false;
    private boolean clouds = true;
    private boolean weather = false;
    private int skyColor = 0xc0d8ff;
    private float starsVisibility = 1F;
    private List<FlatLayerInfo> layers = new ArrayList<>();
    private boolean needsSaving = true;
    private Biome biome = Biomes.PLAINS;
    private Enums.DaylightCycle daylightCycle = Enums.DaylightCycle.CYCLE;

    public static final String PRESET_VOID = "";
    public static final String PRESET_FLAT = "minecraft:bedrock,3*minecraft:dirt,minecraft:grass";
    public static final String PRESET_MINING = "4*minecraft:bedrock,58*minecraft:stone,minecraft:dirt,minecraft:grass";
    public static final Pattern PRESET_VALIDATION_PATTERN = Pattern
            .compile(
                    "^(?:[1-9][0-9]*\\*)?(?:[a-zA-Z]+):(?:[a-zA-Z]+)(?:,(?:[1-9][0-9]*\\*)?(?:[a-zA-Z]+):(?:[a-zA-Z]+))*$");

    public DimensionConfig() {}

    public DimensionConfig(int dimID) {
        this.config = new File(
                DimensionManager.getCurrentSaveRootDirectory() + "/" +
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
                this.biome = Biome.REGISTRY.getObject(new ResourceLocation(configNBT.getString("biome")));
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

    public boolean copyFrom(DimensionConfig source, boolean copySaveInfo, boolean copyVisualInfo,
                            boolean copyGenerationInfo) {
        this.needsSaving = false;
        if (copySaveInfo) {
            this.needsSaving = source.needsSaving;
        }
        if (copyVisualInfo) {
            this.setSkyColor(source.getSkyColor());
            this.setStarVisibility(source.getStarVisibility());
            this.setDaylightCycle(source.getDaylightCycle());
            this.enableClouds(source.cloudsEnabled());
            this.enableWeather(source.weatherEnabled());
        }
        if (copyGenerationInfo) {
            this.setGeneratingTrees(source.generateTrees());
            this.setGeneratingVegetation(source.generateVegetation());
            this.layers = source.layers;
            this.needsSaving = true;
        }
        boolean modified = this.needsSaving;
        this.needsSaving = true;
        return modified;
    }

    public boolean update() {
        File worldFolder = new File(config.toString().replaceAll("/PWConfig.dat", ""));
        if (!worldFolder.exists()) {
            worldFolder.getParentFile().mkdir();
        }
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
        configNBT.setBoolean("vegetation", this.vegetation);
        configNBT.setFloat("stars_visibility", this.starsVisibility);
        configNBT.setBoolean("passive_spawn", this.passiveSpawn);
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
        return true;
    }

    public static DimensionConfig getForDimension(int dimId, boolean isClient) {
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            return CommonProxy.getDimensionConfigs(isClient).get(dimId);
        }
    }

    public void registerWithDimManager(int dimID, boolean isClient) {
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
            DimensionType dimType = DimensionType.register(String.format("personal_world_%s", dimID),
                    String.format("pw_%s", dimID), DimensionType.values().length, PWWorldProvider.class, false);
            DimensionManager.registerDimension(dimID, dimType);
            PersonalWorlds.log.info("DimensionConfig registered for dim {}, client {}", dimID, isClient);
            this.needsSaving = false;
            this.allowGenerationChanges = false;
        }
        if (!isClient) {
            this.update();
        }
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            if (!CommonProxy.getDimensionConfigs(isClient).containsKey(dimID)) {
                CommonProxy.getDimensionConfigs(isClient).put(dimID, this);
            } else {
                CommonProxy.getDimensionConfigs(isClient).get(dimID).copyFrom(this, true, true, true);
            }
        }
    }

    private boolean registerDimension(int dimID) {
        NBTTagCompound nbtTagCompound;
        File file = new File(DimensionManager.getCurrentSaveRootDirectory() + "/PWWorlds.dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error("Could not create PWWorlds.dat! Error:");
                throw new RuntimeException(e);
            }
            nbtTagCompound = new NBTTagCompound();
        } else {
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                log.error("Could not read PWWorlds.dat! Error:");
                throw new RuntimeException(e);
            }
        }
        int[] intArray;
        if (nbtTagCompound.hasKey("dimensions")) {
            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
            intArray = new int[dimensions.length + 1];
            System.arraycopy(dimensions, 0, intArray, 0, dimensions.length);
            intArray[dimensions.length] = dimID;
            nbtTagCompound.setIntArray("dimensions", intArray);
        } else {
            intArray = new int[1];
            intArray[0] = dimID;
            nbtTagCompound.setIntArray("dimensions", intArray);
        }
        try {
            CompressedStreamTools.writeCompressed(nbtTagCompound, Files.newOutputStream(file.toPath()));
        } catch (IOException e) {
            log.error("Could not save PWWorlds.dat! Error:");
            throw new RuntimeException(e);
        }
        return true;
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
        for (int i = 0; i < flatLayerInfos.size(); i++) {
            sb.append(flatLayerInfos.get(i).toString());
            if (i != flatLayerInfos.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    public static List<FlatLayerInfo> LayersFromString(String string) {
        int currY = 0;
        ArrayList<FlatLayerInfo> flatLayerInfos = new ArrayList<>();
        String[] stringArray = string.split(",");
        for (String string1 : stringArray) {
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
        String[] stringArray1 = string.split(":", 2);
        if (stringArray1.length == 3) {
            metadata = Integer.parseInt(stringArray1[2]);
            string = stringArray1[0] + stringArray1[1];
        }
        block = Block.REGISTRY.getObject(new ResourceLocation(string));
        return new FlatLayerInfo(3, layers, block, metadata);
    }

    public String getLayersAsString() {
        return LayersToString(this.layers);
    }

    public static boolean canUseLayers(String preset, boolean onClient) {
        if (preset == null) {
            preset = "";
        }
        if (preset.equals(PRESET_FLAT) || preset.equals(PRESET_VOID) || preset.equals(PRESET_MINING)) {
            return true;
        }
        List<FlatLayerInfo> infos = LayersFromString(preset);
        if (infos.isEmpty() && !preset.trim().isEmpty()) {
            return false;
        }
        for (FlatLayerInfo info : infos) {

            IBlockState blockState = info.getLayerMaterial();
            if (PWConfig.getAllowedBlocks().contains(blockState)) {
                return false;
            }
        }
        return true;
    }

    public void setLayers(String preset) {
        this.layers = LayersFromString(preset);
    }

    public List<FlatLayerInfo> getLayers() {
        return this.layers;
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

    public boolean allowGenerationChanges() {
        return allowGenerationChanges;
    }

    public void setBiome(Biome biome) {
        this.needsSaving = true;
        this.biome = biome;
    }

    public Biome getBiome() {
        return this.biome;
    }

    public float getStarVisibility() {
        return this.starsVisibility;
    }

    public boolean needsSaving() {
        return this.needsSaving;
    }

    public void setStarVisibility(float starVisibility) {
        if (this.starsVisibility != starVisibility) {
            this.needsSaving = true;
            this.starsVisibility = MathHelper.clamp(starVisibility, 0.0f, 1.0f);
        }
    }

    public boolean passiveSpawn() {
        return this.passiveSpawn;
    }

    public void setPassiveSpawn(boolean passiveSpawn) {
        this.needsSaving = true;
        this.passiveSpawn = passiveSpawn;
    }

    public int getSkyColor() {
        return this.skyColor;
    }

    public void setSkyColor(int skyColor) {
        if (this.skyColor != skyColor) {
            this.needsSaving = true;
            this.skyColor = MathHelper.clamp(skyColor, 0, 0xFFFFFF);
        }
    }

    public boolean weatherEnabled() {
        return this.weather;
    }

    public void enableWeather(boolean enableWeather) {
        if (this.weather != enableWeather) {
            this.needsSaving = true;
            this.weather = enableWeather;
        }
    }

    public Enums.DaylightCycle getDaylightCycle() {
        return this.daylightCycle;
    }

    public void setDaylightCycle(Enums.DaylightCycle cycle) {
        if (this.daylightCycle != cycle) {
            this.needsSaving = true;
            this.daylightCycle = cycle;
        }
    }

    public boolean cloudsEnabled() {
        return this.clouds;
    }

    public void enableClouds(boolean enableClouds) {
        if (this.clouds != enableClouds) {
            this.needsSaving = true;
            this.clouds = enableClouds;
        }
    }

    public boolean generateVegetation() {
        return this.vegetation;
    }

    public void setGeneratingVegetation(boolean generateVegetation) {
        if (this.vegetation != generateVegetation) {
            this.needsSaving = true;
            this.vegetation = generateVegetation;
        }
    }

    public boolean generateTrees() {
        return this.generateTrees;
    }

    public void setGeneratingTrees(boolean generateTrees) {
        if (this.generateTrees != generateTrees) {
            this.needsSaving = true;
            this.generateTrees = generateTrees;
        }
    }

    public static DimensionConfig fromPacket(PacketCustom packet) {
        DimensionConfig cfg = new DimensionConfig();
        cfg.readFromPacket(packet);
        return cfg;
    }

    public void readFromPacket(MCDataInput packet) {
        this.setSkyColor(packet.readInt());
        this.setStarVisibility(packet.readFloat());
        this.setDaylightCycle(Enums.DaylightCycle.fromOrdinal(packet.readInt()));
        this.enableClouds(packet.readBoolean());
        this.enableWeather(packet.readBoolean());
        this.setGeneratingVegetation(packet.readBoolean());
        this.setGeneratingTrees(packet.readBoolean());
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
            this.layers.add(fli);
        }
    }

    public void writeToPacket(MCDataOutput packet) {
        packet.writeInt(skyColor);
        packet.writeFloat(starsVisibility);
        packet.writeInt(daylightCycle.ordinal());
        packet.writeBoolean(clouds);
        packet.writeBoolean(weather);
        packet.writeBoolean(vegetation);
        packet.writeBoolean(generateTrees);
        packet.writeVarInt(layers.size());
        for (FlatLayerInfo fli : layers) {
            packet.writeInt(fli.getMinY());
            packet.writeVarInt(fli.getLayerCount());
            packet.writeVarInt(Block.getIdFromBlock(fli.getLayerMaterial().getBlock()));
            packet.writeByte((byte) fli.getLayerMaterial().getBlock().getMetaFromState(fli.getLayerMaterial()));
        }
    }
}
