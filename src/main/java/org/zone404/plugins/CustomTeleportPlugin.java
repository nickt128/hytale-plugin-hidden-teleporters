package org.zone404.plugins;

import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.builtin.teleport.Warp;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.util.BsonUtil;
import org.bson.BsonArray;
import org.bson.BsonDocument;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/// Overrides `TeleportPlugin` and changes the save/load path to warps_hidden.json instead of warps.json.
/// If a warps.json exists but not a warps_hidden.json the warps.json be moved to warps_hidden.json.
public class CustomTeleportPlugin extends TeleportPlugin {
    @Nonnull
    private final ReentrantLock saveLock = new ReentrantLock();
    @Nonnull
    private final AtomicBoolean postSaveRedo = new AtomicBoolean(false);

    public CustomTeleportPlugin(@Nonnull JavaPluginInit init) {
        super(init);
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
}
