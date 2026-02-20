package org.zone404.plugins.pages;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.zone404.plugins.RandomWarpNameWhenTeleporterPlacedSystem;

import javax.annotation.Nonnull;
import java.util.UUID;

public class TeleporterSettingsPage extends com.hypixel.hytale.builtin.adventure.teleporter.page.TeleporterSettingsPage {
    @Nonnull
    private final Ref<ChunkStore> blockRef;
    private final Mode mode;

    public TeleporterSettingsPage(@Nonnull PlayerRef playerRef, @Nonnull Ref<ChunkStore> blockRef, Mode mode) {
        super(playerRef, blockRef, mode);
        this.blockRef = blockRef;
        this.mode = mode;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        Teleporter teleporter = this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
        commandBuilder.append("Teleporter.ui");
        if (teleporter == null) {
            commandBuilder.set("#ErrorScreen.Visible", true);
            commandBuilder.set("#FullSettings.Visible", false);
            commandBuilder.set("#WarpSettings.Visible", false);
            commandBuilder.set("#Buttons.Visible", false);
        } else {
            commandBuilder.set("#ErrorScreen.Visible", false);
            commandBuilder.set("#FullSettings.Visible", this.mode == com.hypixel.hytale.builtin.adventure.teleporter.page.TeleporterSettingsPage.Mode.FULL);
            switch (this.mode.ordinal()) {
                case 0:
                    byte relativeMask = teleporter.getRelativeMask();
                    commandBuilder.set("#BlockRelative #CheckBox.Value", (relativeMask & 64) != 0);
                    Transform transform = teleporter.getTransform();
                    if (transform != null) {
                        commandBuilder.set("#X #Input.Value", transform.getPosition().getX());
                        commandBuilder.set("#Y #Input.Value", transform.getPosition().getY());
                        commandBuilder.set("#Z #Input.Value", transform.getPosition().getZ());
                    }

                    commandBuilder.set("#X #CheckBox.Value", (relativeMask & 1) != 0);
                    commandBuilder.set("#Y #CheckBox.Value", (relativeMask & 2) != 0);
                    commandBuilder.set("#Z #CheckBox.Value", (relativeMask & 4) != 0);
                    if (transform != null) {
                        commandBuilder.set("#Yaw #Input.Value", transform.getRotation().getYaw());
                        commandBuilder.set("#Pitch #Input.Value", transform.getRotation().getPitch());
                        commandBuilder.set("#Roll #Input.Value", transform.getRotation().getRoll());
                    }

                    commandBuilder.set("#Yaw #CheckBox.Value", (relativeMask & 8) != 0);
                    commandBuilder.set("#Pitch #CheckBox.Value", (relativeMask & 16) != 0);
                    commandBuilder.set("#Roll #CheckBox.Value", (relativeMask & 32) != 0);
                    ObjectArrayList<DropdownEntryInfo> worlds = new ObjectArrayList<>();
                    worlds.add(new DropdownEntryInfo(LocalizableString.fromMessageId("server.customUI.teleporter.noWorld"), ""));

                    for (World world : Universe.get().getWorlds().values()) {
                        worlds.add(new DropdownEntryInfo(LocalizableString.fromString(world.getName()), world.getWorldConfig().getUuid().toString()));
                    }

                    commandBuilder.set("#WorldDropdown.Entries", worlds);
                    UUID worldUuid = teleporter.getWorldUuid();
                    commandBuilder.set("#WorldDropdown.Value", worldUuid != null ? worldUuid.toString() : "");

                    commandBuilder.set("#WarpInput.Value", teleporter.getWarp() != null ? teleporter.getWarp() : "");
                    commandBuilder.set("#NewWarp.Value", teleporter.getOwnedWarp() != null ? teleporter.getOwnedWarp() : "");
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", (new EventData()).append("@BlockRelative", "#BlockRelative #CheckBox.Value").append("@X", "#X #Input.Value").append("@Y", "#Y #Input.Value").append("@Z", "#Z #Input.Value").append("@XIsRelative", "#X #CheckBox.Value").append("@YIsRelative", "#Y #CheckBox.Value").append("@ZIsRelative", "#Z #CheckBox.Value").append("@Yaw", "#Yaw #Input.Value").append("@Pitch", "#Pitch #Input.Value").append("@Roll", "#Roll #Input.Value").append("@YawIsRelative", "#Yaw #CheckBox.Value").append("@PitchIsRelative", "#Pitch #CheckBox.Value").append("@RollIsRelative", "#Roll #CheckBox.Value").append("@World", "#WorldDropdown.Value").append("@Warp", "#WarpInput.Value").append("@NewWarp", "#NewWarp.Value"));
                    break;
                case 1:
                    commandBuilder.set("#WarpInput.Value", teleporter.getWarp() != null ? teleporter.getWarp() : "");
                    String placeholder;
                    if (teleporter.hasOwnedWarp() && !teleporter.isCustomName()) {
                        placeholder = teleporter.getOwnedWarp();
                    } else {
                        placeholder = "";
                    }

                    commandBuilder.set("#NewWarp.PlaceholderText", placeholder);
                    String value = teleporter.isCustomName() && teleporter.getOwnedWarp() != null ? teleporter.getOwnedWarp() : "";
                    commandBuilder.set("#NewWarp.Value", value);
                    eventBuilder.addEventBinding(CustomUIEventBindingType.Activating, "#SaveButton", (new EventData()).append("@Warp", "#WarpInput.Value").append("@NewWarp", "#NewWarp.Value"));
            }
        }
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref, @Nonnull Store<EntityStore> store, @Nonnull PageEventData data) {
        var isCustomName = true;
        Teleporter teleporter = this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
        if (data.destinationWarp != null) {
            data.destinationWarp = data.destinationWarp.toLowerCase();
        }
        if (data.warpName == null || data.warpName.isEmpty()) {
            if (teleporter != null && !teleporter.isCustomName()) {
                data.warpName = teleporter.getOwnedWarp();
            } else {
                data.warpName = RandomWarpNameWhenTeleporterPlacedSystem.generatePortalName(10);
            }
            isCustomName = false;
        }
        super.handleDataEvent(ref, store, data);
        if (teleporter != null) {
            teleporter.setIsCustomName(isCustomName);
        }
    }
}
