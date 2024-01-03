package personalworlds.gui;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.ItemDrawable;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.ListWidget;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.world.biome.Biome;
import personalworlds.PWConfig;
import personalworlds.blocks.tile.TilePersonalPortal;

import java.util.ArrayList;

public class PWGuiMUI {

    private TilePersonalPortal TPP;

    public PWGuiMUI(TilePersonalPortal TPP) {
    this.TPP = TPP;
    }
    private ArrayList<IWidget> list = new ArrayList<>();
    private ArrayList<ItemStack> itemStacks = new ArrayList<>();
    private ArrayList<IWidget> blockList = new ArrayList<>();


    public ModularScreen createGUI() {
        for (IBlockState blockState : PWConfig.getAllowedBlocks()) {
            Block block = blockState.getBlock();
            int meta = blockState.getBlock().damageDropped(blockState);
            ItemStack stack = new ItemStack(block, 1, meta);
            blockList.add(new ButtonWidget<>().size(20, 20).overlay(new ItemDrawable(stack)).addTooltipLine(stack.getDisplayName()));
        }
        for (Biome biome : PWConfig.getAllowedBiomes()) {
            list.add(new ButtonWidget<>().size(90, 20).overlay(IKey.str(biome.getBiomeName())));
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
                .size(20, 20)
                .bottom(90).left(9)
                .overlay(UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark").build()));
        panel.child(new ButtonWidget<>()
                .size(20, 20)
                .bottom(65).left(9)
                .overlay(UITexture.builder().imageSize(16, 16).location("personalworlds", "checkmark").build()));
        panel.child(new ListWidget<>(list)
                        .bottom(40).left(9)
                        .size(90, 20));
        panel.child(IKey.str("Trees").asWidget()
                .bottom(105).left(35));
        panel.child(new ListWidget<>(blockList)
                .top(9).right(60)
                .size(20, 150));
        return new ModularScreen(panel);

    }
}
