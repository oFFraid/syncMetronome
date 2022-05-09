package yau.src.metronome.unused;

import android.annotation.SuppressLint;
import android.util.Log;

import com.instacart.library.truetime.TrueTimeRx;

import java.net.InetAddress;
import java.net.UnknownHostException;

import io.reactivex.schedulers.Schedulers;

public class TrueTime {
    @SuppressLint("CheckResult")
    public void init() {
        TrueTimeRx.build()
                .initializeRx("time.google.com")
                .subscribeOn(Schedulers.io())
                .subscribe(date -> {
                    Log.v("TT", "TrueTime was initialized and we have a time: " + date);
                }, Throwable::printStackTrace);

    }

    public Long getOffset() {
        if (isInit()) {
            return TrueTimeRx.now().getTime() - System.currentTimeMillis();
        }
        return 0L;
    }

    public boolean isInit() {
        return TrueTimeRx.isInitialized();
    }
}
