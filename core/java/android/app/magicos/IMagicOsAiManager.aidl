package android.app.magicos;

import android.os.Bundle;

/**
 * Interface definition for MagicOS AI Framework abstraction layer.
 * {@hide}
 */
interface IMagicOsAiManager {
    boolean isAiEngineAvailable();
    Bundle processContextPayload(in Bundle contextData);
    void registerCapsuleCallback(in IBinder token, String componentId);
    void unregisterCapsuleCallback(in IBinder token);
}
