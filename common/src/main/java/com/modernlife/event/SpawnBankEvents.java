package com.modernlife.event;

import com.mojang.logging.LogUtils;
import com.modernlife.ModernLifeMod;
import com.modernlife.entity.BankerEntity;
import com.modernlife.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import java.util.Optional;

public class SpawnBankEvents {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static class BankSavedData extends SavedData {
        public boolean bankSpawned = false;
        public BlockPos bankPos = null;

        public static BankSavedData load(CompoundTag tag) {
            BankSavedData data = new BankSavedData();
            data.bankSpawned = tag.getBoolean("bank_spawned");
            if (tag.contains("bank_pos")) {
                data.bankPos = NbtUtils.readBlockPos(tag.getCompound("bank_pos"));
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            tag.putBoolean("bank_spawned", bankSpawned);
            if (bankPos != null) {
                tag.put("bank_pos", NbtUtils.writeBlockPos(bankPos));
            }
            return tag;
        }
    }

    public static boolean spawnBankNearPlayer(ServerLevel serverLevel, ServerPlayer player) {
        if (serverLevel.dimension() != Level.OVERWORLD) {
            LOGGER.warn("[ModernLife] Bank can only be spawned in the Overworld dimension.");
            return false;
        }

        BankSavedData bankData = serverLevel.getDataStorage().computeIfAbsent(
            BankSavedData::load,
            BankSavedData::new,
            "modernlife_bank_data"
        );

        if (bankData.bankSpawned) {
            LOGGER.info("[ModernLife] Bank has already been spawned in this world.");
            return false;
        }

        ResourceLocation structureLoc = ResourceLocation.tryBuild(ModernLifeMod.MODID, "bank");
        if (structureLoc == null) {
            LOGGER.error("[ModernLife] Could not create ResourceLocation! Mod ID: {}", ModernLifeMod.MODID);
            return false;
        }

        Optional<StructureTemplate> templateOpt = serverLevel.getStructureManager().get(structureLoc);
        if (templateOpt.isEmpty()) {
            LOGGER.error("[ModernLife] Bank template not found: data/{}/structures/bank.nbt", ModernLifeMod.MODID);
            player.sendSystemMessage(Component.translatable("chat.modernlife.bank.error_not_found"));
            return false;
        }

        StructureTemplate template = templateOpt.get();
        Vec3i size = template.getSize();
        BlockPos playerPos = player.blockPosition();

        BlockPos bestPos = findSuitableFlatLand(serverLevel, playerPos, size.getX(), size.getZ());

        if (bestPos == null) {
            int fallbackX = playerPos.getX() + 10;
            int fallbackZ = playerPos.getZ() + 10;
            int fallbackY = getSafeSurfaceY(serverLevel, fallbackX, fallbackZ, playerPos.getY());
            bestPos = new BlockPos(fallbackX, fallbackY, fallbackZ);
        }

        ensureAreaLoaded(serverLevel, bestPos, size);
        prepareTerrain(serverLevel, bestPos, size);

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        template.placeInWorld(serverLevel, bestPos, bestPos, settings, serverLevel.getRandom(), Block.UPDATE_ALL);

        BankerEntity banker = ModEntities.BANKER.get().create(serverLevel);
        if (banker != null) {
            double npcX = bestPos.getX() + 21.7;
            double npcY = bestPos.getY() + 1.75;
            double npcZ = bestPos.getZ() + 14.0;

            BlockPos npcPos = new BlockPos((int) npcX, (int) npcY, (int) npcZ);
            serverLevel.setBlock(npcPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            serverLevel.setBlock(npcPos.above(), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

            banker.moveTo(npcX, npcY, npcZ, 90.0F, 0.0F);
            banker.setYHeadRot(90.0F);
            banker.setYBodyRot(90.0F);
            banker.setNoAi(true);
            banker.setPersistenceRequired();

            serverLevel.addFreshEntity(banker);
        }

        bankData.bankSpawned = true;
        bankData.bankPos = bestPos;
        bankData.setDirty();

        LOGGER.info("[ModernLife] Bank successfully built: {}", bestPos);

        // Komut yerine modun gizli eylemini tetikler
        MutableComponent tpBtn = Component.translatable("chat.modernlife.bank.teleport_button")
                .withStyle(Style.EMPTY.withClickEvent(new ClickEvent(
                        ClickEvent.Action.RUN_COMMAND,
                        "/ml_tp_secret"
                )));

        player.sendSystemMessage(Component.translatable(
                "chat.modernlife.bank.spawn_success",
                bestPos.getX(),
                bestPos.getY(),
                bestPos.getZ()
        ).append(tpBtn));

        return true;
    }

    private static BlockPos findSuitableFlatLand(ServerLevel level, BlockPos origin, int sizeX, int sizeZ) {
        int[] steps = {8, -8, 16, -16, 24, -24, 32, -32};

        for (int dx : steps) {
            for (int dz : steps) {
                int testX = origin.getX() + dx;
                int testZ = origin.getZ() + dz;

                int y00 = getSafeSurfaceY(level, testX, testZ, origin.getY());
                int y10 = getSafeSurfaceY(level, testX + sizeX, testZ, origin.getY());
                int y01 = getSafeSurfaceY(level, testX, testZ + sizeZ, origin.getY());
                int y11 = getSafeSurfaceY(level, testX + sizeX, testZ + sizeZ, origin.getY());

                int minY = Math.min(Math.min(y00, y10), Math.min(y01, y11));
                int maxY = Math.max(Math.max(y00, y10), Math.max(y01, y11));

                if (maxY - minY <= 4) {
                    return new BlockPos(testX, minY, testZ);
                }
            }
        }
        return null;
    }

    private static int getSafeSurfaceY(ServerLevel level, int x, int z, int defaultY) {
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        if (y <= level.getMinBuildHeight() + 5 || y >= level.getMaxBuildHeight()) {
            return defaultY;
        }
        return y;
    }

    private static void ensureAreaLoaded(ServerLevel level, BlockPos startPos, Vec3i size) {
        int minChunkX = startPos.getX() >> 4;
        int maxChunkX = (startPos.getX() + size.getX()) >> 4;
        int minChunkZ = startPos.getZ() >> 4;
        int maxChunkZ = (startPos.getZ() + size.getZ()) >> 4;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                level.getChunk(cx, cz);
            }
        }
    }

    private static void prepareTerrain(ServerLevel level, BlockPos startPos, Vec3i size) {
        for (int x = -1; x < size.getX() + 1; x++) {
            for (int z = -1; z < size.getZ() + 1; z++) {
                for (int y = 0; y < size.getY() + 3; y++) {
                    BlockPos p = startPos.offset(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (!state.isAir()) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }

                for (int y = 1; y <= 3; y++) {
                    BlockPos p = startPos.offset(x, -y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.isAir() || !state.isSolidRender(level, p)) {
                        level.setBlock(p, Blocks.DIRT.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }
}