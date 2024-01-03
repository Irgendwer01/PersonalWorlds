package personalworlds.gui;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import net.minecraft.world.gen.FlatLayerInfo;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import codechicken.lib.gui.GuiDraw;
import personalworlds.PWConfig;
import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.packet.Packets;
import personalworlds.world.DimensionConfig;
import personalworlds.world.Enums;

public class PWGui extends GuiScreen {

    public TilePersonalPortal tpp;
    int xSize, ySize, guiLeft, guiTop;

    WSlider skyRed, skyGreen, skyBlue;
    WSlider starBrightness;
    WTextField biome;
    int biomeCycle = 0;
    WButton biomeEditButton;
    WToggleButton enableWeather;
    WCycleButton enableDaylightCycle;
    WToggleButton enableClouds;
    WButton skyType;
    WToggleButton generateTrees;
    WToggleButton generateVegetation;
    WButton save;
    WTextField presetEntry;
    List<WButton> presetButtons = new ArrayList<>();
    Widget presetEditor;
    Widget rootWidget = new Widget();
    String voidPresetName = "gui.personalWorld.voidWorld";

    DimensionConfig dimensionConfig = new DimensionConfig(4);

    public PWGui(TilePersonalPortal tile) {
        super();
        this.tpp = tile;
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        rootWidget.update();
        if (!this.mc.player.isEntityAlive() || this.mc.player.isDead) {
            this.mc.player.closeScreen();
        }
    }

    private void addBtn(GuiButton btn) {
        this.buttonList.add(btn);
        this.ySize += btn.height + 6;
    }

    private void addWidget(Widget w) {
        this.rootWidget.addChild(w);
        this.ySize += w.position.height + 1;
    }

