package dev.remotearch.screencapture;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ScreenCapture")
public class ScreenCapturePlugin extends Plugin {

    private static final int REQUEST_MEDIA_PROJECTION = 1001;
    private MediaProjectionManager projectionManager;
    private PluginCall savedCall;
    private String serverUrl;

    @Override
    public void load() {
        projectionManager = (MediaProjectionManager)
            getActivity().getSystemService(Activity.MEDIA_PROJECTION_SERVICE);
    }

    @PluginMethod
    public void start(PluginCall call) {
        serverUrl = call.getString("url");
        if (serverUrl == null || serverUrl.isEmpty()) {
            call.reject("Missing 'url' parameter");
            return;
        }

        savedCall = call;

        Intent permissionIntent = projectionManager.createScreenCaptureIntent();
        startActivityForResult(call, permissionIntent, REQUEST_MEDIA_PROJECTION);
    }

    @PluginMethod
    public void stop(PluginCall call) {
        try {
            Context context = getContext();
            Intent stopIntent = new Intent(context, ScreenCaptureService.class);
            context.stopService(stopIntent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Erreur lors de l'arrêt du service : " + e.getMessage());
        }
    }


    @Override
    protected void handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        super.handleOnActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                // Obtenir MediaProjection
                MediaProjection mediaProjection = projectionManager.getMediaProjection(resultCode, data);
                MediaProjectionHolder.setMediaProjection(mediaProjection);

                // Lancer le service foreground
                Context context = getContext();
                Intent serviceIntent = new Intent(context, ScreenCaptureService.class);
                serviceIntent.putExtra("url", serverUrl);
                ContextCompat.startForegroundService(context, serviceIntent);

                savedCall.resolve();
            } else {
                savedCall.reject("Permission denied for screen capture");
            }
        }
    }
}
