package personalworlds.blocks.tile;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.layout.ILayoutWidget;
import com.cleanroommc.modularui.api.widget.IGuiAction;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.sync.GuiSyncHandler;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.utils.Color;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.layout.Column;
import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;

public class TilePersonalPortal extends TileEntity implements IGuiHolder {

    private ArrayList<Block> blocks = new ArrayList<>();
    private boolean treesEnabled = false;
    private int grassCount = 0;
    private int bedrockCount = 0;
    private int stoneBrickCount = 0;
    private int dirtCount = 0;
    private IWidget layers = new Column()
            .top(7)
            .right(5);
    @Override
    public void buildSyncHandler(GuiSyncHandler guiSyncHandler, EntityPlayer entityPlayer) {

    }

    @Override
    public ModularScreen createClientGui(EntityPlayer entityPlayer) {
        return ModularScreen.simple("personalportal_gui", this::createPanel);
    }

    public ModularPanel createPanel(GuiContext context) {
        Rectangle colorPicker = new Rectangle().setColor(Color.RED.normal);
        ModularPanel panel = ModularPanel.defaultPanel(context);
        panel.child(new ButtonWidget<>()
                        .bottom(7)
                        .right(7)
                        .size(40, 15)
                        .overlay(IKey.str("Create").color(Color.GREEN.normal)));
        panel.child(new ButtonWidget<>()
                .top(7)
                .left(7)
                .size(60, 40)
                .background(colorPicker)
                .onMousePressed(pressed -> {
                    panel.getScreen().openPanel(new ColorPickerDialog(context, colorPicker::setColor, colorPicker.getColor(), true));
                    return true;
                }));
        panel.child(new ButtonWidget<>()
                .bottom(14)
                .left(14)
                .size(30)
                .overlay(IKey.dynamic(this::TreesEnabled))
                .onMousePressed(pressed -> {
                    treesEnabled = !treesEnabled;
                    return true;
                }));
        panel.child(new Column()
                        .top(16)
                        .right(25)
                        .size(60)
                .child(new ButtonWidget<>()
                        .overlay(new ItemDrawable(new ItemStack(Blocks.GRASS)), IKey.dynamic(this::GrassCount).alignment(Alignment.BottomRight).scale(0.8F))
                        .addTooltipLine(Blocks.GRASS.getLocalizedName())
                        .onKeyPressed((key, key2) -> {
                            return true;
                        })
                .child(new ButtonWidget<>()
                        .overlay(new ItemDrawable(new ItemStack(Blocks.DIRT)), IKey.dynamic(this::DirtCount).alignment(Alignment.BottomRight).scale(0.8F))
                        .addTooltipLine(Blocks.DIRT.getLocalizedName()))
                .child(new ButtonWidget<>()
                        .overlay(new ItemDrawable(new ItemStack(Blocks.STONEBRICK)), IKey.dynamic(this::StoneBrickCount).alignment(Alignment.BottomRight).scale(0.8F))
                        .addTooltipLine(Blocks.STONEBRICK.getLocalizedName()))
                .child(new ButtonWidget<>()
                        .overlay(new ItemDrawable(new ItemStack(Blocks.BEDROCK)), IKey.dynamic(this::BedrockCount).alignment(Alignment.BottomRight).scale(0.8F))
                        .addTooltipLine(Blocks.BEDROCK.getLocalizedName()))));
        return panel;
    }

    private String TreesEnabled() {
        return treesEnabled ? "true" : "false";
    }

    private String DirtCount() {
        return String.valueOf(dirtCount);
    }

    private String GrassCount() {
        return String.valueOf(grassCount);
    }

    private String BedrockCount() {
        return String.valueOf(bedrockCount);
    }

    private String StoneBrickCount() {
        return String.valueOf(dirtCount);
    }
}
