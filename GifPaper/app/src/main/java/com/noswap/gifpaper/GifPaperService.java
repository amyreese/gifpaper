package com.noswap.gifpaper;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.os.Handler;
import android.os.SystemClock;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by jreese on 7/12/2015.
 */
public class GifPaperService extends WallpaperService {
    static final String TAG = "gifService";
    static final Handler gifHandler = new Handler();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public Engine onCreateEngine() {
        try {
            return new GifEngine();
        } catch (IOException e) {
            Log.w(TAG, "Error creating engine", e);
            stopSelf();
            return null;
        }
    }

    class GifEngine extends Engine {
        private final Movie gif;
        private final int duration;
        private final Runnable runnable;

        float scaleX;
        float scaleY;
        int when;
        long start;

        GifEngine() throws IOException {
            InputStream is = getResources().openRawResource(R.raw.whoa);

            if (is == null) {
                throw new IOException("Unable to open whoa.gif");
            }

            try {
                gif = Movie.decodeStream(is);
                duration = gif.duration();
            } finally {
                is.close();
            }

            when = -1;
            runnable = new Runnable() {
                @Override
                public void run() {
                    animateGif();
                }
            };
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            gifHandler.removeCallbacks(runnable);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                animateGif();
            } else {
                gifHandler.removeCallbacks(runnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            scaleX = width / (1f * gif.width());
            scaleY = height / (1f * gif.height());
            animateGif();
        }

        @Override
        public void onOffsetsChanged(float xOffset, float yOffset,
                                    float xOffsetStep, float yOffsetStep,
                                    int xPixelOffset, int yPixelOffset) {
            super.onOffsetsChanged(
                    xOffset, yOffset,
                    xOffsetStep, yOffsetStep,
                    xPixelOffset, yPixelOffset);
            animateGif();
        }

        void animateGif() {
            tick();

            SurfaceHolder surfaceHolder = getSurfaceHolder();
            Canvas canvas = null;

            try {
                canvas = surfaceHolder.lockCanvas();

                if (canvas != null) {
                    gifCanvas(canvas);
                }
            } finally {
                if (canvas != null) {
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

            gifHandler.removeCallbacks(runnable);

            if (isVisible()) {
                gifHandler.postDelayed(runnable, 1000L/25L);
            }
        }

        void tick() {
            if (when == -1L) {
                when = 0;
                start = SystemClock.uptimeMillis();
            } else {
                long diff = SystemClock.uptimeMillis() - start;
                when = (int) (diff % duration);
            }

        }

        void gifCanvas(Canvas canvas) {
            canvas.save();
            canvas.scale(scaleX, scaleY);
            gif.setTime(when);
            gif.draw(canvas, 0, 0);
            canvas.restore();
        }
    }
}
