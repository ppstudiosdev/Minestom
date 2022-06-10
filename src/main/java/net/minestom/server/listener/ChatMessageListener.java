package net.minestom.server.listener;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.command.CommandManager;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.player.PlayerChatEvent;
import net.minestom.server.event.player.PlayerChatPreviewEvent;
import net.minestom.server.message.ChatPosition;
import net.minestom.server.message.MessageSender;
import net.minestom.server.message.Messenger;
import net.minestom.server.network.ConnectionManager;
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket;
import net.minestom.server.network.packet.client.play.ClientChatPreviewPacket;
import net.minestom.server.network.packet.client.play.ClientCommandChatPacket;
import net.minestom.server.network.packet.server.play.ChatPreviewPacket;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

public class ChatMessageListener {

    private static final CommandManager COMMAND_MANAGER = MinecraftServer.getCommandManager();
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    public static void commandChatListener(ClientCommandChatPacket packet, Player player) {
        final String command = packet.message();
        if (Messenger.canReceiveCommand(player)) {
            COMMAND_MANAGER.execute(player, command);
        } else {
            Messenger.sendRejectionMessage(player);
        }
    }

    public static void chatMessageListener(ClientChatMessagePacket packet, Player player) {
        final String message = packet.message();
        if (!Messenger.canReceiveMessage(player)) {
            Messenger.sendRejectionMessage(player);
            return;
        }

        final Collection<Player> players = CONNECTION_MANAGER.getOnlinePlayers();
        PlayerChatEvent playerChatEvent = new PlayerChatEvent(player, players, () -> buildDefaultChatMessage(player, message), message, packet.signature());

        // Call the event
        EventDispatcher.callCancellable(playerChatEvent, () -> {
            final Function<PlayerChatEvent, Component> formatFunction = playerChatEvent.getChatFormatFunction();

            Component textObject;

            if (formatFunction != null) {
                // Custom format
                textObject = formatFunction.apply(playerChatEvent);
            } else {
                // Default format
                textObject = playerChatEvent.getDefaultChatFormat().get();
            }

            final Collection<Player> recipients = playerChatEvent.getRecipients();
            if (!recipients.isEmpty()) {
                // delegate to the messenger to avoid sending messages we shouldn't be
                Messenger.sendSignedPlayerMessage(recipients, textObject, ChatPosition.CHAT,
                        new MessageSender(player.getUuid(), player.getDisplayName() != null ? player.getDisplayName() :
                                Component.text(player.getUsername()), player.getTeam() == null ?
                                null : player.getTeam().getTeamDisplayName()), packet.signature());
            }
        });
    }

    private static @NotNull Component buildDefaultChatMessage(@NotNull Player player, @NotNull String message) {
        return Component.text(message);
    }

    public static void previewListener(ClientChatPreviewPacket packet, Player player) {
        final PlayerChatPreviewEvent event = new PlayerChatPreviewEvent(player, packet.queryId(), packet.query());
        MinecraftServer.getGlobalEventHandler().callCancellable(event,
                () -> player.sendPacket(new ChatPreviewPacket(event.getId(), event.getResult())));
    }
}