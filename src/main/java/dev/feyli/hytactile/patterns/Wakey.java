package dev.feyli.hytactile.patterns;

import dev.feyli.hytactile.HyTactilePlugin;
import io.github.blackspherefollower.buttplug4j.client.ButtplugClientDevice;
import io.github.blackspherefollower.buttplug4j.client.ButtplugDeviceException;

public class Wakey {
    private Wakey() {
        throw new IllegalStateException("Utility class");
    }

    public static void play(ButtplugClientDevice device) {
        try {
            for (int i = 0; i < 2; i++) {
                device.sendScalarVibrateCmd(0.1f);
                Thread.sleep(100);
                device.sendScalarVibrateCmd(0.0f);
                Thread.sleep(100);
            }
        } catch (ButtplugDeviceException | InterruptedException e) {
            HyTactilePlugin.LOGGER.atWarning().log("Error playing Wakey pattern: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
