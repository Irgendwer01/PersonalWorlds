package personalworlds.world;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import personalworlds.PersonalWorlds;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static personalworlds.PersonalWorlds.*;

public class WorldUtils {

    public static boolean createWorld(Config config, int dimID) {
        DimensionManager.registerDimension(dimID, dimType);
        DimensionManager.initDimension(dimID);
        WorldServer world = DimensionManager.getWorld(dimID);
        config.setSpawnPos(world.getSpawnPoint());
        if (!config.update()) {
            log.error(String.format("Failed to create Config for Personal World %s! Removing Personal World...", dimID));
            removeWorld(dimID);
            return false;
        }
        if (!registerDimension(dimID)) {
            log.error(String.format("Failed to register Personal World %s! Removing Personal World...", dimID));
            removeWorld(dimID);
            return false;
        }
        return true;
    }

    public static boolean removeWorld(int dimID) {
        WorldServer world = DimensionManager.getWorld(dimID);
        File worldDirectory = world.getSaveHandler().getWorldDirectory();
        DimensionManager.unloadWorld(dimID);
        DimensionManager.unregisterDimension(dimID);

        try {
            Files.delete(worldDirectory.toPath());
        } catch (IOException e) {
            log.error(String.format("Failed to remove Folder for Personal World %s! Error: %s", dimID, e));
            return false;
        }
        if (!unregisterDimension(dimID)) {
            log.error(String.format("Failed to unregister Personal World %s!", dimID));
            return false;
        }
        return true;
    }

    private static boolean registerDimension(int dimID) {
        NBTTagCompound nbtTagCompound;
        File file = new File(server.getWorld(0).getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not create PWWorlds.dat! Error: %s", e));
                return false;
            }
            nbtTagCompound = new NBTTagCompound();
        } else {
            try {
                nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
                return false;
            }
        }
        int[] intArray;
        if (nbtTagCompound.hasKey("dimensions")) {
            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
            intArray = new int[dimensions.length+1];
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
            PersonalWorlds.log.error(String.format("Could not save PWWorlds.dat! Error: %s", e));
            return false;
        }
        return true;
    }

    private static boolean unregisterDimension(int dimID) {
        File file = new File(server.getWorld(0).getSaveHandler().getWorldDirectory() + "/PWWorlds.dat");
        if (file.exists()) {
            NBTTagCompound nbtTagCompound;
            try {
               nbtTagCompound = CompressedStreamTools.readCompressed(Files.newInputStream(file.toPath()));
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not read PWWorlds.dat! Error: %s", e));
                return false;
            }
            int[] dimensions = nbtTagCompound.getIntArray("dimensions");
            List<Integer> dimensionsList = Arrays.stream(dimensions).boxed().collect(Collectors.toList());
            dimensionsList.removeIf(Predicate.isEqual(dimID));
            nbtTagCompound.setIntArray("dimensions", dimensionsList.stream().mapToInt(Integer::intValue).toArray());
            try {
                CompressedStreamTools.writeCompressed(nbtTagCompound, Files.newOutputStream(file.toPath()));
            } catch (IOException e) {
                PersonalWorlds.log.error(String.format("Could not save PWWorlds.dat! Error: %s", e));
                return false;
            }
        }
        return true;
    }
}
