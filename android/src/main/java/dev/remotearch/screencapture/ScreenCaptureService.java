package dev.remotearch.screencapture;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.view.Surface;

import androidx.core.app.NotificationCompat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";

    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private HandlerThread captureThread;
    private Handler handler;
    private Surface surface;
    private OutputStream streamOut;
    private boolean isRunning = true;
    private String serverUrl;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        serverUrl = intent.getStringExtra("url");
        mediaProjection = MediaProjectionHolder.getMediaProjection();

        if (serverUrl == null || mediaProjection == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        startForeground(1, createNotification());
        startCapture();
        return START_STICKY;
    }

    private void startCapture() {
        captureThread = new HandlerThread("ScreenCaptureThread");
        captureThread.start();
        handler = new Handler(captureThread.getLooper());

        int width = 960;
        int height = 540;

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        surface = imageReader.getSurface();

        mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height,
            getResources().getDisplayMetrics().densityDpi,
            0, surface, null, handler
        );

        new Thread(this::initStream).start();
    }

    private void initStream() {
        try {
            URL url = new URL(serverUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.connect();

            streamOut = new BufferedOutputStream(conn.getOutputStream());

            handler.post(captureRunnable);  // start capture loop after stream is ready
        } catch (Exception e) {
            Log.e(TAG, "Erreur connexion stream : " + e.getMessage());
            stopSelf();
        }
    }

    private final Runnable captureRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning) return;

            Image image = imageReader.acquireLatestImage();
            if (image != null) {
                Bitmap bmp = imageToBitmap(image);
                image.close();
                if (bmp != null) {
                    writeFrame(bmp);
                }
            }

            handler.postDelayed(this, 200); // 5 fps
        }
    };

    private Bitmap imageToBitmap(Image image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;

            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);

            return Bitmap.createScaledBitmap(bitmap, 960, 540, false);
        } catch (Exception e) {
            Log.e(TAG, "Erreur conversion bitmap : " + e.getMessage());
            return null;
        }
    }

    private void writeFrame(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
            byte[] jpeg = baos.toByteArray();

            int len = jpeg.length;
            if (len > 0xFFFFFF) {
                Log.e(TAG, "Frame trop grande");
                return;
            }

            byte[] header = new byte[3];
            header[0] = (byte) ((len >> 16) & 0xFF);
            header[1] = (byte) ((len >> 8) & 0xFF);
            header[2] = (byte) (len & 0xFF);

            synchronized (this) {
                streamOut.write(header);
                streamOut.write(jpeg);
                streamOut.flush();
            }
        } catch (Exception e) {
            Log.e(TAG, "Erreur stream image : " + e.getMessage());
            stopSelf();
        }
    }

    private Notification createNotification() {
        String channelId = "screen_cap_channel";
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(channelId, "Capture écran", NotificationManager.IMPORTANCE_LOW);
            manager.createNotificationChannel(chan);
        }

        return new NotificationCompat.Builder(this, channelId)
            .setContentTitle("Capture en cours")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setOngoing(true)
            .build();
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (captureThread != null) captureThread.quitSafely();
        if (imageReader != null) imageReader.close();
        if (mediaProjection != null) mediaProjection.stop();
        try {
            if (streamOut != null) streamOut.close();
        } catch (IOException e) {
            Log.e(TAG, "Erreur fermeture stream : " + e.getMessage());
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
