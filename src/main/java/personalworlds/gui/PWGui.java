package personalworlds.gui;

import codechicken.lib.gui.GuiDraw;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import personalworlds.world.Config;
import personalworlds.world.Enums;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PWGui extends GuiScreen {

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
    EntityPlayer player;
    Config config = new Config(4);


    public PWGui(EntityPlayer player) {
        super();
        this.player = player;
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
                ((config.getSkyColor() >> 16) & 0xFF),
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
                ((config.getSkyColor() >> 8) & 0xFF),
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
                ((config.getSkyColor()) & 0xFF),
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
                config.getDaylightCycle().ordinal(),
                () -> config.setDaylightCycle(enableDaylightCycle.getState()));
        this.rootWidget.addChild(this.enableDaylightCycle);

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.starBrightness"), false));
        this.starBrightness = new WSlider(
                new Rectangle(0, this.ySize, 128, 12),
                "%.2f",
                0.0,
                1.0,
                config.getStarsVisibility(),
                0.01,
                false,
                0xFFFFFF,
                null,
                null);
        addWidget(starBrightness);

        this.ySize += 4;
        /*
        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.biome"), false));
        this.biome = new WTextField(new Rectangle(0, this.ySize, 142, 18), desiredConfig.getBiomeId());
        this.biomeEditButton = new WButton(new Rectangle(144, 0, 18, 18), "", false, 0, Icons.PENCIL, () -> {
            this.biomeCycle = (this.biomeEditButton.lastButton == 0) ? (this.biomeCycle + 1)
                    : (this.biomeCycle + PersonalSpaceMod.clientAllowedBiomes.size() - 1);
            this.biomeCycle = this.biomeCycle % PersonalSpaceMod.clientAllowedBiomes.size();
            this.biome.textField.setText(PersonalSpaceMod.clientAllowedBiomes.get(this.biomeCycle));
        });
        this.biome.addChild(biomeEditButton);
        addWidget(this.biome);
         */
        this.ySize += 4;
        this.generateTrees = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                config.isGenerateTrees(),
                () -> config.setGenerateTrees(generateTrees.getValue()));
        this.generateTrees.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.trees"), false));
        addWidget(generateTrees);
        this.enableWeather = new WToggleButton(
                new Rectangle(90, this.generateTrees.position.y, 18, 18),
                "",
                false,
                0,
                config.isWeather(),
                () -> config.setWeather(enableWeather.getValue()));
        this.enableWeather.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.weather"), false));
        rootWidget.addChild(this.enableWeather);
        this.generateVegetation = new WToggleButton(
                new Rectangle(0, this.ySize, 18, 18),
                "",
                false,
                0,
                config.isPopulate(),
                () -> config.setPopulate(generateVegetation.getValue()));
        this.generateVegetation.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.vegetation"), false));
        addWidget(generateVegetation);
        this.enableClouds = new WToggleButton(
                new Rectangle(90, this.generateVegetation.position.y, 18, 18),
                "",
                false,
                0,
                config.isClouds(),
                () -> config.setClouds(enableClouds.getValue()));
        this.enableClouds.addChild(new WLabel(24, 4, I18n.format("gui.personalWorld.clouds"), false));
        rootWidget.addChild(this.enableClouds);

        /*
        voidPresetName = I18n.format("gui.personalWorld.voidWorld");

        this.ySize += 2;
        this.presetEntry = new WTextField(new Rectangle(0, this.ySize, 160, 20), desiredConfig.getLayersAsString());
        if (this.presetEntry.textField.getText().isEmpty()) {
            this.presetEntry.textField.setText(voidPresetName);
        }
        addWidget(presetEntry);
        this.ySize += 2;

        addWidget(new WLabel(0, this.ySize, I18n.format("gui.personalWorld.presets"), false));

        int px = 8, pi = 1;
        for (String preset : Config.defaultPresets) {
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
                I18n.format("gui.done"),
                true,
                WButton.DEFAULT_COLOR,
                Icons.CHECKMARK,
                () -> {
                    Packets.INSTANCE.sendChangeWorldSettings(this.tile, desiredConfig).sendToServer();
                    Minecraft.getMinecraft().displayGuiScreen(null);
                });
        rootWidget.addChild(
                new WButton(
                        new Rectangle(130, ySize, 128, 20),
                        I18n.format("gui.cancel"),
                        true,
                        WButton.DEFAULT_COLOR,
                        Icons.CROSS,
                        () -> Minecraft.getMinecraft().displayGuiScreen(null)));
        addWidget(save);

        this.presetEditor = new Widget();
        this.presetEditor.position = new Rectangle(172, 0, 1, 1);
        this.rootWidget.addChild(this.presetEditor);
         */

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
        config.setSkyColor((skyR << 16) | (skyG << 8) | skyB);
        config.setStarsVisibility((float) this.starBrightness.getValue());
        super.drawScreen(mouseX, mouseY, partialTicks);

        rootWidget.draw(mouseX, mouseY, partialTicks);

        GuiDraw.gui.setZLevel(0.f);
        GuiDraw.drawRect(130, skyRed.position.y, 32, 3 * (skyRed.position.height + 1), 0xFF000000);
        GuiDraw.drawRect(
                131,
                skyRed.position.y + 1,
                30,
                3 * (skyRed.position.height + 1) - 2,
                0xFF000000 | config.getSkyColor());
        Icons.bindTexture();
        GL11.glColor4f(1, 1, 1, config.getSkyColor());
        Icons.STAR.drawAt(132, this.skyRed.position.y + 2);
        Icons.STAR.drawAt(145, this.skyRed.position.y + 12);
        Icons.STAR.drawAt(134, this.skyRed.position.y + 21);
        GL11.glColor4f(1, 1, 1, 1);

        rootWidget.drawForeground(mouseX, mouseY, partialTicks);

        GL11.glPopMatrix();
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
        super.handleMouseInput();
        if (x > 0 || y > 0 || this.selectedButton != null && btn == 0) {
            rootWidget.mouseMovedOrUp(x, y, btn);
        }
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
