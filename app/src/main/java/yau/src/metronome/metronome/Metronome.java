package yau.src.metronome.metronome;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.SoundPool;
import android.media.AudioAttributes;
import android.media.ToneGenerator;
import android.util.Log;

import yau.src.metronome.Const;
import yau.src.metronome.R;

public class Metronome {
    private int bpm = Const.MIN_BPM;
    private boolean shouldPauseOnLostFocus = true;

    private final Context context;

    public MetronomeState getCurrentState() {
        return currentState;
    }

    private MetronomeState currentState = MetronomeState.INITIALIZING;

    private SoundPool soundPool;

    private final ScheduledThreadPoolExecutor scheduledExecutor = new ScheduledThreadPoolExecutor(1);
    private ScheduledFuture<?> scheduledFuture;

    private final Runnable tik = () -> {
        Log.i("TICK", System.currentTimeMillis() + "");
        soundPool.play(1, 1, 1, 2, 0, 1.0f);
    };

    public Metronome(Context context) {
        this.context = context;
        initializeSoundPool();
    }

    public int getIntervalMS() {
        return 60000 / bpm;
    }

    private void initializeSoundPool() {
        // Use the new SoundPool builder on newer version of android
        this.soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .build();
        soundPool.setOnLoadCompleteListener((SoundPool var1, int var2, int var3) -> currentState = MetronomeState.STOPPED);
        int soundResourceId = R.raw.sound2;
        soundPool.load(context, soundResourceId, 2);
    }

    public void onHostResume() {
        if (this.currentState == MetronomeState.PAUSED) {
            start();
        }
    }

    public void onHostPause() {
        if (this.currentState == MetronomeState.PLAYING && this.shouldPauseOnLostFocus) {
            this.stop();
            this.currentState = MetronomeState.PAUSED;
        }
    }

    public void onHostDestroy() {
        stop();
    }

    public void start() {
        if (currentState != MetronomeState.PLAYING) {
            scheduledExecutor.setRemoveOnCancelPolicy(true);
            scheduledFuture = scheduledExecutor.scheduleAtFixedRate(this.tik, 0, this.getIntervalMS(), TimeUnit.MILLISECONDS);
            currentState = MetronomeState.PLAYING;
        }
    }

    public void start(long delay) {
        if (currentState != MetronomeState.PLAYING) {
            scheduledExecutor.setRemoveOnCancelPolicy(true);
            scheduledFuture = scheduledExecutor.scheduleAtFixedRate(this.tik, delay, this.getIntervalMS(), TimeUnit.MILLISECONDS);
            currentState = MetronomeState.PLAYING;
        }
    }

    public void stop() {
        if (this.currentState == MetronomeState.PLAYING) {
            this.scheduledFuture.cancel(false);
            this.currentState = MetronomeState.STOPPED;
        }
    }

    public void setBPM(int newBPM) {
        this.bpm = newBPM;

        // If currently playing, need to restart to pick up the new BPM
        if (this.currentState == MetronomeState.PLAYING) {
            this.stop();
            this.start();
        }
    }

    public int getBPM() {
        return this.bpm;
    }

    public void setShouldPauseOnLostFocus(boolean shouldPause) {
        this.shouldPauseOnLostFocus = shouldPause;
    }

    public boolean getShouldPauseOnLostFocus() {
        return this.shouldPauseOnLostFocus;
    }

    public boolean isPlaying() {
        return this.currentState == MetronomeState.PLAYING;
    }

    public boolean isPaused() {
        return this.currentState == MetronomeState.PAUSED;
    }
}
