package yau.src.metronome.websocket;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Arrays;

import yau.src.metronome.Const;
import yau.src.metronome.models.ClientMessage;
import yau.src.metronome.models.ServerMessage;
import yau.src.metronome.ntp.NTPServer;
import yau.src.metronome.utils.ListenerList;

public class Server extends WebSocketServer {
    Gson gson = new GsonBuilder().create();
    NTPServer timeServer = null;
    public ListenerList onStartListeners = new ListenerList();
    public ListenerList onCloseServerListeners = new ListenerList();
    public ListenerList onErrorListeners = new ListenerList();
    public boolean isStarted = false;
    private final String PREFIX_LOG = "WebSocketServer";

    public Server() {
        super();
    }

    public Server(InetSocketAddress serverAddress) {
        super(serverAddress);
    }

    @Override
    public void stop() throws InterruptedException {
        if (timeServer != null && timeServer.isRunning()) {
            timeServer.stop();
        }
        super.stop();
        isStarted = false;
        onCloseServerListeners.submit(null);
        Log.i(PREFIX_LOG, "was stop");
    }

    @Override
    public void onClose(WebSocket conn, int arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        Log.i(PREFIX_LOG, Arrays.toString(arg1.getStackTrace()));
        isStarted = false;
        onErrorListeners.submit(arg1);
    }


    @Override
    public void onStart() {
        timeServer = new NTPServer(Const.NTP_DEFAULT_PORT);
        try {
            timeServer.start();
        } catch (final IOException e) {
            e.printStackTrace();
            this.onError(null, new Exception("NTP server not started"));
            try {
                this.stop();
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            return;
        }
        isStarted = true;
        onStartListeners.submit(null);
        Log.i(PREFIX_LOG, "opened on " + this.getAddress().toString());
    }

    @Override
    public void onMessage(WebSocket conn, String mess) {
        long t1 = System.currentTimeMillis();
        try {
            ClientMessage lastMessage = gson.fromJson(mess, ClientMessage.class);
            String senderHostAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
            ServerMessage msg = new ServerMessage(senderHostAddress, lastMessage);
            msg.setT1(t1);
            msg.setT2(System.currentTimeMillis());
            broadcast(gson.toJson(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        super.onMessage(conn, message);

    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }
}
