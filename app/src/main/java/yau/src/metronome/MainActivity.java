package yau.src.metronome;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.google.android.material.slider.Slider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import java.net.InetSocketAddress;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import yau.src.metronome.utils.ClientListeners;
import yau.src.metronome.utils.WifiModule;
import yau.src.metronome.websocket.Client;
import yau.src.metronome.databinding.ActivityMainBinding;
import yau.src.metronome.metronome.Metronome;
import yau.src.metronome.metronome.MetronomeState;
import yau.src.metronome.models.ClientMessage;
import yau.src.metronome.models.ServerMessage;
import yau.src.metronome.websocket.Server;
import yau.src.metronome.ntp.NTPClient;
import yau.src.metronome.utils.ListenerCb;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Gson gson = new GsonBuilder().create();
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(
            new ScanContract(),
            new QR(MainActivity.this, this::clientStart, (o) -> {
                this.runOnUiThread(() -> setEnableAll(true));
            })
    );
    int port = Const.SERVER_PORT;
    Server server = null;
    NTPClient ntp = new NTPClient();
    Client client = null;
    Metronome metronome = null;
    private Long offset = 0L;
    String serverName = "";

    //--------------------------------------------LISTENERS
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        metronome = new Metronome(this);
        SliderBpmInit();
    }

    @Override
    protected void onDestroy() {
        stopAll();
        metronome.onHostDestroy();
        super.onDestroy();
    }

    public void onClientClick(View view) {
        setEnableAll(false);
        initConnectionToServer();
    }

    public void onServerClick(View view) {
        setEnableAll(false);
        serverStart();
    }

    public void setMetronomePlay(boolean isPlay, Long delay) {
        if (isPlay) {
            metronome.start(delay);
            binding.metronomeBtn.setText(R.string.to_stop_metronome);
            return;
        }
        metronome.stop();
        binding.metronomeBtn.setText(R.string.to_start_metronome);
    }

    //-------------------------------------------------SERVER

    private void serverStart() {
        InetSocketAddress serverAddress = new InetSocketAddress(Const.SERVER_PORT);
        server = new Server(serverAddress);
        server.setReuseAddr(true);
        server.setTcpNoDelay(true);
        serverListeners();
        new Thread(() -> server.run()).start();
    }

    void serverStop() {
        try {
            if (isServer()) {
                server.stop();
            }
        } catch (InterruptedException e) {
            printError(e);
        }
    }

    void stopAll() {
        serverStop();
        clientDisconnect();
        setMetronomePlay(false, 0L);
    }

    void serverListeners() {
        String ip = WifiModule.getIPAddress();
        if (ip == null || ip.trim().equals("")) {
            toast("Вы должны быть подключены к wi-fi или активировть точку доступа на устройстве!");
            setEnableAll(true);
            return;
        }

        server.onStartListeners.add(new ListenerCb(this, o -> {
            port = server.getPort();
            String url = createWsAddress(ip, port);
            try {
                binding.qrCode.setImageBitmap(QR.generateQrCode(url));
                binding.serverBtn.setOnClickListener((v) -> stopAll());
                binding.serverBtn.setText(R.string.to_stop_server);
                clientStart(new URI(url));
            } catch (Exception e) {
                server.onError(null, new Exception("QR code can't generate or uri string is bad"));
                setEnableAll(true);
                stopAll();
                printError(e);
            }
        }));
        server.onErrorListeners.add(new ListenerCb(this, o -> {
            setEnableAll(true);
            setMetronomePlay(false, 0L);
        }));
        server.onCloseServerListeners.add(new ListenerCb(this, o -> {
            setEnableAll(true);
            binding.serverBtn.setOnClickListener((v) -> serverStart());
            binding.serverBtn.setText(R.string.to_start_server);
            binding.qrCode.setImageBitmap(null);
            setMetronomePlay(false, 0L);
        }));
    }

    boolean isServer() {
        return server != null && server.isStarted;
    }


    //-------------------------------------------------CLIENT

    public void clientStart(URI uri) {
        ClientListeners listeners = clientListeners();
        serverName = uri.getHost();
        new Thread(() -> {
            client = new Client(uri, listeners);
            client.setTcpNoDelay(true);
            try {

                client.connectBlocking();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    boolean isClient() {
        return client != null && client.isOpen();
    }

    void clientDisconnect() {
        if (isClient()) {
            client.close();
        }
    }

    ClientListeners clientListeners() {
        ClientListeners listeners = new ClientListeners();
        listeners.onOpen.add(new ListenerCb(this, o -> {
            getTimeOffset();
            setEnableAll(true);
            if (isClient() && !isServer()) {
                binding.serverBtn.setEnabled(false);
            } else {
                binding.clientBtn.setEnabled(false);
            }
            binding.clientBtn.setOnClickListener((v) -> stopAll());
            binding.clientBtn.setText(R.string.to_disconnect_client);
            setMetronomePlay(false, 0L);
        }));

        listeners.onError.add(new ListenerCb(this, o -> {
            setEnableAll(true);
            setMetronomePlay(false, 0L);
        }));

        listeners.onClose.add(new ListenerCb(this, o -> {
            binding.clientBtn.setOnClickListener((v) -> initConnectionToServer());
            binding.clientBtn.setText(R.string.to_connect_client);
            setMetronomePlay(false, 0L);
            stopAll();
            setEnableAll(true);
        }));

        listeners.onMessage.add(new ListenerCb(this, o -> {
            try {
                String message = (String) o;
                ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
                MetronomeState serverState = serverMessage.clientMessage.state;

                if (serverMessage.clientMessage.bpm != metronome.getBPM()) {
                    updateSliderBpm(serverMessage.clientMessage.bpm);
                    metronome.stop();
                }
                if (serverState == MetronomeState.PLAYING) {
                    long beforeStartNextTick = calculateDelayBeforeStart(serverMessage.t2);
                    setMetronomePlay(true, beforeStartNextTick);
                } else if (serverState == MetronomeState.STOPPED) {
                    setMetronomePlay(false, 0L);
                }
            } catch (Exception e) {
                printError(e);
            }
        }));
        return listeners;
    }

    void getTimeOffset() {
        if (isServer()) return;
        new Thread(() -> {
            try {
                String ips = Const.IS_DEBUG ? Const.DEBUG_IP_SERVER : serverName;
                ntp.exec(ips);
                offset = ntp.getOffset();
                Log.i("OFFSET NTP SERVER", ntp.getOffset() + "");
            } catch (Exception e) {
                printError(e);
                toast("Предупреждение: для лучшей синхронизации попробуйте переподключиться !");
            }
        }).start();
    }

    void initConnectionToServer() {
        if (Const.IS_DEBUG) {
            try {
                URI uri = new URI(createWsAddress(Const.DEBUG_IP_SERVER, Const.SERVER_PORT));
                clientStart(uri);
            } catch (Exception e) {
                printError(e);
            }
            return;
        }
        setEnableAll(true);
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    //--------------------------------------------------UI

    void setEnableAll(boolean isEnable) {
        binding.serverBtn.setEnabled(isEnable);
        binding.clientBtn.setEnabled(isEnable);
        binding.rangeBpm.setEnabled(isEnable);
        binding.metronomeBtn.setEnabled(isEnable);
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
    }

    void updateSliderBpm(int bpmInt) {
        metronome.setBPM(bpmInt);
        String bpm = String.valueOf(bpmInt);
        binding.rangeBpm.setValue(metronome.getBPM());
        binding.bpmText.setText(bpm);
    }

    private void SliderBpmInit() {
        binding.rangeBpm.setValueTo(Const.MAX_BPM);
        binding.rangeBpm.setValueFrom(Const.MIN_BPM);
        binding.rangeBpm.setStepSize(1);
        binding.rangeBpm.setValue(metronome.getBPM());
        binding.bpmText.setText(String.valueOf(metronome.getBPM()));

        binding.rangeBpm.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                int bpmInt = Math.round(slider.getValue());
                if (isServer() || isClient()) {
                    ClientMessage m = new ClientMessage(metronome.getCurrentState(), bpmInt);
                    client.send(gson.toJson(m));
                    return;
                }
                updateSliderBpm(bpmInt);
            }
        });
    }

    public void metronomeBtn(View view) {
        try {
            int currentBpm = metronome.getBPM();
            if (metronome.isPlaying()) {
                if (isServer() || isClient()) {
                    ClientMessage m = new ClientMessage(MetronomeState.STOPPED, currentBpm);
                    client.send(gson.toJson(m));
                    return;
                }
                setMetronomePlay(false, 0L);
                return;
            }
            if (isServer() || isClient()) {
                ClientMessage m = new ClientMessage(MetronomeState.PLAYING, currentBpm);
                client.send(gson.toJson(m));
                return;
            }
            setMetronomePlay(true, 0L);

        } catch (Exception e) {
            printError(e);
        }
    }

    //------------------------------UTILS

    void printError(Exception e) {
        Log.i("ERROR", e.getMessage());
    }

    Long getOffset() {
        return offset;
    }

    String createWsAddress(String ip, int port) {
        return "ws://" + ip + ":" + port;
    }


    void test(long unixSeconds, String pref) {
        Date date = new java.util.Date(unixSeconds);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS z", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String formattedDate = sdf.format(date);
        Log.i(pref, formattedDate);
        Log.i("TEST", getOffset() + "");
    }

    long calculateDelayBeforeStart(long timestampServerStart) {
        if (isServer()) return 0;
        long offset = getOffset() - 1;
        test(timestampServerStart, "SERVER LOCAL TIME");
        long currentMs = System.currentTimeMillis() + offset;
        test(currentMs, "CURRENT LOCAL TIME");

        long delayMs = currentMs - timestampServerStart;
        long deltaMs = metronome.getIntervalMS();
        long beforeStartNextTick = deltaMs - delayMs;

        if (beforeStartNextTick < 0) {
            beforeStartNextTick = 0;
            toast("Время задержки меньше 0");
        }
        Log.i("TIMES",
                " currentMs: " + currentMs
                        + " timestampServerStart: " + timestampServerStart
                        + " delayMs " + delayMs
                        + " deltaMs " + deltaMs
                        + " beforeStartNextTick " + beforeStartNextTick);
        return beforeStartNextTick;
    }
}