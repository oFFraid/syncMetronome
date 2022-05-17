package yau.src.metronome.websocket;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import yau.src.metronome.utils.ClientListeners;
import yau.src.metronome.utils.Listener;
import yau.src.metronome.utils.ListenerList;


public class Client extends WebSocketClient {
    public ClientListeners listeners = null;
    private final String PREFIX_LOG = "WebSocketClient";

    public Client(URI serverURI) {
        super(serverURI);
        listeners = new ClientListeners();
    }

    public Client(URI serverURI, ClientListeners listeners) {
        super(serverURI);
        this.listeners = listeners;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i(PREFIX_LOG, "on open");
        listeners.onOpen.submit(null);
    }

    @Override
    public void onMessage(String message) {
        listeners.onMessage.submit(message);

        Log.i(PREFIX_LOG, "new server message: " + message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        listeners.onClose.submit(null);
        Log.i(PREFIX_LOG, "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        Log.i(PREFIX_LOG, ex.getMessage());
        listeners.onError.submit(ex);
    }
}
