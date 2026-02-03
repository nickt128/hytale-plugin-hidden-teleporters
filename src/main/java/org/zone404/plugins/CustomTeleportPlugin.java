package org.zone404.plugins;

import com.hypixel.hytale.assetstore.event.LoadedAssetsEvent;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.builtin.teleport.commands.teleport.SpawnCommand;
import com.hypixel.hytale.builtin.teleport.commands.teleport.TeleportCommand;
import com.hypixel.hytale.builtin.teleport.commands.warp.WarpCommand;
import com.hypixel.hytale.builtin.teleport.components.TeleportHistory;
import com.hypixel.hytale.common.plugin.PluginIdentifier;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.SemverRange;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginManager;
import com.hypixel.hytale.server.core.plugin.PluginState;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.events.AddWorldEvent;
import com.hypixel.hytale.server.core.universe.world.events.AllWorldsLoadedEvent;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonArray;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/// Overrides `TeleportPlugin` and changes the save/load path to warps_hidden.json instead of warps.json.
/// If a warps.json exists but not a warps_hidden.json the warps.json be moved to warps_hidden.json.
@SuppressWarnings("unused")
public class CustomTeleportPlugin extends TeleportPlugin {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.get("HiddenTeleporters|CustomTeleport");
    @Nonnull
    private final ReentrantLock saveLock = new ReentrantLock();
    @Nonnull
    private final AtomicBoolean postSaveRedo = new AtomicBoolean(false);

    public CustomTeleportPlugin(@Nonnull JavaPluginInit init) {
        super(init);
        var tpPlugin = PluginManager.get().getPlugin(PluginIdentifier.fromString("Hytale:Teleport"));
        disableTeleportPlugin((TeleportPlugin) tpPlugin);
        if (tpPlugin != null && tpPlugin.isDisabled()) {
            this.getLogger().at(Level.INFO).log("Hytale:Teleport disabled successfully.");
        } else {
            this.getLogger().at(Level.WARNING).log("Failed to disable Hytale:Teleport");
        }

        var teleporterPlugin = PluginManager.get().getPlugin(PluginIdentifier.fromString("Hytale:Teleporter"));
        if (teleporterPlugin != null) {
            var deps = new HashMap<>(teleporterPlugin.getManifest().getDependencies());
            deps.remove(PluginIdentifier.fromString("Hytale:Teleport"));
            setDependencies(teleporterPlugin, deps);
            this.getLogger().at(Level.INFO).log("Successfully removed Hytale:Teleport dependency from Hytale:Teleporter.");
        } else {
            this.getLogger().at(Level.WARNING).log("Failed to replace Hytale:Teleport dependency");
        }
    }

    @Nonnull
    @Override
    public HytaleLogger getLogger() {
        return LOGGER;
    }

    @Override
    protected void setup() {
        setInstance(this);
        CommandRegistry commandRegistry = this.getCommandRegistry();
        EventRegistry eventRegistry = this.getEventRegistry();
        commandRegistry.registerCommand(new TeleportCommand());
        commandRegistry.registerCommand(new WarpCommand());
        commandRegistry.registerCommand(new SpawnCommand());
        eventRegistry.register(LoadedAssetsEvent.class, ModelAsset.class, this::onModelAssetChange);
        eventRegistry.registerGlobal(ChunkPreLoadProcessEvent.class, this::onChunkPreLoadProcess);
        eventRegistry.registerGlobal(AddWorldEvent.class, CustomTeleportPlugin::onAddWorld);
        eventRegistry.registerGlobal(AllWorldsLoadedEvent.class, (_) -> this.loadWarps());
        this.setPrivateField("teleportHistoryComponentType", EntityStore.REGISTRY.registerComponent(TeleportHistory.class, TeleportHistory::new));
        this.setPrivateField("warpComponentType", EntityStore.REGISTRY.registerComponent(WarpComponent.class, () -> {
            throw new UnsupportedOperationException("WarpComponent must be created manually");
        }));
    }

