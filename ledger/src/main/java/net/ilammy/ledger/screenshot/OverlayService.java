package net.ilammy.ledger.screenshot;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * Service that displays an overlay button for capturing screenshots.
 * The button can be moved around the screen and when clicked, captures
 * a screenshot and sends it over TCP.
 */
public class OverlayService extends Service {
    
    private WindowManager windowManager;
    private View overlayView;
    private ScreenshotCapture screenshotCapture;
    private TcpClient tcpClient;
    
    // TCP connection parameters (can be passed via intent extras)
    private String serverHost = "192.168.1.100";
    private int serverPort = 8888;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        screenshotCapture = new ScreenshotCapture(this);
        tcpClient = new TcpClient();
        
        createOverlayView();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Get TCP server details from intent if provided
        if (intent != null) {
            serverHost = intent.getStringExtra("server_host");
            if (serverHost == null) serverHost = "192.168.1.100";
            serverPort = intent.getIntExtra("server_port", 8888);
        }
        
        return START_STICKY;
    }
    
    private void createOverlayView() {
        // Create overlay layout parameters
        int layoutType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutType = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutType = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 100;
        
        // Create the overlay view
        FrameLayout layout = new FrameLayout(this);
        Button button = new Button(this);
        button.setText("📷");
        button.setTextSize(24);
        
        // Make button draggable
        button.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return false;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(overlayView, params);
                        return false;
                }
                return false;
            }
        });
        
        // Handle screenshot capture on click
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                captureAndSendScreenshot();
            }
        });
        
        layout.addView(button);
        overlayView = layout;
        
        // Add view to window
        windowManager.addView(overlayView, params);
    }
    
    private void captureAndSendScreenshot() {
        Toast.makeText(this, "Capturing screenshot...", Toast.LENGTH_SHORT).show();
        
        screenshotCapture.captureScreenshot(new ScreenshotCapture.ScreenshotCallback() {
            @Override
            public void onScreenshotCaptured(Bitmap bitmap) {
                // Send screenshot over TCP in background thread
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean success = tcpClient.sendBitmap(bitmap, serverHost, serverPort);
                        
                        // Show result on UI thread
                        overlayView.post(new Runnable() {
                            @Override
                            public void run() {
                                String message = success ? 
                                    "Screenshot sent successfully!" : 
                                    "Failed to send screenshot";
                                Toast.makeText(OverlayService.this, message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
            
            @Override
            public void onScreenshotFailed(String error) {
                Toast.makeText(OverlayService.this, "Capture failed: " + error, 
                    Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        tcpClient.close();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
