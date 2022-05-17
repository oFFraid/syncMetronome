package yau.src.metronome.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListenerList implements Iterable<Listener> {
    protected List<Listener> listeners = new ArrayList<>();

    public void add(Listener listener) {
        listeners.add(listener);
    }

    public void remove(Listener listener) {
        listeners.remove(listener);
    }

    public Iterator<Listener> iterator() {
        return listeners.iterator();
    }

    public void submit(Object data) {
        for (Listener listener : this) {
            listener.submit(data);
        }
    }
}

