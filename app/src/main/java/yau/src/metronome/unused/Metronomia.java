package yau.src.metronome.unused;

import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.PlatformVpnProfile;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.Timer;
import java.util.TimerTask;

import yau.src.metronome.metronome.MetronomeState;

public class Metronomia {

    private Timer metronomeRepeatingTimer = null;
    private int lastBpm = 1;
    private long delayTimeIfNeeded = 0;
    private long metronomeIntervalTime = 0;
    private long metronomeRepeatStartedRepeatingAt = 0;
    ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 80);

    public MetronomeState getCurrentState() {
        return currentState;
    }

    private MetronomeState currentState = MetronomeState.INITIALIZING;

    public Metronomia(LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            if (event == Lifecycle.Event.ON_PAUSE) {
                cancel();
            } else if (event == Lifecycle.Event.ON_STOP) {
                cancel();
            }
        });
    }


    private void cancel() {
        if (metronomeRepeatingTimer != null) {
            metronomeRepeatingTimer.cancel();
            currentState = MetronomeState.STOPPED;
        }
    }

    public void start(int bpm, long startDelay) {
        cancel();
        delayTimeIfNeeded = calculateDelayTimeIfBpmIsChanged(lastBpm, bpm, delayTimeIfNeeded);

        if (startDelay != 0) {
            delayTimeIfNeeded = startDelay;
        }

        metronomeRepeatingTimer = new Timer();
        metronomeIntervalTime = getIntervalMS(bpm);
        metronomeRepeatingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                metronomeRepeatStartedRepeatingAt = System.currentTimeMillis();
                toneG.startTone(ToneGenerator.TONE_SUP_PIP, 50);
            }
        }, delayTimeIfNeeded, metronomeIntervalTime);
        lastBpm = bpm;
        currentState = MetronomeState.PLAYING;
    }

    public void pause() {
        delayTimeIfNeeded = metronomeIntervalTime - (System.currentTimeMillis() - metronomeRepeatStartedRepeatingAt);
        cancel();
    }

    public int getBPM() {
        return lastBpm;
    }

    public boolean isPlaying() {
        return this.currentState == MetronomeState.PLAYING;
    }

    public void stop() {
        cancel();
    }


    public long getIntervalMS(int bpm) {
        return (60000 / bpm);
    }
    public long getIntervalMS() {
        if(lastBpm == 0){
            return getIntervalMS(60);
        }
        return getIntervalMS(lastBpm);
    }

    private long calculateDelayTimeIfBpmIsChanged(int oldBpm, int newBpm, long lastDelayTimeIfNeeded) {
        if (oldBpm != newBpm) {
            return ((lastDelayTimeIfNeeded * getIntervalMS(newBpm)) / getIntervalMS(oldBpm));
        } else return lastDelayTimeIfNeeded;
    }
}
