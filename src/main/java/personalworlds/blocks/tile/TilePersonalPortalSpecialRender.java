package personalworlds.blocks.tile;

import com.cleanroommc.modularui.api.value.IStringValue;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

public class TilePersonalPortalSpecialRender extends TileEntitySpecialRenderer<TilePersonalPortal> {

    @Override
    public void render(TilePersonalPortal te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

        if (te.getDisplayName() != null) {
            this.setLightmapDisabled(true);
            this.drawNameplate(te, te.getDisplayName().getFormattedText(), x, y, z, 24);
            this.setLightmapDisabled(false);
        }
    }

    @Override
    protected void drawNameplate(TilePersonalPortal te, String str, double x, double y, double z, int maxDistance) {
        super.drawNameplate(te, str, x, y, z, maxDistance);
    }
}
