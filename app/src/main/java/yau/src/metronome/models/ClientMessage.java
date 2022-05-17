package yau.src.metronome.models;

import yau.src.metronome.metronome.MetronomeState;

public class ClientMessage {
    public ClientMessage(MetronomeState state, int bpm) {
        this.state = state;
        this.bpm = bpm;
    }

    public MetronomeState state;
    public int bpm;
}
