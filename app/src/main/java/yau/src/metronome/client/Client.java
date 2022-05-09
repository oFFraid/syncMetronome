package yau.src.metronome.client;

import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

import yau.src.metronome.utils.Listener;
import yau.src.metronome.utils.ListenerList;

public class Client extends WebSocketClient {
    public ListenerList onOpenListeners = new ListenerList();
    public ListenerList onCloseListeners = new ListenerList();
    public ListenerList onErrorListeners = new ListenerList();
    public ListenerList onMessageListeners = new ListenerList();

    public Client(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i("WebSocketClient", "on open");
        onOpenListeners.submit(null);
    }

    @Override
    public void onMessage(String message) {
        Log.i("WebSocketClient", "new server message: " + message);
        onMessageListeners.submit(message);
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("WebSocketClient", "Connection closed by " + (remote ? "remote peer" : "us") + " Code: " + code + " Reason: " + reason);
        onCloseListeners.submit(null);
    }

    @Override
    public void onError(Exception ex) {
        Log.i("WebSocketClient", ex.getMessage());
        onErrorListeners.submit(ex);
    }
}
