package org.zone404.plugins;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.interaction.server.UsedTeleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.system.TurnOffTeleportersSystem;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.zone404.plugins.pages.TeleporterSettingsPage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;

/**
 * Clears a teleporter's destination warp the first time it is used when its "clear on use" flag is set.
 */
public class ClearOnUseTeleporterSystem extends RefChangeSystem<EntityStore, UsedTeleporter> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.get("HiddenTeleporters|ClearOnUseTeleporterSystem");
    @Nonnull
    private final ComponentType<EntityStore, UsedTeleporter> usedTeleporterComponentType;
    @Nonnull
    private final ComponentType<EntityStore, TransformComponent> transformComponentType;
    @Nonnull
    private final Query<EntityStore> query;

    public ClearOnUseTeleporterSystem(
            @Nonnull ComponentType<EntityStore, UsedTeleporter> usedTeleporterType,
            @Nonnull ComponentType<EntityStore, TransformComponent> transformType) {
        this.usedTeleporterComponentType = usedTeleporterType;
        this.transformComponentType = transformType;
        this.query = Query.and(usedTeleporterType, transformType);
    }

    @Nonnull
    @Override
    public ComponentType<EntityStore, UsedTeleporter> componentType() {
        return this.usedTeleporterComponentType;
    }

    @Override
    public void onComponentAdded(@Nonnull Ref<EntityStore> ref,
                                 @Nonnull UsedTeleporter usedTeleporter,
                                 @Nonnull Store<EntityStore> store,
                                 @Nonnull CommandBuffer<EntityStore> commandBuffer) {
        TransformComponent transform = commandBuffer.getComponent(ref, this.transformComponentType);
        if (transform == null) {
            transform = store.getComponent(ref, this.transformComponentType);
        }
        if (transform == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        org.joml.Vector3d pos = transform.getPosition();
        int px = (int) Math.floor(pos.x);
        int py = (int) Math.floor(pos.y);
        int pz = (int) Math.floor(pos.z);

        // The player's recorded position may not line up exactly with the teleporter block, so check the surrounding blocks.
        boolean cleared = false;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    cleared |= clearIfMarked(world, px + dx, py + dy, pz + dz);
                }
            }
        }
        if (cleared) {
            TurnOffTeleportersSystem.updatePortalBlocksInWorld(world);
        }
    }

    private boolean clearIfMarked(World world, int x, int y, int z) {
        Ref<ChunkStore> blockRef = BlockModule.getBlockEntity(world, x, y, z);
        if (blockRef == null || !blockRef.isValid()) {
            return false;
        }
        Store<ChunkStore> blockStore = blockRef.getStore();
        Teleporter teleporter = blockStore.getComponent(blockRef, Teleporter.getComponentType());
        if (teleporter == null || teleporter.getWarp() == null) {
            return false;
        }
        if (!TeleporterSettingsPage.isClearOnUse(blockRef)) {
            return false;
        }
        teleporter.setWarp(null);
        BlockModule.BlockStateInfo blockStateInfo = blockStore.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
        if (blockStateInfo != null) {
            blockStateInfo.markNeedsSaving();
        }
        TeleporterSettingsPage.setClearOnUse(blockRef, false);
        LOGGER.atInfo().log("Cleared destination on use for teleporter at " + x + "," + y + "," + z);
        return true;
    }

    @Override
    public void onComponentSet(@Nonnull Ref<EntityStore> ref,
                               @Nullable UsedTeleporter old,
                               @Nonnull UsedTeleporter current,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Override
    public void onComponentRemoved(@Nonnull Ref<EntityStore> ref,
                                   @Nonnull UsedTeleporter component,
                                   @Nonnull Store<EntityStore> store,
                                   @Nonnull CommandBuffer<EntityStore> commandBuffer) {
    }

    @Nonnull
    @Override
    public Query<EntityStore> getQuery() {
        return this.query;
    }

    @Nonnull
    @Override
    public Set<Dependency<EntityStore>> getDependencies() {
        return Set.of();
    }
}