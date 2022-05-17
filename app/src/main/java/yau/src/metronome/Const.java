package yau.src.metronome;

public class Const {
    public static final String DEBUG_IP_SERVER = "192.168.43.1";
    private static final int DEBUG_SERVER_PORT = 5050;
    public static final int NTP_DEFAULT_PORT = 5812;
    public static final int SERVER_PORT = Const.IS_DEBUG ? Const.DEBUG_SERVER_PORT : 0;
    public static final int MIN_BPM = 60;
    public static final int MAX_BPM = 240;
    public static final boolean IS_DEBUG = false;
}
