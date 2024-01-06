package personalworlds.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.FlatLayerInfo;

import com.cleanroommc.modularui.api.drawable.IDrawable;
import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.value.DoubleValue;
import com.cleanroommc.modularui.widget.ParentWidget;
import com.cleanroommc.modularui.widget.WidgetTree;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ColorPickerDialog;
import com.cleanroommc.modularui.widgets.ListWidget;
import com.cleanroommc.modularui.widgets.SliderWidget;

import personalworlds.PWConfig;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.world.DimensionConfig;

public class PWGuiMUI {

    private final TilePersonalPortal tpp;

    private final IDrawable checkmark = UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark")
            .build();
    private final IDrawable crossmark = UITexture.builder().imageSize(16, 16).location("personalworlds", "crossmark")
            .build();

    private final ArrayList<String> layers = new ArrayList<>();
    private final ArrayList<IWidget> blockList = new ArrayList<>();
    private ModularPanel panel;
    private final DimensionConfig dimensionConfig = new DimensionConfig(0);
    private ListWidget layersWidget;
    private ListWidget biomesWidget;
    private boolean firstDraw = true;

    public PWGuiMUI(TilePersonalPortal tpp) {
        this.tpp = tpp;
    }

    public ModularScreen createGUI() {
        Consumer<Integer> skyColor = color -> {
            dimensionConfig.setSkyColor(color);
        };
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
                        layers.add(new FlatLayerInfo(3, 1, block, meta).toString());
                        redrawLayers();
                        return true;
                    }));
        }
        ModularPanel panel = ModularPanel.defaultPanel("PWGUI");
        this.panel = panel;
        panel.size(200, 200);
        panel.child(IKey.str("Personal Portal").asWidget()
                .top(7).left(7));
        panel.child(new ButtonWidget<>()
                .overlay(IKey.str("Done"))
                .size(60, 20)
                .bottom(9).right(9)
                .onMousePressed(i -> {
                    Packets.INSTANCE.sendChangeWorldSettings(tpp, dimensionConfig).sendToServer();
                    panel.closeIfOpen();
                    return true;
                }));
        panel.child(new ButtonWidget<>()
                .overlay(IKey.str("Cancel"))
                .size(60, 20)
                .bottom(9).left(9)
                .onMousePressed(i -> {
                    panel.closeIfOpen();
                    return true;
                }));
        panel.child(IKey.str("Layers").asWidget()
                .top(7).right(175));
        panel.child(IKey.str("Biome").asWidget()
                .bottom(145).left(45));
        panel.child(IKey.str("Presets").asWidget()
                .bottom(145).left(135));
        panel.child(IKey.str("Settings").asWidget()
                .top(22).left(35));
        panel.child(new SliderWidget()
                .size(100, 15)
                .top(35).left(7)
                .value(new DoubleValue(dimensionConfig.getStarVisibility()))
                .onUpdateListener(widget -> {
                    dimensionConfig.setStarVisibility((float) widget.getSliderValue());
                    widget.overlay(IKey.str(
                            String.format("Star Brightness: %.0f", dimensionConfig.getStarVisibility() * 100) + "%")
                            .color(0xFFFFFF).scale(0.7F));
                })
                .bounds(0F, 1F)
                .background(GuiTextures.MC_BUTTON_DISABLED));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .top(53).left(8)
                .overlay(checkmark)
                .addTooltipLine("Trees").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.setGeneratingTrees(!dimensionConfig.generateTrees());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.generateTrees() ? checkmark : crossmark);
                }));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .top(53).left(30)
                .overlay(checkmark)
                .addTooltipLine("Vegetation").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.setGeneratingVegetation(!dimensionConfig.generateVegetation());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.generateVegetation() ? checkmark : crossmark);
                }));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .top(53).left(53)
                .overlay(checkmark)
                .addTooltipLine("Passive Spawning").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.setPassiveSpawn(!dimensionConfig.passiveSpawn());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.passiveSpawn() ? checkmark : crossmark);
                }));
        panel.child(new ListWidget<>(blockList)
                .top(20).right(60)
                .size(15, 115));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .bottom(65).left(20)
                .onMousePressed(mouse -> {
                    ColorPickerDialog colorPickerDialog = new ColorPickerDialog("Sky Color", skyColor,
                            dimensionConfig.getSkyColor(), false);
                    colorPickerDialog.updateColor(dimensionConfig.getSkyColor());
                    colorPickerDialog.setDraggable(true);
                    colorPickerDialog.openIn(panel.getScreen());
                    return true;
                }));
        redrawBiomeList();
        this.firstDraw = false;
        return new ModularScreen(panel);
    }

    private void redrawBiomeList() {
        if (biomesWidget != null) {
            panel.getChildren().removeIf(widget -> widget.equals(biomesWidget));
        }
        ArrayList<IWidget> biomeList = new ArrayList<>();
        for (Biome biome : PWConfig.getAllowedBiomes()) {
            biomeList.add(new ButtonWidget<>().size(100, 15)
                    .overlay(IKey.str(biome.getBiomeName()))
                    .onMousePressed(mouse -> {
                        if (dimensionConfig.getBiome() != biome) {
                            dimensionConfig.setBiome(biome);
                            redrawBiomeList();
                            return true;
                        }
                        return false;
                    })
                    .background(dimensionConfig.getBiome() == biome ? GuiTextures.MC_BUTTON_DISABLED :
                            GuiTextures.MC_BUTTON)
                    .hoverBackground(dimensionConfig.getBiome() == biome ? GuiTextures.MC_BUTTON_DISABLED :
                            GuiTextures.MC_BUTTON_HOVERED));
        }
        biomesWidget = new ListWidget<>(biomeList)
                .bottom(40).left(9)
                .size(100, 15);
        panel.child(biomesWidget);
        if (!firstDraw) {
            WidgetTree.resize(panel);
            WidgetTree.resize(biomesWidget);
        }
    }

    private void redrawLayers() {
        if (layersWidget != null) {
            panel.getChildren().removeIf(widget -> widget.equals(layersWidget));
        }
        int currY = 0;
        ArrayList<IWidget> layerWidget = new ArrayList<>();
        for (int i = 0; i < layers.size(); i++) {
            FlatLayerInfo layerInfo = DimensionConfig.LayerFromString(layers.get(i));
            AtomicInteger layerCount = new AtomicInteger(layerInfo.getLayerCount());
            IBlockState blockState = layerInfo.getLayerMaterial();
            Block block = layerInfo.getLayerMaterial().getBlock();
            int itemMeta = block.damageDropped(blockState);
            int meta = block.getMetaFromState(blockState);
            ItemStack stack = new ItemStack(block, 1, itemMeta);
            boolean arrowDown = i != layers.size() - 1 && layers.size() != 1;
            boolean arrowUp = i > 0;
            AtomicInteger finalI = new AtomicInteger(i);
            layerWidget.add(i, new ParentWidget<>().size(15, 15)
                    .overlay(new ItemDrawable(stack))
                    .addTooltipLine(stack.getDisplayName())
                    .tooltipScale(0.6F)
                    .child(IKey.str(Integer.toString(layerCount.get())).scale(0.6F).color(0xFFFFF).asWidget()
                            .align(Alignment.BottomCenter))
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.TopLeft)
                            .overlay(GuiTextures.ADD)
                            .addTooltipLine("Increase")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                FlatLayerInfo newLayer = new FlatLayerInfo(3, layerCount.incrementAndGet(), block,
                                        meta);
                                layers.set(finalI.get(), newLayer.toString());
                                redrawLayers();
                                return true;
                            }))
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.BottomLeft)
                            .overlay(GuiTextures.REMOVE)
                            .addTooltipLine("Decrease")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                if (layerCount.get() == 1) {
                                    return true;
                                }
                                FlatLayerInfo newLayer = new FlatLayerInfo(3, layerCount.decrementAndGet(), block,
                                        meta);
                                layers.set(finalI.get(), newLayer.toString());
                                redrawLayers();
                                return true;
                            }))
                    .child(new ButtonWidget<>().size(4, 4)
                            .align(Alignment.CenterLeft)
                            .overlay(GuiTextures.CROSS_TINY)
                            .addTooltipLine("Remove")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                layers.remove(finalI.get());
                                redrawLayers();
                                return true;
                            }))
                    .childIf(arrowUp, new ButtonWidget<>().size(4, 4)
                            .align(Alignment.TopRight)
                            .overlay(GuiTextures.MOVE_UP)
                            .addTooltipLine("Move up")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                Collections.swap(layers, finalI.getAndDecrement(), finalI.get());
                                redrawLayers();
                                return true;
                            }))
                    .childIf(arrowDown, new ButtonWidget<>().size(4, 4)
                            .align(Alignment.BottomRight)
                            .overlay(GuiTextures.MOVE_DOWN)
                            .addTooltipLine("Move down")
                            .tooltipScale(0.6F)
                            .onMousePressed(mouse -> {
                                Collections.swap(layers, finalI.getAndIncrement(), finalI.get());
                                redrawLayers();
                                return true;
                            })));

        }
        dimensionConfig.setLayers(toPreset(layers));
        layersWidget = new ListWidget<>(layerWidget)
                .top(20).right(20)
                .size(15, 115);
        panel.child(layersWidget);
        WidgetTree.resize(panel);
        WidgetTree.resize(layersWidget);
    }

    private String toPreset(List<String> layerList) {
        StringBuilder sb = new StringBuilder();
        for (int i = layerList.size()-1; i > -1; i--) {
            sb.append(layerList.get(i));
            if (i != 0) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
