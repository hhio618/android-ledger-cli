package net.ilammy.ledger.screenshot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

/**
 * Utility class for capturing screenshots using MediaProjection API.
 * Requires MEDIA_PROJECTION permission which must be obtained through
 * an Activity result.
 */
public class ScreenshotCapture {
    
    private Context context;
    private MediaProjectionManager projectionManager;
    private MediaProjection mediaProjection;
    private static Intent mediaProjectionIntent;
    private static int mediaProjectionResultCode;
    
    public interface ScreenshotCallback {
        void onScreenshotCaptured(Bitmap bitmap);
        void onScreenshotFailed(String error);
    }
    
    public ScreenshotCapture(Context context) {
        this.context = context;
        this.projectionManager = (MediaProjectionManager) 
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }
    
    /**
     * Store the media projection permission result.
     * This should be called from an Activity's onActivityResult.
     */
    public static void setMediaProjectionData(int resultCode, Intent data) {
        mediaProjectionResultCode = resultCode;
        mediaProjectionIntent = data;
    }
    
    /**
     * Check if we have media projection permission.
     */
    public static boolean hasPermission() {
        return mediaProjectionIntent != null && mediaProjectionResultCode == Activity.RESULT_OK;
    }
    
    /**
     * Capture a screenshot.
     */
    public void captureScreenshot(final ScreenshotCallback callback) {
        if (!hasPermission()) {
            callback.onScreenshotFailed("Media projection permission not granted");
            return;
        }
        
        // Get screen dimensions
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        
        final int width = metrics.widthPixels;
        final int height = metrics.heightPixels;
        final int density = metrics.densityDpi;
        
        // Create ImageReader
        final ImageReader imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        );
        
        // Create MediaProjection
        mediaProjection = projectionManager.getMediaProjection(
            mediaProjectionResultCode, mediaProjectionIntent
        );
        
        // Create VirtualDisplay
        VirtualDisplay virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null, null
        );
        
        // Wait a bit for the virtual display to stabilize
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Image image = null;
                try {
                    image = imageReader.acquireLatestImage();
                    
                    if (image != null) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        int pixelStride = planes[0].getPixelStride();
                        int rowStride = planes[0].getRowStride();
                        int rowPadding = rowStride - pixelStride * width;
                        
                        // Create bitmap
                        Bitmap bitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        );
                        bitmap.copyPixelsFromBuffer(buffer);
                        
                        // Crop if there's padding
                        if (rowPadding != 0) {
                            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
                        }
                        
                        callback.onScreenshotCaptured(bitmap);
                    } else {
                        callback.onScreenshotFailed("Failed to acquire image");
                    }
                } catch (Exception e) {
                    callback.onScreenshotFailed("Exception: " + e.getMessage());
                } finally {
                    if (image != null) {
                        image.close();
                    }
                    imageReader.close();
                    virtualDisplay.release();
                    if (mediaProjection != null) {
                        mediaProjection.stop();
                    }
                }
            }
        }, 100);
    }
}
