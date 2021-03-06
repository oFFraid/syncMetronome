package yau.src.metronome.ntp;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.net.InetAddress;

import yau.src.metronome.Const;


public class NTPClient {
    private static final String SERVER_NAME = "pool.ntp.org";
    private volatile TimeInfo timeInfo;

    public Long getOffset() {
        return offset;
    }

    private volatile Long offset = 0L;

    public void exec(String serverName) throws Exception {
        if (serverName.trim().equals("")) {
            serverName = SERVER_NAME;
        }
        NTPUDPClient client = new NTPUDPClient();
        // We want to timeout if a response takes longer than 10 seconds
        client.setDefaultTimeout(10_000);
        InetAddress inetAddress = InetAddress.getByName(serverName);
        TimeInfo timeInfo = client.getTime(inetAddress, Const.NTP_DEFAULT_PORT);
        timeInfo.computeDetails();
        if (timeInfo.getOffset() != null) {
            this.timeInfo = timeInfo;
            this.offset = timeInfo.getOffset();
        }
    }

    public boolean isComputed() {
        return timeInfo != null && offset != null;
    }
}

