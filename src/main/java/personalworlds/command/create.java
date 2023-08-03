package personalworlds.command;

import net.minecraft.block.Block;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraftforge.common.DimensionManager;
import personalworlds.PersonalWorlds;
import personalworlds.world.Config;
import personalworlds.world.PWTeleporter;
import personalworlds.world.PWWorldProvider;

import static personalworlds.PersonalWorlds.dimType;

public class create extends CommandBase {
    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/create";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        int ID = 5;
        EntityPlayerMP player = getCommandSenderAsPlayer(sender);
        DimensionManager.registerDimension(ID, dimType);
        DimensionManager.initDimension(ID);
        WorldServer world = DimensionManager.getWorld(ID);
        ((PWWorldProvider)world.provider).setConfig(new Config(PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/personal_world_" + ID + "/PWConfig.dat"));
        ((PWWorldProvider)world.provider).setBlocks(new Block[]{Blocks.DIRT, Blocks.GRASS, Blocks.STONEBRICK});
        Config config = ((PWWorldProvider)world.provider).getConfig();
        config.setPassiveSpawn(true);
        config.setPopulate(true);
        config.setStarsVisibility(1F);
        config.setSkyColor(0x07f26d);
        config.setOwner(player.getUniqueID());
        config.setSpawnPos(world.getSpawnCoordinate());
        config.update();
        world.provider.biomeProvider = new BiomeProviderSingle(Biomes.PLAINS);
        player.changeDimension(ID, new PWTeleporter(world));
    }
}
