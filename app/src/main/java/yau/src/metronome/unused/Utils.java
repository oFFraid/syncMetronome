package yau.src.metronome.unused;

import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Date;

public class Utils {
    static long calcNTPOffset() throws IOException {
        long time = getCurrentNetworkTime();
        long offset = time - System.currentTimeMillis();
        return offset;
    }

    static public long getCurrentNetworkTime() throws IOException {
        NTPUDPClient timeClient = new NTPUDPClient();
        String TIME_SERVER = "time.google.com";
        TimeInfo timeInfo = null;

        InetAddress inetAddress = InetAddress.getByName(TIME_SERVER);
        timeInfo = timeClient.getTime(inetAddress);

        //long returnTime = timeInfo.getReturnTime();   //local device time
        long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();   //server time
        timeInfo.getMessage().getTransmitTimeStamp().getTime();
        Date time = new Date(returnTime);

        //TODO make this into a debug mode/verbose?
        if (true) {
            CharSequence text = "Time from " + TIME_SERVER + ": " + time +
                    timeInfo.getMessage().getTransmitTimeStamp().toDateString();
            Log.d("ntp", text.toString());
        }

        return returnTime;
    }

}
