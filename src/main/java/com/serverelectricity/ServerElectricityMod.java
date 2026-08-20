package com.serverelectricity;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import org.joml.Vector3f;

import java.util.*;

public class ServerElectricityMod implements ModInitializer {
    public static final String MOD_ID = "server_electricity";

    public static final ItemGroup ELECTRIC_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            new Identifier(MOD_ID, "electricity_tab"),
            ItemGroupEvents.createBuilder()
                    .displayName(Text.literal("⚡ 전기 모드 (Electricity)"))
                    .icon(() -> new ItemStack(ServerElectricityMod.SOLAR_GENERATOR_ITEM))
                    .entries((displayContext, entries) -> {
                        entries.add(SOLAR_GENERATOR_ITEM);
                        entries.add(TRANSFORMER_ITEM);
                        entries.add(SUBSTATION_ITEM);
                        entries.add(BATTERY_BOX_ITEM);
                        entries.add(ELECTRIC_FURNACE_ITEM);
                        entries.add(LV_CABLE_ITEM);
                        entries.add(HV_CABLE_ITEM);
                        entries.add(EHV_CABLE_ITEM);
                    })
                    .build()
    );

    public static final Block SOLAR_GENERATOR = new SolarGeneratorBlock(Block.Settings.copy(Blocks.IRON_BLOCK));
    public static Item SOLAR_GENERATOR_ITEM;
    public static BlockEntityType<SolarGeneratorBlockEntity> SOLAR_GENERATOR_BE;

    public static final Block TRANSFORMER = new TransformerBlock(Block.Settings.copy(Blocks.IRON_BLOCK));
    public static Item TRANSFORMER_ITEM;
    public static BlockEntityType<TransformerBlockEntity> TRANSFORMER_BE;

    public static final Block SUBSTATION = new SubstationBlock(Block.Settings.copy(Blocks.IRON_BLOCK));
    public static Item SUBSTATION_ITEM;
    public static BlockEntityType<SubstationBlockEntity> SUBSTATION_BE;

    public static final Block BATTERY_BOX = new BatteryBoxBlock(Block.Settings.copy(Blocks.IRON_BLOCK));
    public static Item BATTERY_BOX_ITEM;
    public static BlockEntityType<BatteryBoxBlockEntity> BATTERY_BOX_BE;

    public static final Block ELECTRIC_FURNACE = new ElectricFurnaceBlock(Block.Settings.copy(Blocks.IRON_BLOCK));
    public static Item ELECTRIC_FURNACE_ITEM;
    public static BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE_BE;

    public static final Block LV_CABLE_BLOCK = new CableBlock(100, Block.Settings.copy(Blocks.REDSTONE_WIRE));
    public static Item LV_CABLE_ITEM;

    public static final Block HV_CABLE_BLOCK = new CableBlock(1000, Block.Settings.copy(Blocks.REDSTONE_WIRE));
    public static Item HV_CABLE_ITEM;

    public static final Block EHV_CABLE_BLOCK = new CableBlock(5000, Block.Settings.copy(Blocks.REDSTONE_WIRE));
    public static Item EHV_CABLE_ITEM;

    @Override
    public void onInitialize() {
        registerBlocksAndItems();

        SOLAR_GENERATOR_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "solar_be"),
                FabricBlockEntityTypeBuilder.create(SolarGeneratorBlockEntity::new, SOLAR_GENERATOR).build());
        TRANSFORMER_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "transformer_be"),
                FabricBlockEntityTypeBuilder.create(TransformerBlockEntity::new, TRANSFORMER).build());
        SUBSTATION_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "substation_be"),
                FabricBlockEntityTypeBuilder.create(SubstationBlockEntity::new, SUBSTATION).build());
        BATTERY_BOX_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "battery_be"),
                FabricBlockEntityTypeBuilder.create(BatteryBoxBlockEntity::new, BATTERY_BOX).build());
        ELECTRIC_FURNACE_BE = Registry.register(Registries.BLOCK_ENTITY_TYPE, new Identifier(MOD_ID, "furnace_be"),
                FabricBlockEntityTypeBuilder.create(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE).build());

        ServerTickEvents.END_WORLD_TICK.register(this::tickWorldElectricity);
    }

    private void registerBlocksAndItems() {
        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "solar_generator"), SOLAR_GENERATOR);
        SOLAR_GENERATOR_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "solar_generator"), new BlockItem(SOLAR_GENERATOR, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "transformer"), TRANSFORMER);
        TRANSFORMER_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "transformer"), new BlockItem(TRANSFORMER, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "substation"), SUBSTATION);
        SUBSTATION_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "substation"), new BlockItem(SUBSTATION, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "battery_box"), BATTERY_BOX);
        BATTERY_BOX_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "battery_box"), new BlockItem(BATTERY_BOX, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "electric_furnace"), ELECTRIC_FURNACE);
        ELECTRIC_FURNACE_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "electric_furnace"), new BlockItem(ELECTRIC_FURNACE, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "lv_cable"), LV_CABLE_BLOCK);
        LV_CABLE_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "lv_cable"), new BlockItem(LV_CABLE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "hv_cable"), HV_CABLE_BLOCK);
        HV_CABLE_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "hv_cable"), new BlockItem(HV_CABLE_BLOCK, new Item.Settings()));

        Registry.register(Registries.BLOCK, new Identifier(MOD_ID, "ehv_cable"), EHV_CABLE_BLOCK);
        EHV_CABLE_ITEM = Registry.register(Registries.ITEM, new Identifier(MOD_ID, "ehv_cable"), new BlockItem(EHV_CABLE_BLOCK, new Item.Settings()));
    }

    private void tickWorldElectricity(ServerWorld world) {
        if (world.getTime() % 20 != 0) return;

        List<BlockPos> positions = new ArrayList<>();
        for (BlockEntity be : world.blockEntities) {
            if (be instanceof MachineBlockEntity) {
                positions.add(be.getPos());
            }
        }

        for (BlockPos pos : positions) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof MachineBlockEntity machine) {
                machine.checkBreakdown(world);

                if (machine.isBroken()) {
                    world.spawnParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 3, 0.1, 0.2, 0.1, 0.02);
                    continue;
                }

                machine.tickEnergy(world);
            }
        }
    }

    public static class MachineBlockEntity extends BlockEntity {
        protected int energy = 0;
        protected int maxEnergy = 10000;
        protected boolean broken = false;

        public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
            super(type, pos, state);
        }

        public void checkBreakdown(ServerWorld world) {
            if (broken) return;
            Difficulty difficulty = world.getDifficulty();
            double chance = 0.0;
            if (difficulty == Difficulty.NORMAL) chance = 0.0005;
            else if (difficulty == Difficulty.HARD) chance = 0.002;

            if (chance > 0 && world.random.nextDouble() < chance) {
                broken = true;
                markDirty();
            }
        }

        public boolean tryRepair(PlayerEntity player, Hand hand) {
            ItemStack stack = player.getStackInHand(hand);
            if (broken && stack.isOf(Items.COPPER_INGOT)) {
                if (!player.getAbilities().creativeMode) stack.decrement(1);
                broken = false;
                markDirty();
                if (world != null) {
                    world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                }
                return true;
            }
            return false;
        }

        public boolean isBroken() { return broken; }
        public void tickEnergy(ServerWorld world) {}
        public int getEnergy() { return energy; }
        public int getMaxEnergy() { return maxEnergy; }
    }

    public static class SolarGeneratorBlockEntity extends MachineBlockEntity {
        public SolarGeneratorBlockEntity(BlockPos pos, BlockState state) { super(SOLAR_GENERATOR_BE, pos, state); }
        @Override
        public void tickEnergy(ServerWorld world) {
            if (world.isDay() && !world.isRaining() && world.isSkyVisible(pos.up())) {
                energy = Math.min(maxEnergy, energy + 100);
                markDirty();
            }
        }
    }

    public static class TransformerBlockEntity extends MachineBlockEntity {
        public TransformerBlockEntity(BlockPos pos, BlockState state) {
            super(TRANSFORMER_BE, pos, state);
            this.maxEnergy = 50000;
        }
    }

    public static class SubstationBlockEntity extends MachineBlockEntity {
        public SubstationBlockEntity(BlockPos pos, BlockState state) {
            super(SUBSTATION_BE, pos, state);
            this.maxEnergy = 200000;
        }
    }

    public static class BatteryBoxBlockEntity extends MachineBlockEntity {
        public BatteryBoxBlockEntity(BlockPos pos, BlockState state) {
            super(BATTERY_BOX_BE, pos, state);
            this.maxEnergy = 500000;
        }
    }

    public static class ElectricFurnaceBlockEntity extends MachineBlockEntity {
        public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
            super(ELECTRIC_FURNACE_BE, pos, state);
            this.maxEnergy = 20000;
        }
    }

    public static class CableBlock extends Block {
        private final int capacity;
        public CableBlock(int capacity, Settings settings) {
            super(settings);
            this.capacity = capacity;
        }
        public int getCapacity() { return capacity; }
    }

    public static class BaseMachineBlock extends Block implements BlockEntityProvider {
        public BaseMachineBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return null; }

        @Override
        public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
            if (!world.isClient) {
                BlockEntity be = world.getBlockEntity(pos);
                if (be instanceof MachineBlockEntity machine) {
                    if (machine.tryRepair(player, hand)) {
                        sendLangMessage(player, "machine_repaired", "🔧 기계가 수리되었습니다!", "🔧 Machine Repaired!", "🔧 机器已修复！", "🔧 機械が修理されました！");
                        return ActionResult.SUCCESS;
                    }
                    if (machine.isBroken()) {
                        sendLangMessage(player, "machine_broken", "⚠️ 기계가 고장났습니다! 구리 주괴로 수리하세요.", "⚠️ Machine broken! Repair with Copper Ingot.", "⚠️ 机器故障！请使用铜锭修复。", "⚠️ 故障中！銅インゴットで修理してください。");
                        return ActionResult.SUCCESS;
                    }
                    
                    String status = String.format("⚡ 전력 상태: %d / %d FE", machine.getEnergy(), machine.getMaxEnergy());
                    player.sendMessage(Text.literal(status), true);
                }
            }
            return ActionResult.SUCCESS;
        }

        private void sendLangMessage(PlayerEntity player, String key, String ko, String en, String zh, String ja) {
            String lang = "ko_kr";
            if (player instanceof ServerPlayerEntity serverPlayer) {
                lang = serverPlayer.getClientOptions().language().toLowerCase();
            }
            String msg = switch (lang) {
                case "en_us" -> en;
                case "zh_cn" -> zh;
                case "ja_jp" -> ja;
                default -> ko;
            };
            player.sendMessage(Text.literal(msg), true);
        }
    }

    public static class SolarGeneratorBlock extends BaseMachineBlock {
        public SolarGeneratorBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new SolarGeneratorBlockEntity(pos, state); }
    }

    public static class TransformerBlock extends BaseMachineBlock {
        public TransformerBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new TransformerBlockEntity(pos, state); }
    }

    public static class SubstationBlock extends BaseMachineBlock {
        public SubstationBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new SubstationBlockEntity(pos, state); }
    }

    public static class BatteryBoxBlock extends BaseMachineBlock {
        public BatteryBoxBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new BatteryBoxBlockEntity(pos, state); }
    }

    public static class ElectricFurnaceBlock extends BaseMachineBlock {
        public ElectricFurnaceBlock(Settings settings) { super(settings); }
        @Override
        public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new ElectricFurnaceBlockEntity(pos, state); }
    }
}
