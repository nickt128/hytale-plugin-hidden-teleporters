package org.zone404.plugins.pages;

import com.hypixel.hytale.builtin.adventure.teleporter.component.Teleporter;
import com.hypixel.hytale.builtin.adventure.teleporter.system.CreateWarpWhenTeleporterPlacedSystem;
import com.hypixel.hytale.builtin.adventure.teleporter.system.TurnOffTeleportersSystem;
import com.hypixel.hytale.builtin.teleport.TeleportPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo;
import com.hypixel.hytale.server.core.ui.LocalizableString;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.zone404.plugins.RandomWarpNameWhenTeleporterPlacedSystem;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.UUID;

public class TeleporterSettingsPage extends com.hypixel.hytale.builtin.adventure.teleporter.page.TeleporterSettingsPage {
    @Nonnull
    private static final HytaleLogger LOGGER = HytaleLogger.get("HiddenTeleporters|TeleporterSettingsPage");
    @Nonnull
    private final Ref<ChunkStore> blockRef;
    private final Mode mode;

    public TeleporterSettingsPage(@Nonnull PlayerRef playerRef, @Nonnull Ref<ChunkStore> blockRef, Mode mode) {
        super(playerRef, blockRef, mode);
        this.blockRef = blockRef;
        this.mode = mode;
    }

    public void build(@Nonnull Ref<EntityStore> ref, @Nonnull UICommandBuilder commandBuilder, @Nonnull UIEventBuilder eventBuilder, @Nonnull Store<EntityStore> store) {
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        Teleporter teleporter = this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
        LOGGER.atInfo().log(playerComponent != null ? (playerComponent.getDisplayName() + " is opening") : "Opening" + " teleporter with name: " + (teleporter != null ? teleporter.getOwnedWarp() : ""));
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
                    String placeholder = "";
                    if (teleporter.hasOwnedWarp() && !teleporter.isCustomName()) {
                        placeholder = teleporter.getOwnedWarp();
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
        Player playerComponent = store.getComponent(ref, Player.getComponentType());
        if (playerComponent != null) {
            BlockModule.BlockStateInfo blockStateInfo = this.blockRef.getStore().getComponent(this.blockRef, BlockModule.BlockStateInfo.getComponentType());
            if (blockStateInfo == null) {
                playerComponent.getPageManager().setPage(ref, store, Page.None);
            } else {
                Ref<ChunkStore> chunkRef = blockStateInfo.getChunkRef();
                if (!chunkRef.isValid()) {
                    playerComponent.getPageManager().setPage(ref, store, Page.None);
                } else {
                    WorldChunk worldChunkComponent = chunkRef.getStore().getComponent(chunkRef, WorldChunk.getComponentType());

                    assert worldChunkComponent != null;

                    Teleporter teleporterComponent = this.blockRef.getStore().getComponent(this.blockRef, Teleporter.getComponentType());
                    if (teleporterComponent == null) {
                        playerComponent.getPageManager().setPage(ref, store, Page.None);
                    } else {
                        String oldOwnedWarp = teleporterComponent.getOwnedWarp();
                        boolean customName = true;
                        if (data.warpName == null || data.warpName.isEmpty()) {
                            if (oldOwnedWarp == null) {
                                data.warpName = RandomWarpNameWhenTeleporterPlacedSystem.generatePortalName(10);
                                customName = false;
                            } else {
                                data.warpName = oldOwnedWarp;
                                customName = teleporterComponent.isCustomName();
                            }

                            if (data.warpName == null) {
                                UICommandBuilder commandBuilder = new UICommandBuilder();
                                commandBuilder.set("#NewWarp.PlaceholderText", Message.translation("server.customUI.teleporter.warpNameRightHereHint"));
                                commandBuilder.set("#ErrorLabel.Text", Message.translation("server.customUI.teleporter.errorMissingWarpName"));
                                commandBuilder.set("#ErrorLabel.Visible", true);
                                this.sendUpdate(commandBuilder);
                                return;
                            }
                        }

                        if (!data.warpName.equalsIgnoreCase(oldOwnedWarp)) {
                            boolean alreadyExists = TeleportPlugin.get().getWarps().containsKey(data.warpName.toLowerCase());
                            if (alreadyExists) {
                                UICommandBuilder commandBuilder = new UICommandBuilder();
                                commandBuilder.set("#ErrorLabel.Text", Message.translation("server.customUI.teleporter.errorWarpAlreadyExists"));
                                commandBuilder.set("#ErrorLabel.Visible", true);
                                this.sendUpdate(commandBuilder);
                                return;
                            }
                        }

                        if (oldOwnedWarp != null && !oldOwnedWarp.isEmpty()) {
                            var oldWarp = TeleportPlugin.get().getWarps().remove(oldOwnedWarp.toLowerCase());
                            if (oldWarp == null) {
                                LOGGER.atWarning().log("Failed to remove old warp: " + oldOwnedWarp + " not found");
                            }
                        }

                        playerComponent.getPageManager().setPage(ref, store, Page.None);
                        String ownedWarpBefore = teleporterComponent.getOwnedWarp();
                        String destinationWarpBefore = teleporterComponent.getWarp();
                        CreateWarpWhenTeleporterPlacedSystem.createWarp(worldChunkComponent, blockStateInfo, data.warpName);
                        LOGGER.atInfo().log("Setting teleporter warp name to: " + data.warpName);
                        teleporterComponent.setOwnedWarp(data.warpName);
                        teleporterComponent.setIsCustomName(customName);
                        switch (this.mode.ordinal()) {
                            case 0:
                                teleporterComponent.setWorldUuid(data.world != null && !data.world.isEmpty() ? UUID.fromString(data.world) : null);
                                Transform transform = new Transform();
                                transform.getPosition().setX(data.x);
                                transform.getPosition().setY(data.y);
                                transform.getPosition().setZ(data.z);
                                transform.getRotation().setYaw(data.yaw);
                                transform.getRotation().setPitch(data.pitch);
                                transform.getRotation().setRoll(data.roll);
                                teleporterComponent.setTransform(transform);
                                teleporterComponent.setRelativeMask((byte)((data.xIsRelative ? 1 : 0) | (data.yIsRelative ? 2 : 0) | (data.zIsRelative ? 4 : 0) | (data.yawIsRelative ? 8 : 0) | (data.pitchIsRelative ? 16 : 0) | (data.rollIsRelative ? 32 : 0) | (data.isBlockRelative ? 64 : 0)));
                                teleporterComponent.setWarp(data.destinationWarp != null && !data.destinationWarp.isEmpty() ? data.destinationWarp.toLowerCase() : null);
                                break;
                            case 1:
                                teleporterComponent.setWorldUuid(null);
                                teleporterComponent.setTransform(null);
                                teleporterComponent.setWarp(data.destinationWarp != null && !data.destinationWarp.isEmpty() ? data.destinationWarp.toLowerCase() : null);
                        }

                        boolean ownChanged = !Objects.equals(ownedWarpBefore, teleporterComponent.getOwnedWarp());
                        boolean destinationChanged = !Objects.equals(destinationWarpBefore, teleporterComponent.getWarp());
                        if (ownChanged || destinationChanged) {
                            World world = store.getExternalData().getWorld();
                            TurnOffTeleportersSystem.updatePortalBlocksInWorld(world);
                            worldChunkComponent.markNeedsSaving();
                        }

                    }
                }
            }
        }
    }
}
