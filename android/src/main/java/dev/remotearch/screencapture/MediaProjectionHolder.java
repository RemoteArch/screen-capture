package dev.remotearch.screencapture;

import android.media.projection.MediaProjection;

public class MediaProjectionHolder {
    private static MediaProjection projection;

    public static void setMediaProjection(MediaProjection mp) {
        projection = mp;
    }

    public static MediaProjection getMediaProjection() {
        return projection;
    }
}
