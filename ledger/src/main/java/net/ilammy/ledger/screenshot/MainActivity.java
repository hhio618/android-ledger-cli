package net.ilammy.ledger.screenshot;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Main activity for requesting permissions and starting the overlay service.
 * This activity requests:
 * 1. SYSTEM_ALERT_WINDOW permission for overlay
 * 2. MediaProjection permission for screen capture
 */
public class MainActivity extends Activity {
    
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final int REQUEST_MEDIA_PROJECTION = 1002;
    
    private EditText serverHostInput;
    private EditText serverPortInput;
    private Button startServiceButton;
    private TextView statusText;
    
    private boolean hasOverlayPermission = false;
    private boolean hasMediaProjectionPermission = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create UI programmatically
        createUI();
        
        // Check permissions
        checkPermissions();
    }
    
    private void createUI() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        
        // Title
        TextView title = new TextView(this);
        title.setText("Screenshot Overlay");
        title.setTextSize(24);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);
        
        // Status text
        statusText = new TextView(this);
        statusText.setText("Checking permissions...");
        statusText.setPadding(0, 0, 0, 20);
        layout.addView(statusText);
        
        // Server host input
        TextView hostLabel = new TextView(this);
        hostLabel.setText("Server Host:");
        layout.addView(hostLabel);
        
        serverHostInput = new EditText(this);
        serverHostInput.setHint("192.168.1.100");
        serverHostInput.setText("192.168.1.100");
        serverHostInput.setPadding(20, 20, 20, 20);
        layout.addView(serverHostInput);
        
        // Server port input
        TextView portLabel = new TextView(this);
        portLabel.setText("Server Port:");
        portLabel.setPadding(0, 20, 0, 0);
        layout.addView(portLabel);
        
        serverPortInput = new EditText(this);
        serverPortInput.setHint("8888");
        serverPortInput.setText("8888");
        serverPortInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        serverPortInput.setPadding(20, 20, 20, 20);
        layout.addView(serverPortInput);
        
        // Request permissions button
        Button requestPermissionsButton = new Button(this);
        requestPermissionsButton.setText("Request Permissions");
        requestPermissionsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestPermissions();
            }
        });
        layout.addView(requestPermissionsButton);
        
        // Start service button
        startServiceButton = new Button(this);
        startServiceButton.setText("Start Overlay Service");
        startServiceButton.setEnabled(false);
        startServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startOverlayService();
            }
        });
        layout.addView(startServiceButton);
        
        // Stop service button
        Button stopServiceButton = new Button(this);
        stopServiceButton.setText("Stop Overlay Service");
        stopServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopOverlayService();
            }
        });
        layout.addView(stopServiceButton);
        
        setContentView(layout);
    }
    
    private void checkPermissions() {
        // Check overlay permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            hasOverlayPermission = Settings.canDrawOverlays(this);
        } else {
            hasOverlayPermission = true;
        }
        
        // Check media projection permission
        hasMediaProjectionPermission = ScreenshotCapture.hasPermission();
        
        updateStatus();
    }
    
    private void requestPermissions() {
        // Request overlay permission
        if (!hasOverlayPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION);
        }
        // Request media projection permission
        else if (!hasMediaProjectionPermission) {
            MediaProjectionManager projectionManager = 
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            startActivityForResult(
                projectionManager.createScreenCaptureIntent(),
                REQUEST_MEDIA_PROJECTION
            );
        } else {
            Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateStatus() {
        StringBuilder status = new StringBuilder();
        status.append("Overlay Permission: ")
            .append(hasOverlayPermission ? "✓ Granted" : "✗ Not Granted")
            .append("\n");
        status.append("Screen Capture Permission: ")
            .append(hasMediaProjectionPermission ? "✓ Granted" : "✗ Not Granted");
        
        statusText.setText(status.toString());
        
        // Enable start button only if all permissions are granted
        startServiceButton.setEnabled(hasOverlayPermission && hasMediaProjectionPermission);
    }
    
    private void startOverlayService() {
        String host = serverHostInput.getText().toString();
        String portStr = serverPortInput.getText().toString();
        
        if (host.isEmpty()) {
            Toast.makeText(this, "Please enter server host", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid port number", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Intent intent = new Intent(this, OverlayService.class);
        intent.putExtra("server_host", host);
        intent.putExtra("server_port", port);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        
        Toast.makeText(this, "Overlay service started", Toast.LENGTH_SHORT).show();
        finish(); // Close activity, overlay will remain
    }
    
    private void stopOverlayService() {
        Intent intent = new Intent(this, OverlayService.class);
        stopService(intent);
        Toast.makeText(this, "Overlay service stopped", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            checkPermissions();
            if (hasOverlayPermission && !hasMediaProjectionPermission) {
                // Auto-request next permission
                requestPermissions();
            }
        } else if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK) {
                ScreenshotCapture.setMediaProjectionData(resultCode, data);
                hasMediaProjectionPermission = true;
                Toast.makeText(this, "Screen capture permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
            }
            checkPermissions();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }
}
