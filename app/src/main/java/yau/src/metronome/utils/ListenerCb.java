package yau.src.metronome.utils;

import android.app.Activity;

public class ListenerCb implements Listener {
    Activity owner;
    Listener listener;

    public ListenerCb(Activity owner, Listener listener) {
        this.owner = owner;
        this.listener = listener;
    }

    @Override
    public void submit(Object o) {
        owner.runOnUiThread(() -> {
            listener.submit(o);
        });
    }
}
