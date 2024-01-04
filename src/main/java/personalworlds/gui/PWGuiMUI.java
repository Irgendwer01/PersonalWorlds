package personalworlds.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.FlatLayerInfo;
import personalworlds.PWConfig;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.world.DimensionConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

public class PWGuiMUI {

    private final TilePersonalPortal tpp;

    private final ArrayList<IWidget> biomeList = new ArrayList<>();
    private final ArrayList<IWidget> blockList = new ArrayList<>();
    private ModularPanel panel;
    private final DimensionConfig dimensionConfig = new DimensionConfig();
    private ListWidget layersWidget;

    public PWGuiMUI(TilePersonalPortal tpp) {
        this.tpp = tpp;
    }

    public ModularScreen createGUI() {
        for (IBlockState blockState : PWConfig.getAllowedBlocks()) {
            Block block = blockState.getBlock();
            int itemMeta = block.damageDropped(blockState);
            int meta = block.getMetaFromState(blockState);
            ItemStack stack = new ItemStack(block, 1, itemMeta);
            blockList.add(new ButtonWidget<>().size(15, 15)
                    .overlay(new ItemDrawable(stack))
                    .addTooltipLine(stack.getDisplayName())
                    .tooltipScale(0.6F)
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
        this.panel = panel;
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
        return new ModularScreen(panel);
    }

    private void redrawLayers() {
        if (layersWidget != null) {
            panel.getChildren().removeIf(widget -> widget.equals(layersWidget));
        }
        ArrayList<IWidget> layers = new ArrayList<>();
        for (int i = 0; i < dimensionConfig.getLayers().size(); i++) {
            FlatLayerInfo layerInfo = dimensionConfig.getLayers().get(i);
            AtomicInteger layerCount = new AtomicInteger(layerInfo.getLayerCount());
            IBlockState blockState = layerInfo.getLayerMaterial();
            Block block = layerInfo.getLayerMaterial().getBlock();
            int itemMeta = block.damageDropped(blockState);
            int meta = block.getMetaFromState(blockState);
            ItemStack stack = new ItemStack(block, 1, itemMeta);
            boolean arrowDown = i != dimensionConfig.getLayers().size()-1 && dimensionConfig.getLayers().size() != 1;
            boolean arrowUp = i > 0;
            AtomicInteger finalI = new AtomicInteger(i);
            layers.add(i, new ParentWidget<>().size(15, 15)
                    .overlay(new ItemDrawable(stack))
                    .addTooltipLine(stack.getDisplayName())
                    .tooltipScale(0.6F)
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.TopLeft)
                            .overlay(GuiTextures.ADD)
                            .addTooltipLine("Increase")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                FlatLayerInfo newLayer = new FlatLayerInfo(3, layerCount.incrementAndGet(), block, meta);
                                dimensionConfig.getLayers().set(finalI.get(), newLayer);
                                redrawLayers();
                                return true;
                            }))
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.BottomLeft)
                            .overlay(GuiTextures.REMOVE)
                            .addTooltipLine("Decrease")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                FlatLayerInfo newLayer = new FlatLayerInfo(3, layerCount.decrementAndGet(), block, meta);
                                dimensionConfig.getLayers().set(finalI.get(), newLayer);
                                redrawLayers();
                                return true;
                            }))
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.CenterLeft)
                            .overlay(GuiTextures.CROSS_TINY)
                            .addTooltipLine("Remove")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                dimensionConfig.getLayers().remove(finalI.get());
                                redrawLayers();
                                return true;
                            }))
                    .childIf(arrowUp, new ButtonWidget<>().size(4, 4)
                            .align(Alignment.TopRight)
                            .overlay(GuiTextures.MOVE_UP)
                            .addTooltipLine("Move up")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                Collections.swap(dimensionConfig.getLayers(), finalI.getAndDecrement(), finalI.get());
                                redrawLayers();
                                return true;
                            }))
                    .childIf(arrowDown, new ButtonWidget<>().size(4, 4)
                            .align(Alignment.BottomRight)
                            .overlay(GuiTextures.MOVE_DOWN)
                            .addTooltipLine("Move down")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                Collections.swap(dimensionConfig.getLayers(), finalI.getAndIncrement(), finalI.get());
                                redrawLayers();
                                return true;
                            })));

        }

        layersWidget = new ListWidget<>(layers)
                .top(9).right(20)
                .size(15, 150);
        panel.child(layersWidget);
        WidgetTree.resize(panel);
        WidgetTree.resize(layersWidget);
    }

}
