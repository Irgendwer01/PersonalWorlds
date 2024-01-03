package personalworlds.world;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.packet.PacketCustom;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DimensionType;
import net.minecraft.world.gen.FlatLayerInfo;

import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.common.DimensionManager;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;
import personalworlds.proxy.CommonProxy;

import static personalworlds.PersonalWorlds.*;

public class DimensionConfig {

    private File config;

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

    public static final String PRESET_UW_VOID = "";
    public static final String PRESET_UW_GARDEN = "minecraft:bedrock,3*minecraft:dirt,minecraft:grass";
    public static final String PRESET_UW_MINING = "4*minecraft:bedrock,58*minecraft:stone,minecraft:dirt,minecraft:grass";
    public static final Pattern PRESET_VALIDATION_PATTERN = Pattern
            .compile("^(?:[1-9][0-9]*\\*)?(?:[a-zA-Z]+):(?:[a-zA-Z]+)(?:,(?:[1-9][0-9]*\\*)?(?:[a-zA-Z]+):(?:[a-zA-Z]+))*$");


    public DimensionConfig() {
    }

    public DimensionConfig(int dimID) {
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

    public static DimensionConfig fromPacket(PacketCustom packet) {
        DimensionConfig cfg = new DimensionConfig();
        cfg.readFromPacket(packet);
        return cfg;
    }

    public void readFromPacket(MCDataInput packet) {
        this.needsSaving = true;
        this.setSkyColor(packet.readInt());
        this.setStarsVisibility(packet.readFloat());
        this.setDaylightCycle(Enums.DaylightCycle.fromOrdinal(packet.readVarInt()));
        this.setClouds(packet.readBoolean());
        this.setWeather(packet.readBoolean());
        this.setVegetation(packet.readBoolean());
        this.setGenerateTrees(packet.readBoolean());
        String temp = packet.readString();
        this.layers = LayersFromString(temp);
    }

    public void writeToPacket(MCDataOutput packet) {
        packet.writeInt(skyColor);
        packet.writeFloat(starsVisibility);
        packet.writeInt(daylightCycle.ordinal());
        packet.writeBoolean(clouds);
        packet.writeBoolean(weather);
        packet.writeBoolean(vegetation);
        packet.writeBoolean(generateTrees);
        String temp = LayersToString(layers);
        packet.writeString(temp);
    }

    public boolean copyFrom(DimensionConfig source, boolean copySaveInfo, boolean copyVisualInfo, boolean copyGenerationInfo) {
        this.needsSaving = false;
        if(copySaveInfo) {
            this.needsSaving = source.needsSaving;
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

    public static DimensionConfig getForDimension(int dimId, boolean isClient) {
        synchronized (CommonProxy.getDimensionConfigs(isClient)) {
            return CommonProxy.getDimensionConfigs(isClient).get(dimId);
        }
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

    //string = stringBuilder.append(string).append(flatLayerInfo.toString()).append(",").toString();

    public static List<FlatLayerInfo> LayersFromString(String string) {
        int currY = 0;
        ArrayList<FlatLayerInfo> flatLayerInfos = new ArrayList<>();
        String[] stringArray = string.split(",");
        for (String string1 : stringArray) {
            currY++;
            FlatLayerInfo flatLayerInfo = LayerFromString(string1);
            flatLayerInfo.setMinY(PWConfig.minY + currY);
            flatLayerInfos.add(flatLayerInfo);
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

    public String getLayersAsString() {
        return LayersToString(this.layers);
    }

    public static boolean canUseLayers(String preset, boolean onClient) {
        if (preset == null) {
            preset = "";
        }
        if (preset.equals(PRESET_UW_GARDEN) || preset.equals(PRESET_UW_VOID) || preset.equals(PRESET_UW_MINING)) {
            return true;
        }
        List<FlatLayerInfo> infos = LayersFromString(preset);
        if (infos.isEmpty() && !preset.trim().isEmpty()) {
            return false;
        }
        for (FlatLayerInfo info : infos) {

            IBlockState blockState = info.getLayerMaterial();
            if(PWConfig.getAllowedBlocks().contains(blockState)) {
                return false;
            }
        }
        return true;
    }

    public void setLayers(String preset) {
        this.layers = LayersFromString(preset);
    }

    public void registerWithDimManager(int dimID, boolean isClient) {
        this.config = new File(
                PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" +
                        "personal_world_" + dimID + "/PWConfig.dat");
        if(!DimensionManager.isDimensionRegistered(dimID)) {
            if (!isClient) {
                if (!registerDimension(dimID)) {
                    log.fatal("Failed to register dimension {} in PWWorlds.dat!", dimID);
                    return;
                }
            }
            DimensionManager.registerDimension(dimID, dimType);
            PersonalWorlds.log.info("DimensionConfig registered for dim {}, client {}", dimID, isClient, new Throwable());
        }
        if (!isClient) {
            this.needsSaving = false;
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
        File file = new File(server.getWorld(0).getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                log.error(String.format("Could not create PWWorlds.dat! Error: %s", e));
                return false;
            }
            nbtTagCompound = new NBTTagCompound();
        } else {
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
                return false;
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
            log.error(String.format("Could not save PWWorlds.dat! Error: %s", e));
            return false;
        }
        return true;
    }

    private boolean unregisterDimension(int dimID) {
        File file = new File(server.getWorld(0).getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
        if (file.exists()) {
            NBTTagCompound nbtTagCompound;
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
                return false;
            }
            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
            List<Integer> dimensionsList = Arrays.stream(dimensions).boxed().collect(Collectors.toList());
            dimensionsList.removeIf(Predicate.isEqual(dimID));
            nbtTagCompound.setIntArray("dimensions", dimensionsList.stream().mapToInt(Integer::intValue).toArray());
            try {
                CompressedStreamTools.writeCompressed(nbtTagCompound, Files.newOutputStream(file.toPath()));
            } catch (IOException e) {
                log.error(String.format("Could not save PWWorlds.dat! Error: %s", e));
                return false;
            }
        }
        return true;
    }
}
