package com.app.medikit.util;

public class Constants {

    /** Other **/
    public static String        TAG = "Hell";
    public static boolean       IS_NETWORK_CONNECTED = false;
    public static String        DATA_LISTENER_KEY = "dataListenerKey";
    public static String        DATA_INTENT_KEY = "dataIntentKey";
    public static String        CONTACT_BUNDLE_KEY = "contactBundleKey";

    /** Data Min Max Value **/
    public static final double  TEMPERATURE_MIN_VALUE = 97;
    public static final double  TEMPERATURE_MAX_VALUE = 99;
    public static final long    PULSE_MIN_VALUE = 60;
    public static final long    PULSE_MAX_VALUE = 100;
    public static final long    SPO2_NORMAL_VALUE = 94;

    /** Timer **/
    public static final long    SPLASH_TIMING = 2000;
    public static final long    REFRESH_TIMING = 60000; /*1 min*/
}
