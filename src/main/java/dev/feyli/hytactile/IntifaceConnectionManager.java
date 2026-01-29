package dev.feyli.hytactile;

import com.hypixel.hytale.logger.HytaleLogger;
import io.github.blackspherefollower.buttplug4j.connectors.jetty.websocket.client.ButtplugClientWSClient;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Manages the connection to the Intiface server with automatic retry and reconnection logic.
 * Implements exponential backoff strategy to prevent overwhelming the server.
 * Singleton pattern ensures only one connection manager exists.
 */
public class IntifaceConnectionManager {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    
    // Singleton instance
    private static final AtomicReference<IntifaceConnectionManager> instance = new AtomicReference<>();

    // Retry configuration constants
    private static final int INITIAL_RETRY_DELAY_MS = 1000;      // 1 second
    private static final int MAX_RETRY_DELAY_MS = 30000;         // 30 seconds
    private static final double BACKOFF_MULTIPLIER = 2.0;        // Double each time
    
    // Connection state tracking
    private enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    
    private final ButtplugClientWSClient client;
    private final URI serverUri;
    private final ScheduledExecutorService executorService;
    private final AtomicReference<ConnectionState> state;
    private final AtomicInteger attemptCount;
    
    /**
     * Private constructor to prevent direct instantiation.
     * Use getInstance() to get the singleton instance.
     *
     * @param client The Buttplug WebSocket client to manage
     * @param serverUri The URI of the Intiface server
     */
    private IntifaceConnectionManager(ButtplugClientWSClient client, URI serverUri) {
        this.client = client;
        this.serverUri = serverUri;
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "IntifaceConnectionManager");
            thread.setDaemon(true);
            return thread;
        });
        this.state = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.attemptCount = new AtomicInteger(0);
    }
    
    /**
     * Gets the singleton instance of the connection manager.
     * Creates a new instance if one doesn't exist.
     *
     * @param client The Buttplug WebSocket client to manage
     * @param serverUri The URI of the Intiface server
     * @return The singleton IntifaceConnectionManager instance
     */
    public static IntifaceConnectionManager getInstance(ButtplugClientWSClient client, URI serverUri) {
        if (instance.get() == null) {
            synchronized (IntifaceConnectionManager.class) {
                if (instance.get() == null) {
                    instance.set(new IntifaceConnectionManager(client, serverUri));
                }
            }
        }
        return instance.get();
    }
    
    /**
     * Gets the existing singleton instance.
     * Returns null if getInstance(client, serverUri) has not been called yet.
     *
     * @return The singleton IntifaceConnectionManager instance, or null if not initialized
     */
    public static IntifaceConnectionManager getInstance() {
        return instance.get();
    }
    
    /**
     * Initiates connection to the Intiface server with automatic retry.
     * This method returns immediately and attempts connection asynchronously.
     */
    public void connect() {
        if (state.get() == ConnectionState.CONNECTED || state.get() == ConnectionState.CONNECTING) {
            LOGGER.atWarning().log("Connection already active or in progress. Ignoring connect request.");
            return;
        }
        
        attemptCount.set(0);
        scheduleReconnect(0);
    }
    
    /**
     * Schedules a reconnection attempt after the specified delay.
     *
     * @param delayMs Delay in milliseconds before attempting connection
     */
    private void scheduleReconnect(long delayMs) {
        state.set(ConnectionState.CONNECTING);
        
        executorService.schedule(this::attemptConnection, delayMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Attempts to connect to the Intiface server.
     * On failure, schedules another attempt with exponential backoff.
     */
    private void attemptConnection() {
        int currentAttempt = attemptCount.incrementAndGet();
        
        LOGGER.atInfo().log("Attempting to connect to Intiface server at %s (attempt %d)...", 
                           serverUri, currentAttempt);
        
        try {
            client.connect(serverUri);
            state.set(ConnectionState.CONNECTED);
            LOGGER.atInfo().log("Successfully connected to Intiface server.");
            
            // Reset attempt counter on successful connection
            attemptCount.set(0);
            
        } catch (Exception e) {
            state.set(ConnectionState.DISCONNECTED);
            
            // Calculate next retry delay with exponential backoff
            long nextDelay = calculateNextDelay(currentAttempt);
            
            LOGGER.atSevere().withCause(e).log(
                "Failed to connect to Intiface server: %s. Retrying in %d seconds (attempt %d)...",
                e.getMessage(), 
                nextDelay / 1000,
                currentAttempt + 1
            );
            
            // Schedule next retry attempt
            scheduleReconnect(nextDelay);
        }
    }
    
    /**
     * Calculates the next retry delay using exponential backoff.
     *
     * @param attemptNumber The current attempt number (1-based)
     * @return Delay in milliseconds before next retry
     */
    private long calculateNextDelay(int attemptNumber) {
        if (attemptNumber <= 1) {
            return INITIAL_RETRY_DELAY_MS;
        }
        
        // Calculate exponential backoff: INITIAL_DELAY * (MULTIPLIER ^ (attempt - 1))
        long delay = (long) (INITIAL_RETRY_DELAY_MS * Math.pow(BACKOFF_MULTIPLIER, attemptNumber - 1.0));
        
        // Cap at maximum delay
        return Math.min(delay, MAX_RETRY_DELAY_MS);
    }
    
    /**
     * Initiates reconnection to the Intiface server.
     * This should be called when a disconnect is detected.
     */
    public void reconnect() {
        LOGGER.atWarning().log("Connection lost to Intiface server. Initiating reconnection...");
        state.set(ConnectionState.DISCONNECTED);
        attemptCount.set(0);
        scheduleReconnect(0);
    }
    
    /**
     * Checks if the client is currently connected to the Intiface server.
     *
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }
    
    /**
     * Shuts down the connection manager and cleans up resources.
     * This should be called when the plugin is shutting down.
     */
    public void shutdown() {
        LOGGER.atInfo().log("Shutting down Intiface connection manager...");
        
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException _) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        state.set(ConnectionState.DISCONNECTED);
    }
}
