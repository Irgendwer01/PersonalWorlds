package personalworlds.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.viewport.GuiContext;
import com.cleanroommc.modularui.theme.WidgetTheme;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.FlatLayerInfo;
import personalworlds.PWConfig;
import personalworlds.PersonalWorlds;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.world.DimensionConfig;

import java.util.ArrayList;
import java.util.function.Consumer;

public class PWGuiMUI {

    private ArrayList<IWidget> biomeList = new ArrayList<>();
    private ArrayList<IWidget> blockList = new ArrayList<>();
    private ListWidget widget;
    private final DimensionConfig dimensionConfig = new DimensionConfig();


    public ModularScreen createGUI() {
        for (IBlockState blockState : PWConfig.getAllowedBlocks()) {
            Block block = blockState.getBlock();
            int itemMeta = block.damageDropped(blockState);
            int meta = block.getMetaFromState(blockState);
            ItemStack stack = new ItemStack(block, 1, itemMeta);
            blockList.add(new ButtonWidget<>().size(15, 15)
                    .overlay(new ItemDrawable(stack))
                    .addTooltipLine(stack.getDisplayName())
                    .onMousePressed(i -> {
                        dimensionConfig.getLayers().add(new FlatLayerInfo(3, 1, block, meta));
                        redrawLayers();
                       return true;
                    }));
        }
        for (Biome biome : PWConfig.getAllowedBiomes()) {
            biomeList.add(new ButtonWidget<>().size(85, 15).overlay(IKey.str(biome.getBiomeName())));
        }

        ModularPanel panel = ModularPanel.defaultPanel("PWGUI");
        panel.size(200, 200);
        panel.child(IKey.str("Personal Portal").asWidget()
                    .top(7).left(7));
        panel.child(new ButtonWidget<>()
                        .overlay(IKey.str("Done"))
                        .size(60, 20)
                        .bottom(9).right(9));
        panel.child(new ButtonWidget<>()
                .overlay(IKey.str("Cancel"))
                .size(60, 20)
                .bottom(9).left(9)
                .onMousePressed(i ->  {
                    panel.closeIfOpen();
                    return true;
                }));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .bottom(90).left(9)
                .overlay(UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark").build()));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .bottom(65).left(9)
                .overlay(UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark").build()));
        panel.child(new ListWidget<>(biomeList)
                        .bottom(40).left(9)
                        .size(85, 15));
        panel.child(new ListWidget<>(blockList)
                .top(9).right(60)
                .size(15, 150));
        widget = new ListWidget<>()
                .top(9).right(20)
                .size(15, 150);
        panel.child(widget);
        return new ModularScreen(panel);
    }

    private void redrawLayers() {
        for (FlatLayerInfo flatLayerInfo : dimensionConfig.getLayers()) {
            IBlockState blockState = flatLayerInfo.getLayerMaterial();
            Block block = flatLayerInfo.getLayerMaterial().getBlock();
            int itemMeta = block.damageDropped(blockState);
            ItemStack stack = new ItemStack(block, 1, itemMeta);
            int count = widget.getValues().size();
            widget.getValues().clear();
            widget.add(new ButtonWidget<>().size(15, 15)
                    .overlay(new ItemDrawable(stack))
                    .addTooltipLine(stack.getDisplayName()), count);
        }
        WidgetTree.resize(widget);
    }
}
