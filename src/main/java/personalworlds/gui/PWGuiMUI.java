package personalworlds.gui;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.cleanroommc.modularui.widgets.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
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

import personalworlds.PWConfig;
import personalworlds.packet.Packets;
import personalworlds.world.DimensionConfig;

public class PWGuiMUI {

    private int targetDim;
    private int dimID;
    private BlockPos blockPos;
    private final IDrawable checkmark = UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark")
            .build();
    private final IDrawable crossmark = UITexture.builder().imageSize(16, 16).location("personalworlds", "crossmark")
            .build();
    private List<String> layers = new ArrayList<>();
    private ModularPanel panel;
    private DimensionConfig dimensionConfig;
    private ListWidget layersWidget;
    private final CategoryList biomesWidget = new CategoryList()
            .bottom(40).left(9)
            .size(100, 15);
    private final CategoryList presetsWidget = new CategoryList().background(GuiTextures.MC_BUTTON)
                .bottom(40).right(11)
                .size(70, 15);
    private boolean firstDraw = true;

    public PWGuiMUI(int targetDim, int dimID, int x, int y, int z) {
        this.targetDim = targetDim;
        this.dimID = dimID;
        this.blockPos = new BlockPos(x, y, z);
    }

    public ModularScreen createGUI() {
        if (targetDim == 0) {
            dimensionConfig = new DimensionConfig(0);
        } else {
            dimensionConfig = DimensionConfig.getForDimension(targetDim, true);
        }
        final ArrayList<IWidget> blockList = new ArrayList<>();
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
                    Packets.INSTANCE.sendChangeWorldSettings(dimID, blockPos, dimensionConfig).sendToServer();
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
                .top(53).left(7)
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
                .top(35).left(10)
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
                .top(35).left(30)
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
                .top(35).left(50)
                .overlay(checkmark)
                .addTooltipLine("Passive Spawning").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.setPassiveSpawn(!dimensionConfig.passiveSpawn());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.passiveSpawn() ? checkmark : crossmark);
                }));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .top(35).left(70)
                .overlay(checkmark)
                .addTooltipLine("Weather").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.enableWeather(!dimensionConfig.weatherEnabled());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.weatherEnabled() ? checkmark : crossmark);
                }));
        panel.child(new ButtonWidget<>()
                .size(15, 15)
                .top(35).left(90)
                .overlay(checkmark)
                .addTooltipLine("Clouds").tooltipScale(0.6F)
                .onMousePressed(i -> {
                    dimensionConfig.enableClouds(!dimensionConfig.cloudsEnabled());
                    return true;
                })
                .onUpdateListener(widget -> {
                    widget.overlay(dimensionConfig.cloudsEnabled() ? checkmark : crossmark);
                }));
        panel.child(new ListWidget<>(blockList)
                .top(20).right(60)
                .size(15, 115));
        redrawBiomeList();
        redrawPresets();
        this.firstDraw = false;
        return new ModularScreen(panel);
    }

    private void redrawBiomeList() {
        if (!firstDraw) {
            panel.getChildren().removeIf(widget -> widget.equals(biomesWidget));
        }
        biomesWidget.getChildren().clear();
        for (Biome biome : PWConfig.getAllowedBiomes()) {
            if (biome == dimensionConfig.getBiome()) {
                biomesWidget.background(GuiTextures.MC_BUTTON)
                        .overlay(IKey.str(biome.getBiomeName()).color(0xFFFFFF));
            } else {
                biomesWidget.child(new ButtonWidget<>().size(100, 15)
                        .overlay(IKey.str(biome.getBiomeName()))
                        .onMousePressed(mouse -> {
                            dimensionConfig.setBiome(biome);
                            redrawBiomeList();
                            return true;
                        }));
            }
        }
        panel.child(biomesWidget);
        if (!firstDraw) {
            WidgetTree.resize(panel);
            WidgetTree.resize(biomesWidget);
        }
    }

    private void redrawPresets() {
        boolean presetActive = false;
        if (!firstDraw) {
            panel.getChildren().removeIf(widget -> widget.equals(presetsWidget));
        }
        presetsWidget.getChildren().clear();
        for (Map.Entry<String, String> entry : PWConfig.getPresets().entrySet()) {
            if (isPreset(entry.getValue())) {
                presetsWidget.overlay(IKey.str(entry.getKey()));
                presetActive = true;
            } else {
                presetsWidget.child(new ButtonWidget<>()
                        .size(70, 15)
                        .overlay(IKey.str(entry.getKey()))
                        .onMousePressed(mouse -> {
                            this.layers = fromPreset(entry.getValue());
                            redrawLayers();
                            return true;
                        }));
            }
        }
        if (!presetActive) {
            presetsWidget.overlay(IKey.str("Custom"));
        }
        panel.child(presetsWidget);
        if (!firstDraw) {
            WidgetTree.resize(panel);
            WidgetTree.resize(presetsWidget);
        }
    }

    private void redrawLayers() {
        if (layersWidget != null) {
            panel.getChildren().removeIf(widget -> widget.equals(layersWidget));
        }
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

    private boolean isPreset(String preset) {
        return toPreset(layers).equals(preset);
    }

    private String toPreset(List<String> layerList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < layerList.size(); i++) {
            sb.append(layerList.get(i));
            if (i != layerList.size()-1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    private List<String> fromPreset(String preset) {
        ArrayList<String> layers = new ArrayList<>();
        if (preset.contains(",")) {
            String[] stringArray = preset.split(",");
            for (int i = stringArray.length-1; i > -1; --i) {
                layers.add(stringArray[i]);
            }
        } else {
            layers.add(preset);
        }
        return layers;
    }
}
