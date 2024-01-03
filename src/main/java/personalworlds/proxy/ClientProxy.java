package personalworlds.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import personalworlds.blocks.tile.TilePersonalPortal;
import personalworlds.gui.PWGui;

public class ClientProxy extends CommonProxy {

    public void closeGUI(TilePersonalPortal owner) {
        GuiScreen screen = Minecraft.getMinecraft().currentScreen;
        if (screen instanceof PWGui gui) {
            if (gui.tpp == owner) {
                Minecraft.getMinecraft().displayGuiScreen(null);
            }
        }
    }
}