    @Override
    public void loadWarps() {
        this.getLogger().at(Level.INFO).log("loading warps_hidden.json");
        BsonDocument document = null;
        Path universePath = Universe.get().getPath();
        Path oldPath = universePath.resolve("warps.json");
        Path path = universePath.resolve("warps_hidden.json");

        if (Files.exists(oldPath) && !Files.exists(path)) {
            try {
                this.getLogger().at(Level.INFO).log("moving warps.json to warps_hidden.json");
                Files.move(oldPath, path);
            } catch (IOException _) {
            }
        }

        if (Files.exists(path)) {
            document = BsonUtil.readDocument(path).join();
        }

        if (document != null) {
            BsonArray bsonWarps = document.containsKey("Warps") ? document.getArray("Warps") : document.getArray("warps");
            this.getWarps().clear();

            for(Warp warp : Objects.requireNonNull(Warp.ARRAY_CODEC.decode(bsonWarps))) {
                this.getWarps().put(warp.getId().toLowerCase(), warp);
            }

            this.getLogger().at(Level.INFO).log("Loaded %d hidden warps", bsonWarps.size());
        } else {
            this.getLogger().at(Level.INFO).log("Loaded 0 warps (No warps_hidden.json found)");
        }

        // set private loaded field to true
        try {
            Field loaded = this.getClass().getSuperclass().getDeclaredField("loaded");
            loaded.setAccessible(true);
            ((AtomicBoolean) loaded.get(this)).set(true);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveWarps0() {
        Warp[] array = this.getWarps().values().toArray(Warp[]::new);
        BsonDocument document = new BsonDocument("Warps", Warp.ARRAY_CODEC.encode(array));
        Path path = Universe.get().getPath().resolve("warps_hidden.json");
        BsonUtil.writeDocument(path, document).join();
        this.getLogger().at(Level.INFO).log("Saved %d hidden warps to warps_hidden.json", array.length);
    }

    @Override
    public void saveWarps() {
        if (this.saveLock.tryLock()) {
            try {
                this.saveWarps0();
            } catch (Throwable e) {
                this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to save hidden warps:");
            } finally {
                this.saveLock.unlock();
            }

            if (this.postSaveRedo.getAndSet(false)) {
                this.saveWarps();
            }
        } else {
            this.postSaveRedo.set(true);
        }

    }

    public static void setInstance(CustomTeleportPlugin instance) {
        try {
            Field instanceField = TeleportPlugin.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, instance);
            LOGGER.at(Level.INFO).log("TeleportPlugin instance successfully overridden.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.at(Level.SEVERE).log("Failed to override TeleportPlugin instance: " + e);
        }
    }

    private void setPrivateField(String name, Object value) {
        try {
            Field field = TeleportPlugin.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(TeleportPlugin.get(), value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.at(Level.SEVERE).log("Failed to set private field: " + e);
        }
    }

    private static void disableTeleportPlugin(TeleportPlugin plugin) {
        try {
            Field stateField = PluginBase.class.getDeclaredField("state");
            stateField.setAccessible(true);
            stateField.set(plugin, PluginState.DISABLED);
            LOGGER.at(Level.INFO).log("TeleportPlugin disabled.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.at(Level.SEVERE).log("Failed to disable TeleportPlugin: " + e);
        }
    }

    public static void setDependencies(PluginBase plugin, Map<PluginIdentifier, SemverRange> dependencies) {
        var manifest = plugin.getManifest();
        try {
            Field stateField = PluginManifest.class.getDeclaredField("dependencies");
            stateField.setAccessible(true);
            stateField.set(manifest, dependencies);
            LOGGER.at(Level.INFO).log("Dependencies overridden successfully.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            LOGGER.at(Level.SEVERE).log("Failed to disable TeleportPlugin: " + e);
        }
    }

    private Method getPrivateMethod(String methodName, Class<?>...parameterTypes) throws NoSuchMethodException {
        Method method = TeleportPlugin.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    private void onModelAssetChange(@Nonnull LoadedAssetsEvent<String, ModelAsset, DefaultAssetMap<String, ModelAsset>> event) {
        try {
            Method method = this.getPrivateMethod("onModelAssetChange", LoadedAssetsEvent.class);
            method.invoke(TeleportPlugin.get(), event);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            this.getLogger().at(Level.SEVERE).log("Failed call onModelAssetChange: " + e);
        }
    }

    private void onChunkPreLoadProcess(@Nonnull ChunkPreLoadProcessEvent event) {
        try {
            Method method = this.getPrivateMethod("onChunkPreLoadProcess", ChunkPreLoadProcessEvent.class);
            method.invoke(TeleportPlugin.get(), event);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            this.getLogger().at(Level.SEVERE).log("Failed call onChunkPreLoadProcess: " + e);
        }
    }

    private static void onAddWorld(AddWorldEvent event) {
        var markerProviders = event.getWorld().getWorldMapManager().getMarkerProviders();
        if (markerProviders.get("warps") != null) {
            LOGGER.at(Level.INFO).log("Removing warps marker provider.");
            markerProviders.remove("warps");
        } else {
            LOGGER.at(Level.INFO).log("Warps marker provider not found.");
        }
    }
}
