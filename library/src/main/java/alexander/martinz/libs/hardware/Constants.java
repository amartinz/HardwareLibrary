package alexander.martinz.libs.hardware;

import alexander.martinz.libs.hardware.device.Device;

public class Constants {
    public static final int API_NOT_SUPPORTED = -100;

    public static final int NOT_INITIALIZED = -3;
    public static final int INITIALIZATION_STARTED = -2;
    public static final int INVALID = -1;

    public static final String UNAVAILABLE = "Unavailable";
    public static final String UNKNOWN = "Unknown";

    // change this to false or remove the "isRooted" check to force
    public static boolean USE_ROOT = Device.isRooted();

    public static final String[] ENABLED_STATES = { "Y", "TRUE", "1", "255" };
}