    @Override
    public void initGui() {
        this.ySize = 0;
        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.skyColor"), false));
        this.skyRed = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.red") + "%.0f",
                0.0,
                255.0,
                ((dimensionConfig.getSkyColor() >> 16) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyRed);
        this.skyGreen = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.green") + "%.0f",
                0.0,
                255.0,
                ((dimensionConfig.getSkyColor() >> 8) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyGreen);
        this.skyBlue = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                I18n.format("gui.personalWorld.skyColor.blue") + "%.0f",
                0.0,
                255.0,
                ((dimensionConfig.getSkyColor()) & 0xFF),
                1.0,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(skyBlue);

        this.ySize += 4;

        this.enableDaylightCycle = new WCycleButton(
                new Rectangle(130, this.ySize, 18, 18),
                "",
                false,
                0,
                Arrays.asList(
                        new WCycleButton.ButtonState(Enums.DaylightCycle.SUN, Icons.SUN),
                        new WCycleButton.ButtonState(Enums.DaylightCycle.MOON, Icons.MOON),
                        new WCycleButton.ButtonState(Enums.DaylightCycle.CYCLE, Icons.SUN_MOON)),
                dimensionConfig.getDaylightCycle().ordinal(),
                () -> dimensionConfig.setDaylightCycle(enableDaylightCycle.getState()));
        this.rootWidget.addChild(this.enableDaylightCycle);

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.starBrightness"), false));
        this.starBrightness = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                "%.2f",
                0.0,
                1.0,
                dimensionConfig.getStarsVisibility(),
                0.01,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(starBrightness);

        this.ySize += 4;
        /*
         * addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.biome"), false));
         * this.biome = new WTextField(new Rectangle(0, this.ySize, 142, 18), desiredConfig.getBiomeId());
         * this.biomeEditButton = new WButton(new Rectangle(144, 0, 18, 18), "", false, 0, Icons.PENCIL, () -> {
         * this.biomeCycle = (this.biomeEditButton.lastButton == 0) ? (this.biomeCycle + 1)
         * : (this.biomeCycle + PersonalSpaceMod.clientAllowedBiomes.size() - 1);
         * this.biomeCycle = this.biomeCycle % PersonalSpaceMod.clientAllowedBiomes.size();
         * this.biome.textField.setText(PersonalSpaceMod.clientAllowedBiomes.get(this.biomeCycle));
         * });
         * this.biome.addChild(biomeEditButton);
         * addWidget(this.biome);
         */
        this.ySize += 4;
        this.generateTrees = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                dimensionConfig.isGenerateTrees(),
                () -> dimensionConfig.setGenerateTrees(generateTrees.getValue()));
        this.generateTrees.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.trees"), false));
        addWidget(generateTrees);
        this.enableWeather = new WToggleButton(
                new Rectangle(90, this.generateTrees.position.y, 18, 18),
                "",
                false,
                0,
                dimensionConfig.isWeather(),
                () -> dimensionConfig.setWeather(enableWeather.getValue()));
        this.enableWeather.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.weather"), false));
        rootWidget.addChild(this.enableWeather);
        this.generateVegetation = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                dimensionConfig.isVegetation(),
                () -> dimensionConfig.setVegetation(generateVegetation.getValue()));
        this.generateVegetation.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.vegetation"), false));
        addWidget(generateVegetation);
        this.enableClouds = new WToggleButton(
                new Rectangle(90, this.generateVegetation.position.y, 18, 18),
                "",
                false,
                0,
                dimensionConfig.isClouds(),
                () -> dimensionConfig.setClouds(enableClouds.getValue()));
        this.enableClouds.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.clouds"), false));
        rootWidget.addChild(this.enableClouds);

        voidPresetName = I18n.format("gui.personalWorld.voidWorld");

        this.ySize += 2;
        this.presetEntry = new WTextField(new Rectangle(0, this.ySize, 160, 20), dimensionConfig.getLayersAsString());
        if (this.presetEntry.textField.getText().isEmpty()) {
            this.presetEntry.textField.setText(voidPresetName);
        }
        addWidget(presetEntry);
        this.ySize += 2;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.presets"), false));

        int px = 8, pi = 1;
        for (String preset : PWConfig.defaultPresets) {
            if (preset.isEmpty()) {
                preset = voidPresetName;
            }
            String finalPreset = preset;
            presetButtons.add(
                    new WButton(
                            new Rectangle(px, this.ySize, 24, 18),
                            Integer.toString(pi),
                            true,
                            WButton.DEFAULT_COLOR,
                            null,
                            () -> this.presetEntry.textField.setText(finalPreset)));
            rootWidget.addChild(presetButtons.get(presetButtons.size() - 1));
            ++pi;
            px += 26;
        }

        this.ySize += 20;
        this.save = new WButton(
              new Rectangle(0, ySize, 128, 20),
            I18n.format("gui.done"), true, WButton.DEFAULT_COLOR, Icons.CHECKMARK, () -> {
                  Packets.INSTANCE.sendChangeWorldSettings(this.tpp, dimensionConfig).sendToServer();
                  Minecraft.getMinecraft().displayGuiScreen(null);
              });
        rootWidget.addChild(new WButton(new Rectangle(130, ySize, 128, 20),
                I18n.format("gui.cancel"),
                true,
                WButton.DEFAULT_COLOR,
                Icons.CROSS, () -> Minecraft.getMinecraft().displayGuiScreen(null)));
         addWidget(save);

        this.presetEditor = new Widget();
        this.presetEditor.position = new Rectangle(172, 0, 1, 1);
        this.rootWidget.addChild(this.presetEditor);



        this.xSize = 320 - 16;
        this.ySize = 240 - 16;
        this.guiLeft = (this.width - this.xSize) / 2;
        this.guiTop = (this.height - this.ySize) / 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        GL11.glPushMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glTranslatef(this.guiLeft, this.guiTop, 0.0f);
        mouseX -= guiLeft;
        mouseY -= guiTop;
        Icons.bindTexture();
        Icons.GUI_BG.draw9Patch(-8, -8, xSize + 16, ySize + 16);

        int skyR = MathHelper.clamp(skyRed.getValueInt(), 0, 255);
        int skyG = MathHelper.clamp(skyGreen.getValueInt(), 0, 255);
        int skyB = MathHelper.clamp(skyBlue.getValueInt(), 0, 255);
        dimensionConfig.setSkyColor((skyR << 16) | (skyG << 8) | skyB);
        dimensionConfig.setStarsVisibility((float) this.starBrightness.getValue());


        super.drawScreen(mouseX, mouseY, partialTicks);

        String actualText = this.presetEntry.textField.getText();
        if (voidPresetName.equals(actualText)) {
            actualText = "";
        }

        boolean inputsValid = true;

        if(!DimensionConfig.PRESET_VALIDATION_PATTERN.matcher(actualText).matches() && !actualText.equals("")) {
            this.presetEntry.textField.setTextColor(0xFF0000);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.invalidSyntax");
            inputsValid = false;
        } else if (!DimensionConfig.canUseLayers(actualText, true)) {
            this.presetEntry.textField.setTextColor(0xFFFF00);
            this.presetEntry.tooltip = I18n.format("gui.personalWorld.notAllowed");
            inputsValid = false;
        } else {
            this.presetEntry.textField.setTextColor(0xA0FFA0);
            this.presetEntry.tooltip = null;
            this.dimensionConfig.setLayers(actualText);
            this.regeneratePresetEditor();
        }

        this.save.enabled = inputsValid;

        rootWidget.draw(mouseX, mouseY, partialTicks);

        GuiDraw.gui.setZLevel(0.f);
        GuiDraw.drawRect(130, skyRed.position.y, 32, 3 * (skyRed.position.height + 1), 0xFF000000);
        GuiDraw.drawRect(
                131,
                skyRed.position.y + 1,
                30,
                3 * (skyRed.position.height + 1) - 2,
                0xFF000000 | dimensionConfig.getSkyColor());
        Icons.bindTexture();
        GL11.glColor4f(1, 1, 1, dimensionConfig.getStarsVisibility());
        Icons.STAR.drawAt(132, this.skyRed.position.y + 2);
        Icons.STAR.drawAt(145, this.skyRed.position.y + 12);
        Icons.STAR.drawAt(134, this.skyRed.position.y + 21);
        GL11.glColor4f(1, 1, 1, 1);

        rootWidget.drawForeground(mouseX, mouseY, partialTicks);

        GL11.glPopMatrix();
    }

    private void regeneratePresetEditor() {
//        final boolean generationEnabled = desiredConfig.getAllowGenerationChanges();
//        this.presetEditor.children.clear();
//        // Palette
//        int curX = 0;
//        int curY = 0;
//        for (String bl : PersonalSpaceMod.clientAllowedBlocks) {
//            String[] blName = bl.split(":");
//            if (blName.length != 2) continue;
//            Block block = GameRegistry.findBlock(blName[0], blName[1]);
//            ItemStack is = new ItemStack(block);
//            WButton addBtn = new WButton(new Rectangle(curX, curY, 20, 20), "", false, 0, null, () -> {
//                FlatLayerInfo fli = new FlatLayerInfo(1, block);
//                this.dimensionConfig.getMutableLayers().add(fli);
//                this.dimensionConfig.setLayers(this.dimensionConfig.getLayersAsString());
//                this.configToPreset();
//            });
//            addBtn.itemStack = is;
//            addBtn.itemStackText = "+";
//            addBtn.tooltip = (is.getItem() != null) ? is.getDisplayName() : block.getLocalizedName();
//            addBtn.enabled = generationEnabled;
//            this.presetEditor.addChild(addBtn);
//            curY += 21;
//            if (curY > 188) {
//                curY = 0;
//                curX += 21;
//            }
//        }
//        // Layers
//        curY = 0;
//        curX += 22;
//        this.presetEditor.addChild(new WLabel(curX, curY, I18n.format("gui.personalWorld.layers"), false));
//        curY += 10;
//        List<FlatLayerInfo> fli = this.dimensionConfig.getLayers();
//        for (int i = fli.size() - 1; i >= 0; i--) {
//            FlatLayerInfo info = fli.get(i);
//            final int finalI = i;
//            WButton block = new WButton(new Rectangle(curX + 12, curY, 20, 28), "", false, 0, null, null);
//            Block gameBlock = info.func_151536_b();
//            block.enabled = false;
//            block.itemStack = new ItemStack(gameBlock);
//            block.itemStackText = Integer.toString(info.getLayerCount());
//            block.tooltip = gameBlock.getLocalizedName();
//            this.presetEditor.addChild(block);
//
//            // up
//            if (i < fli.size() - 1) {
//                block.addChild(new WButton(new Rectangle(-12, 0, 10, 10), "", false, 0, Icons.SMALL_UP, () -> {
//                    Collections.swap(this.dimensionConfig.getMutableLayers(), finalI, finalI + 1);
//                    this.configToPreset();
//                }));
//            }
//            block.addChild(new WButton(new Rectangle(-12, 9, 10, 10), "", false, 0, Icons.SMALL_CROSS, () -> {
//                this.dimensionConfig.getMutableLayers().remove(finalI);
//                this.configToPreset();
//            }));
//            if (i > 0) {
//                block.addChild(new WButton(new Rectangle(-12, 18, 10, 10), "", false, 0, Icons.SMALL_DOWN, () -> {
//                    Collections.swap(this.dimensionConfig.getMutableLayers(), finalI, finalI - 1);
//                    this.configToPreset();
//                }));
//            }
//            IntConsumer plusMinus = (mul) -> {
//                FlatLayerInfo orig = this.dimensionConfig.getMutableLayers().get(finalI);
//                boolean shiftHeld = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT);
//                boolean ctrlHeld = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL);
//                int newCnt = ctrlHeld ? 64 : (shiftHeld ? 10 : 1);
//                newCnt *= mul;
//                newCnt = MathHelper.clamp(orig.getLayerCount() + newCnt, 1, 255);
//                this.dimensionConfig.getMutableLayers().set(finalI, new FlatLayerInfo(newCnt, orig.func_151536_b()));
//                this.dimensionConfig.setLayers(this.dimensionConfig.getLayersAsString());
//                this.configToPreset();
//            };
//            block.addChild(
//                    new WButton(
//                            new Rectangle(21, 5, 18, 18),
//                            "",
//                            false,
//                            0,
//                            generationEnabled ? Icons.PLUS : Icons.LOCK,
//                            () -> plusMinus.accept(1)));
//            block.addChild(
//                    new WButton(
//                            new Rectangle(40, 5, 18, 18),
//                            "",
//                            false,
//                            0,
//                            generationEnabled ? Icons.MINUS : Icons.LOCK,
//                            () -> plusMinus.accept(-1)));
//
//            for (Widget child : block.children) {
//                child.enabled = generationEnabled;
//            }
//
//            curY += 30;
//            if (curY > 188) {
//                curY = 10;
//                curX += 21;
//            }
//        }
    }

    @Override
    protected void keyTyped(char character, int key) throws IOException {
        super.keyTyped(character, key);
        rootWidget.keyTyped(character, key);
    }

    @Override
    protected void mouseClicked(int x, int y, int button) throws IOException {
        x -= guiLeft;
        y -= guiTop;
        super.mouseClicked(x, y, button);
        rootWidget.mouseClicked(x, y, button);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int x = (Mouse.getEventX() * this.width / this.mc.displayWidth) - guiLeft;
        int y = (this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1) - guiTop;
        int btn = Mouse.getEventButton();
        if (btn != -1) {
            rootWidget.mouseMovedOrUp(x, y, btn);
        }
        super.handleMouseInput();
    }

    @Override
    protected void mouseClickMove(int x, int y, int lastBtn, long timeDragged) {
        x -= guiLeft;
        y -= guiTop;
        super.mouseClickMove(x, y, lastBtn, timeDragged);
        rootWidget.mouseClickMove(x, y, lastBtn, timeDragged);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        super.actionPerformed(button);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
