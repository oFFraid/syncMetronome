package yau.src.metronome.server;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
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
    }

    @Override
    public void onClose(WebSocket arg0, int arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onError(WebSocket arg0, Exception arg1) {
        Log.i("WebSocketServer", Arrays.toString(arg1.getStackTrace()));
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
            return;
        }
        Log.i("WebSocketServer", "opened on " + this.getAddress().toString());
        isStarted = true;
        onStartListeners.submit(null);
    }

    @Override
    public void onMessage(WebSocket conn, String mess) {
        long t1 = System.currentTimeMillis();
        try {
            ClientMessage clientMessage = gson.fromJson(mess, ClientMessage.class);
            Log.i("ADDRESS", conn.getRemoteSocketAddress().toString());

            String senderHostAddress = conn.getRemoteSocketAddress().getAddress().getHostAddress();
            ServerMessage msg = new ServerMessage(senderHostAddress, clientMessage);
            msg.setT1(t1);
            msg.setT2(System.currentTimeMillis());
            this.broadcast(gson.toJson(msg));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {

    }
}
