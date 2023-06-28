package personalworlds.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.DimensionManager;
import personalworlds.world.PWTeleporter;

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
        DimensionManager.registerDimension(ID, DimensionType.getById(10));
        DimensionManager.initDimension(ID);
        WorldServer world = DimensionManager.getWorld(ID);
        world.provider.biomeProvider = new BiomeProviderSingle(Biomes.PLAINS);
        player.changeDimension(ID, new PWTeleporter(world));
        int x = player.getPosition().getX();
        int y = player.getPosition().getY();
        int z = player.getPosition().getZ();
        world.setBlockState(new BlockPos(x, y-1, z), Blocks.STONEBRICK.getDefaultState());
    }
}
