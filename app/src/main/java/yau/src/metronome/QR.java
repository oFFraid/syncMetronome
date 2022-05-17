package yau.src.metronome;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.RemoteException;
import android.service.carrier.CarrierMessagingService;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.journeyapps.barcodescanner.ScanIntentResult;

import java.net.URI;
import java.net.URISyntaxException;

import yau.src.metronome.utils.Listener;

public class QR implements ActivityResultCallback<ScanIntentResult> {
    Context context;
    CarrierMessagingService.ResultCallback<URI> callback;
    Listener cbNotQR;

    public QR(Context ctx, CarrierMessagingService.ResultCallback<URI> callback, Listener cbNotQR) {
        context = ctx;
        this.callback = callback;
        this.cbNotQR = cbNotQR;
    }

    public static Bitmap generateQrCode(String text) throws WriterException {
        return new BarcodeEncoder().encodeBitmap(text, BarcodeFormat.QR_CODE, 500, 500);
    }

    @Override
    public void onActivityResult(ScanIntentResult result) {
        if (result.getContents() == null) {
            Intent originalIntent = result.getOriginalIntent();
            if (originalIntent == null) {
                cbNotQR.submit(null);
                Toast.makeText(context, "Отменено", Toast.LENGTH_LONG).show();
            } else if (originalIntent.hasExtra(Intents.Scan.MISSING_CAMERA_PERMISSION)) {
                cbNotQR.submit(null);
                Toast.makeText(context, "Отменено из-за отсутствия разрешения камеры", Toast.LENGTH_LONG).show();
            }
        } else {
            String decodedQr = result.getContents();
            try {
                URI uri = new URI(decodedQr);
                callback.onReceiveResult(uri);
            } catch (URISyntaxException | RemoteException e) {
                cbNotQR.submit(null);
                Toast.makeText(context, "Ошибка чтения QR!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}