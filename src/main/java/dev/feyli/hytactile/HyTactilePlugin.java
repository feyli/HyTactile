package dev.feyli.hytactile;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import dev.feyli.hytactile.commands.VibrateCommand;
import dev.feyli.hytactile.events.WelcomeEvent;
import dev.feyli.hytactile.patterns.Wakey;
import io.github.blackspherefollower.buttplug4j.client.ButtplugClientDevice;
import io.github.blackspherefollower.buttplug4j.client.IDeviceEvent;
import io.github.blackspherefollower.buttplug4j.connectors.jetty.websocket.client.ButtplugClientWSClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ExecutionException;

public class HyTactilePlugin extends JavaPlugin {

    public HyTactilePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final ButtplugClientWSClient buttplugClient = new ButtplugClientWSClient("HyTactile Client");

    private static final Integer PORT = 12345;

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new VibrateCommand("vibrate", "Sends a slight vibration to the player running the command."));
        this.getEventRegistry().registerGlobal(PlayerReadyEvent.class, WelcomeEvent::onPlayerReady);

        buttplugClient.setOnConnected(_ -> {
            LOGGER.atInfo().log("Buttplug client connected successfully. ");
            try {
                buttplugClient.startScanning();
            } catch (IOException | ExecutionException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LOGGER.atSevere().withCause(e).log("Failed to start scanning for devices.");
            }
        });


        IDeviceEvent deviceEventHandler = new IDeviceEvent() {
            @Override
            public void deviceAdded(ButtplugClientDevice newDevice) {
                Wakey.play(newDevice);
                LOGGER.atInfo().log("New device added: " + newDevice.getName());
            }

            @Override
            public void deviceRemoved(long l) {
                LOGGER.atInfo().log("Device removed: " + l);
            }
        };

        buttplugClient.setDeviceAdded(deviceEventHandler);

        try {
            String uriString = "ws://127.0.0.1:%d/buttplug".formatted(PORT);
            buttplugClient.connect(new URI(uriString));
        } catch (Exception e) {
            // TODO: Attempt reconnection until successful
            LOGGER.atSevere().withCause(e).log("Failed to connect to Buttplug server.");
        }
    }

    @Override
    protected void shutdown() {
        try {
            // Stop all devices before disconnecting
            buttplugClient.stopAllDevices();
            LOGGER.atInfo().log("Stopped all devices.");

            // Disconnect from the Buttplug server
            buttplugClient.disconnect();
            LOGGER.atInfo().log("Disconnected from Buttplug server.");
        } catch (ExecutionException | InterruptedException | IOException e) {
            LOGGER.atSevere().withCause(e).log("Failed to disconnect from Buttplug server.");
            Thread.currentThread().interrupt();
        }
    }
}