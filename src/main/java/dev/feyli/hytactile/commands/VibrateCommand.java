package dev.feyli.hytactile.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import dev.feyli.hytactile.HyTactilePlugin;
import io.github.blackspherefollower.buttplug4j.client.ButtplugClientDevice;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VibrateCommand extends AbstractCommand {

    public VibrateCommand(String name, String description) {
        super(name, description);
    }

    @Nullable
    @Override
    protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
        // Check if client is connected before attempting to control devices
        if (!HyTactilePlugin.buttplugClient.isConnected()) {
            context.sendMessage(Message.parse("§cNot connected to Intiface server. Please wait for connection to be established."));
            return CompletableFuture.completedFuture(null);
        }
        
        List<ButtplugClientDevice> devices = HyTactilePlugin.buttplugClient.getDevices();
        
        if (devices.isEmpty()) {
            context.sendMessage(Message.parse("§eNo devices found. Make sure your devices are connected and scanning is enabled."));
            return CompletableFuture.completedFuture(null);
        }
        
        for (ButtplugClientDevice device : devices) {
            try {
                device.sendScalarVibrateCmd(0.3f);
            } catch (Exception e) {
                context.sendMessage(Message.parse("§cAn error occurred while trying to vibrate the device: " + e.getMessage()));
            }
        }
        return CompletableFuture.completedFuture(null);
    }

}
