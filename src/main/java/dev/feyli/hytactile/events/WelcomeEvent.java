package dev.feyli.hytactile.events;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;

public class WelcomeEvent {
    private WelcomeEvent() {
        throw new IllegalStateException("Utility class");
    }

    public static void onPlayerReady(PlayerReadyEvent event) {
        Player player = event.getPlayer();
        player.sendMessage(Message.raw("Welcome " + player.getDisplayName()));
    }

}