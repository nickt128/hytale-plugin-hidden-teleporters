package org.zone404.plugins;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.logging.Level;

public class RandomWarpNameWhenTeleporterPlacedSystem extends RefChangeSystem<ChunkStore, PlacedByInteractionComponent> {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.get("HiddenTeleporters|RandomWarpNameSystem");

    @Nonnull
    public ComponentType<ChunkStore, PlacedByInteractionComponent> componentType() {
        return PlacedByInteractionComponent.getComponentType();
    }

    public void onComponentAdded(@Nonnull Ref<ChunkStore> ref, @Nonnull PlacedByInteractionComponent placedByInteractionComponent, @Nonnull Store<ChunkStore> chunkStore, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {
        Teleporter teleporterComponent = commandBuffer.getComponent(ref, Teleporter.getComponentType());
        if (teleporterComponent == null) {
            LOGGER.at(Level.SEVERE).log("Failed to get teleporter component.");
            return;
        }
        var warpId = teleporterComponent.getOwnedWarp().toLowerCase();
        LOGGER.at(Level.INFO).log("Got warp id: " + warpId);
        TeleportPlugin teleportPlugin = TeleportPlugin.get();
        var warp = teleportPlugin.getWarps().get(warpId.toLowerCase());
        LOGGER.at(Level.INFO).log("Got warp: " + warp);
        var newWarpId = generatePortalName(10);
        LOGGER.at(Level.INFO).log("Renaming to: " + newWarpId);


        BlockModule.BlockStateInfo blockStateInfoComponent = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

        assert blockStateInfoComponent != null;

        Ref<ChunkStore> chunkRef = blockStateInfoComponent.getChunkRef();
        if (chunkRef != null && chunkRef.isValid()) {
            WorldChunk worldChunk = chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
            if (worldChunk != null) {
                var transform = warp.getTransform();
                assert transform != null;
                var newWarp = new Warp(transform, newWarpId, worldChunk.getWorld(), "*Teleporter", warp.getCreationDate());
                teleportPlugin.getWarps().remove(warpId);
                teleportPlugin.getWarps().put(newWarpId, newWarp);
                teleportPlugin.saveWarps();
                teleporterComponent.setOwnedWarp(newWarpId);
                LOGGER.at(Level.INFO).log("Successfully renamed warp to: " + newWarpId);
            }
        }
    }

    public void onComponentSet(@Nonnull Ref<ChunkStore> ref, @NullableDecl PlacedByInteractionComponent placedByInteractionComponent, @Nonnull PlacedByInteractionComponent t1, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

    }

    public void onComponentRemoved(@Nonnull Ref<ChunkStore> ref, @Nonnull PlacedByInteractionComponent placedByInteractionComponent, @Nonnull Store<ChunkStore> store, @Nonnull CommandBuffer<ChunkStore> commandBuffer) {

    }

    @Nullable
    public Query<ChunkStore> getQuery() {
        return Query.and(PlacedByInteractionComponent.getComponentType(), Teleporter.getComponentType(), BlockModule.BlockStateInfo.getComponentType());
    }

    public static String generatePortalName(int length) {
        String chars = "abcdefghijklmnopqrstuvwxyz0123456789";
        var random = new Random();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
