package personalworlds.world;

import lombok.Setter;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DimensionType;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.gen.IChunkGenerator;
import personalworlds.PersonalWorlds;

import javax.annotation.Nullable;

public class PWWorldProvider extends WorldProvider {

    @Setter
    private Config config;

    public PWWorldProvider() {
    }

    public Config getConfig() {
        if (config == null) {
            config = new Config(PersonalWorlds.server.getWorld(0).getSaveHandler().getWorldDirectory().getAbsolutePath() + "/" + getSaveFolder() + "/PWConfig.dat");
        }
        return config;
    }

    @Override
    public DimensionType getDimensionType() {
        return DimensionType.OVERWORLD;
    }

    @Override
    public IChunkGenerator createChunkGenerator() {
        return new PWChunkGenerator(this.world);
    }

    @Nullable
    @Override
    public String getSaveFolder() {
        return "personal_world_" + this.getDimension();
    }

    @Override
    public float getStarBrightness(float par1) {
        return getConfig().getStarsVisibility();
    }

    @Override
    public Vec3d getFogColor(float p_76562_1_, float p_76562_2_) {
        int color = getConfig().getSkyColor();

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        return new Vec3d(red, green, blue);
    }

    @Override
    public Vec3d getSkyColor(Entity cameraEntity, float partialTicks) {
        return getFogColor(0.0F, partialTicks);
    }
}
