package personalworlds.proxy;


import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import personalworlds.PWValues;
import personalworlds.PersonalWorlds;
import personalworlds.block.entity.PortalEntity;

public class CommonProxy {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, PWValues.ModID);
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, PWValues.ModID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, PWValues.ModID);

    public static final RegistryObject<Block> portal_block = BLOCKS.register("personal_portal",
            () -> new Block(BlockBehaviour.Properties.of()));

    public static final RegistryObject<BlockEntityType<PortalEntity>> portal_entity = BLOCK_ENTITIES.register("personal_portal",
            () -> BlockEntityType.Builder.of(PortalEntity::new, portal_block.get()).build(null));

    public static final RegistryObject<CreativeModeTab> creative_tab = TABS.register("pw_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("E"))
                    .icon(portal_block.get().asItem()::getDefaultInstance)
                    .displayItems((displayParams, output) -> output.accept(portal_block.get().asItem().getDefaultInstance()))
                    .build());

    public static void init(IEventBus bus) {
        PersonalWorlds.log.error("E");
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
    }

}
