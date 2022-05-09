package yau.src.metronome;

import android.app.Activity;
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

import yau.src.metronome.WifiModule.WifiModule;
import yau.src.metronome.client.Client;
import yau.src.metronome.models.ClientMessage;
import yau.src.metronome.databinding.ActivityMainBinding;
import yau.src.metronome.metronome.Metronome;
import yau.src.metronome.metronome.MetronomeState;
import yau.src.metronome.ntp.NTPClient;
import yau.src.metronome.server.Server;
import yau.src.metronome.models.ServerMessage;
import yau.src.metronome.utils.Listener;

class ListenerCb implements Listener {
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

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    Gson gson = new GsonBuilder().create();
    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(), new QR(MainActivity.this, this::clientStart));
    int port = Const.SERVER_PORT;
    Server server = null;
    NTPClient ntp = new NTPClient();
    Client client = null;
    Metronome metronome = null;
    private Long offset = 0L;

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
    protected void onStop() {
        serverStop();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        serverStop();
        super.onDestroy();
    }

    public void onClientClick(View view) {
        initConnectionToServer();
    }

    public void onServerClick(View view) {
        serverStart();
    }

    public void metronomeBtn(View view) {
        try {
            int currentBpm = metronome.getBPM();
            if (metronome.isPlaying()) {
                ClientMessage m = new ClientMessage(MetronomeState.STOPPED, currentBpm);
                client.send(gson.toJson(m));
            } else {
                ClientMessage m = new ClientMessage(MetronomeState.PLAYING, currentBpm);
                client.send(gson.toJson(m));
            }
        } catch (Exception e) {
            printError(e);
        }
    }

    //-------------------------------------------------SERVER
    private void runServer() {
        new Thread(() -> {
            server.run();
        }).start();
    }

    private void serverStart() {
        InetSocketAddress serverAddress = new InetSocketAddress(port);
        server = new Server(serverAddress);
        server.setReuseAddr(true);
        server.setTcpNoDelay(true);
        serverSubs();
        runServer();
    }

    void serverStop() {
        try {
            if (server != null) {
                server.stop();
                //QEWEQEQW EQEWQ EQWE WQ EQW EQW EQW EW EQQWEWQWQ EWQE WQ E
                Log.i("SERVER", "was stop");
            }
        } catch (InterruptedException e) {
            printError(e);
        }
    }

    void serverSubs() {
        String ip = WifiModule.getIPAddress();
        if (ip == null || ip.trim().equals("")) {
            toast("Вы должны быть подключены к wi-fi или активировть точку доступа на устройстве!");
            return;
        }

        server.onStartListeners.add(new ListenerCb(this, (o) -> {

            Log.i("RRR", "WDWDW");
            String url = createWsAddress(ip, port);
            try {
                binding.qrCode.setImageBitmap(QR.generateQrCode(url));
                binding.serverBtn.setOnClickListener((v) -> serverStop());
                binding.serverBtn.setText(R.string.to_stop_server);
                port = server.getPort();
                clientStart(new URI(url));
                binding.clientBtn.setEnabled(false);
            } catch (Exception e) {
                server.onError(null, new Exception("QR code can't generate or uri string is bad"));
                serverStop();
                printError(e);
            }
        }));

        server.onCloseServerListeners.add(new ListenerCb(this, (o) -> {
            binding.clientBtn.setEnabled(true);
            binding.serverBtn.setOnClickListener((v) -> serverStart());
            binding.serverBtn.setText(R.string.to_start_server);
            binding.qrCode.setImageBitmap(null);
        }));

      /*  LifecycleOwner owner = this;
        server.isStarted.observe(owner, aBoolean -> {
            if (aBoolean) {
                String url = createWsAddress(ip, port);
                try {
                    binding.qrCode.setImageBitmap(QR.generateQrCode(url));
                    binding.serverBtn.setOnClickListener((v) -> serverStop());
                    binding.serverBtn.setText(R.string.to_stop_server);
                    port = server.getPort();
                    clientStart(new URI(url));
                    binding.clientBtn.setEnabled(false);
                } catch (Exception e) {
                    server.onError(null, new Exception("QR code can't generate or uri string is bad"));
                    serverStop();
                    printError(e);
                }
            } else {
                binding.clientBtn.setEnabled(true);
                binding.serverBtn.setOnClickListener((v) -> serverStart());
                binding.serverBtn.setText(R.string.to_start_server);
                binding.qrCode.setImageBitmap(null);
            }
        });*/
    }

    boolean isServer() {
        return server.isStarted;
    }

    //-------------------------------------------------CLIENT

    public void clientStart(URI uri) {
        new Thread(() -> {
            client = new Client(uri);
            client.setTcpNoDelay(true);
            client.setReuseAddr(true);
            runOnUiThread(this::clientSubs);
            try {
                client.connectBlocking();
            } catch (Exception e) {
                printError(e);
            }
        }).start();
    }

    void clientDisconnect() {
        if (client != null && client.isOpen()) {
            client.close();
        }
    }

    void clientSubs() {
        // LifecycleOwner owner = this;
        client.onOpenListeners.add(new ListenerCb(this, (o) -> {
            getTimeOffset();
            binding.clientBtn.setOnClickListener((v) -> clientDisconnect());
            binding.clientBtn.setText(R.string.to_disconnect_client);
            if (!isServer()) {
                binding.serverBtn.setEnabled(false);
            }
        }));
        client.onCloseListeners.add(new ListenerCb(this, (o) -> {
            binding.clientBtn.setOnClickListener((v) -> initConnectionToServer());
            binding.clientBtn.setText(R.string.to_connect_client);
            binding.serverBtn.setEnabled(true);
        }));
    /*    client.isConnected.observe(owner, aBoolean -> {
            if (aBoolean) {
                getTimeOffset();
                binding.clientBtn.setOnClickListener((v) -> clientDisconnect());
                binding.clientBtn.setText(R.string.to_disconnect_client);
                if (!isServer()) {
                    binding.serverBtn.setEnabled(false);
                }
            } else {
                binding.clientBtn.setOnClickListener((v) -> initConnectionToServer());
                binding.clientBtn.setText(R.string.to_connect_client);
                binding.serverBtn.setEnabled(true);
            }
        });*/

/*
        client.error.observe(owner, e -> toast(e.getMessage()));
*/
        client.onMessageListeners.add(new ListenerCb(this, (o) -> {
            try {
                String message = (String) o;
                ServerMessage serverMessage = gson.fromJson(message, ServerMessage.class);
                MetronomeState serverState = serverMessage.clientMessage.state;
             /*   if (serverMessage.clientMessage.bpm != metronome.getBPM()) {
                    String bpm = String.valueOf(serverMessage.clientMessage.bpm);
                    metronome.setBPM(serverMessage.clientMessage.bpm);
                    binding.bpm.setValue(metronome.getBPM());
                    binding.bpmText.setText(bpm);
                    return;
                }*/
                if (serverState == MetronomeState.PLAYING) {
                    long beforeStartNextTick = calculateDelayBeforeStart(serverMessage.t2);

                    metronome.start(beforeStartNextTick);
                    binding.metronomeBtn.setText(R.string.to_stop_metronome);
                } else if (serverState == MetronomeState.STOPPED) {
                    metronome.stop();
                    binding.metronomeBtn.setText(R.string.to_start_metronome);
                }
            } catch (Exception e) {
                printError(e);
            }
        }));
      /*  client.serverMessage.observe(owner, s -> {
            try {
                ServerMessage serverMessage = gson.fromJson(s, ServerMessage.class);
                MetronomeState serverState = serverMessage.clientMessage.state;
             *//*   if (serverMessage.clientMessage.bpm != metronome.getBPM()) {
                    String bpm = String.valueOf(serverMessage.clientMessage.bpm);
                    metronome.setBPM(serverMessage.clientMessage.bpm);
                    binding.bpm.setValue(metronome.getBPM());
                    binding.bpmText.setText(bpm);
                    return;
                }*//*
                if (serverState == MetronomeState.PLAYING) {
                    long beforeStartNextTick = calculateDelayBeforeStart(serverMessage.t2);

                    metronome.start(beforeStartNextTick);
                    binding.metronomeBtn.setText(R.string.to_stop_metronome);
                } else if (serverState == MetronomeState.STOPPED) {
                    metronome.stop();
                    binding.metronomeBtn.setText(R.string.to_start_metronome);
                }
            } catch (Exception e) {
                printError(e);
            }
        });*/
    }

    void getTimeOffset() {
        if (isServer()) return;
        new Thread(() -> {
            try {
                ntp.exec(Const.DEBUG_IP_SERVER + "");
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
        ScanOptions options = new ScanOptions();
        options.setOrientationLocked(false);
        barcodeLauncher.launch(options);
    }

    //--------------------------------------------------UI
    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show());
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
                ClientMessage m = new ClientMessage(metronome.getCurrentState(), bpmInt);
                client.send(gson.toJson(m));
            }
        });
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

 /*   void test(long unixSeconds, String pref) {
        Date date = new java.util.Date(unixSeconds);
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss.SSS z", Locale.getDefault());
        sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
        String formattedDate = sdf.format(date);
        Log.i(pref, formattedDate);
        Log.i("TEST", getOffset() + "");
    }*/

    long calculateDelayBeforeStart(long timestampServerStart) {
        long offset = getOffset();
        //test(timestampServerStart, "SERVER LOCAL TIME");
        long currentMs = System.currentTimeMillis() + offset;
        //test(currentMs, "CURRENT LOCAL TIME");
        long delayMs = currentMs - timestampServerStart;
        long deltaMs = metronome.getIntervalMS();
        long beforeStartNextTick = deltaMs - delayMs;

        if (beforeStartNextTick < 0) {
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