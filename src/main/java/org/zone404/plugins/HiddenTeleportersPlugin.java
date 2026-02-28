package org.zone404.plugins;

import com.hypixel.hytale.builtin.adventure.teleporter.TeleporterPlugin;
import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.interaction.components.PlacedByInteractionComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.zone404.plugins.pages.TeleporterSettingsPageSupplier;

import javax.annotation.Nonnull;

@SuppressWarnings("unused")
public class HiddenTeleportersPlugin extends JavaPlugin {
    private static HiddenTeleportersPlugin instance;

    public HiddenTeleportersPlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Nonnull
    @Override
    public HytaleLogger getLogger() {
        return super.getLogger();
    }

    @Override
    protected void setup() {
        instance = this;
        this.getCodecRegistry(OpenCustomUIInteraction.PAGE_CODEC).register("Teleporter", TeleporterSettingsPageSupplier.class, TeleporterSettingsPageSupplier.CODEC);
        ComponentType<ChunkStore, PlacedByInteractionComponent> placedByInteractionComponentType = PlacedByInteractionComponent.getComponentType();
        ComponentType<ChunkStore, Teleporter> teleporterComponentType = TeleporterPlugin.get().getTeleporterComponentType();
        ComponentType<ChunkStore, BlockModule.BlockStateInfo> blockStateInfoComponentType = BlockModule.BlockStateInfo.getComponentType();
        this.getChunkStoreRegistry().registerSystem(new RandomWarpNameWhenTeleporterPlacedSystem(placedByInteractionComponentType, teleporterComponentType, blockStateInfoComponentType));
    }

    @Override
    protected void shutdown() {}

    @Nonnull
    public static HiddenTeleportersPlugin get() {
        return instance;
    }
}
