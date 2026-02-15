package org.zone404.plugins;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.system.CreateWarpWhenTeleporterPlacedSystem;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefChangeSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.Instant;
import java.util.Random;
import java.util.Set;
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
        var newWarpId = generatePortalName(10);


        BlockModule.BlockStateInfo blockStateInfoComponent = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());

        assert blockStateInfoComponent != null;

        Ref<ChunkStore> chunkRef = blockStateInfoComponent.getChunkRef();
        if (chunkRef != null && chunkRef.isValid()) {
            WorldChunk worldChunk = chunkStore.getComponent(chunkRef, WorldChunk.getComponentType());
            if (worldChunk != null) {
                createWarp(worldChunk, blockStateInfoComponent, newWarpId);
                teleporterComponent.setOwnedWarp(newWarpId);
                // prevent `CreateWarpWhenTeleporterPlacedSystem` from overwriting the random warp id
                teleporterComponent.setWarpNameWordListKey("something_that_doesnt_exist123");
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

    @Nonnull
    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, CreateWarpWhenTeleporterPlacedSystem.class));
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

    public static void createWarp(@Nonnull WorldChunk worldChunk, @Nonnull BlockModule.BlockStateInfo blockStateInfo, @Nonnull String name) {
        int chunkBlockX = worldChunk.getX() << 5;
        int chunkBlockZ = worldChunk.getZ() << 5;
        int index = blockStateInfo.getIndex();
        int x = chunkBlockX + ChunkUtil.xFromBlockInColumn(index);
        int y = ChunkUtil.yFromBlockInColumn(index);
        int z = chunkBlockZ + ChunkUtil.zFromBlockInColumn(index);
        BlockChunk blockChunkComponent = worldChunk.getBlockChunk();

        assert blockChunkComponent != null;

        BlockSection section = blockChunkComponent.getSectionAtBlockY(y);
        int rotationIndex = section.getRotationIndex(x, y, z);
        RotationTuple rotationTuple = RotationTuple.get(rotationIndex);
        Rotation rotationYaw = rotationTuple.yaw();
        float warpRotationYaw = (float)rotationYaw.getRadians() + (float)Math.toRadians(180.0F);
        Vector3d warpPosition = (new Vector3d(x, y, z)).add(0.5F, 0.65, 0.5F);
        Transform warpTransform = new Transform(warpPosition, new Vector3f(Float.NaN, warpRotationYaw, Float.NaN));
        String warpId = name.toLowerCase();
        Warp warp = new Warp(warpTransform, name, worldChunk.getWorld(), "*Teleporter", Instant.now());
        TeleportPlugin teleportPlugin = TeleportPlugin.get();
        teleportPlugin.getWarps().put(warpId, warp);
        teleportPlugin.saveWarps();
    }
}
