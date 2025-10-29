# Android Screenshot Overlay Implementation Guide

This project now includes a complete implementation for capturing screenshots via an overlay button and sending them over TCP.

## 🎯 Features

- **Floating Overlay Button**: A draggable camera button that stays on top of all apps
- **Screen Capture**: Uses MediaProjection API to capture high-quality screenshots
- **TCP Transfer**: Sends captured screenshots to a TCP server
- **Permission Management**: User-friendly permission request flow

## 📁 Files Created

### Core Components

1. **MainActivity.java** (`ledger/src/main/java/net/ilammy/ledger/screenshot/MainActivity.java`)
   - Entry point for the app
   - Handles permission requests (overlay and screen capture)
   - Allows user to configure TCP server host and port
   - Starts/stops the overlay service

2. **OverlayService.java** (`ledger/src/main/java/net/ilammy/ledger/screenshot/OverlayService.java`)
   - Background service that displays the floating camera button
   - Button is draggable and can be positioned anywhere on screen
   - Captures screenshot when button is clicked
   - Sends screenshot to TCP server

3. **ScreenshotCapture.java** (`ledger/src/main/java/net/ilammy/ledger/screenshot/ScreenshotCapture.java`)
   - Utility class for capturing screenshots
   - Uses MediaProjection API (Android 5.0+)
   - Converts screen content to Bitmap
   - Handles permission state

4. **TcpClient.java** (`ledger/src/main/java/net/ilammy/ledger/screenshot/TcpClient.java`)
   - TCP client for network communication
   - Compresses bitmap to PNG format
   - Sends data with simple protocol: [4-byte length][PNG data]
   - Handles connection errors gracefully

## 🔧 How to Use

### 1. Setup TCP Server

First, you need a TCP server to receive the screenshots. Here's a simple Python server example:

```python
import socket
import struct

def receive_screenshot(host='0.0.0.0', port=8888):
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((host, port))
    server.listen(1)
    print(f"Server listening on {host}:{port}")
    
    while True:
        client, addr = server.accept()
        print(f"Connection from {addr}")
        
        try:
            # Read 4-byte length
            length_data = client.recv(4)
            if len(length_data) == 4:
                length = struct.unpack('>I', length_data)[0]
                print(f"Receiving {length} bytes")
                
                # Read image data
                data = b''
                while len(data) < length:
                    chunk = client.recv(min(length - len(data), 4096))
                    if not chunk:
                        break
                    data += chunk
                
                # Save screenshot
                filename = f"screenshot_{addr[1]}.png"
                with open(filename, 'wb') as f:
                    f.write(data)
                print(f"Screenshot saved as {filename}")
        
        except Exception as e:
            print(f"Error: {e}")
        finally:
            client.close()

if __name__ == '__main__':
    receive_screenshot()
```

Run the server:
```bash
python3 server.py
```

### 2. Build and Install the App

```bash
# Build the APK
./gradlew assembleDebug

# Install on device
adb install ledger/build/outputs/apk/debug/ledger-debug.apk
```

### 3. Configure and Run

1. **Launch the app** - Open "Screenshot Overlay" from your app drawer
2. **Grant permissions** - Click "Request Permissions"
   - Allow overlay permission
   - Allow screen capture permission
3. **Configure server** - Enter your server's IP address and port
   - Default: 192.168.1.100:8888
   - Make sure your device can reach the server
4. **Start service** - Click "Start Overlay Service"
5. **Use the overlay** - A camera button (📷) will appear on screen
   - Drag it anywhere you want
   - Click it to capture and send screenshot
   - Toast notifications show success/failure

### 4. Stop the Service

To stop the overlay:
- Reopen the app
- Click "Stop Overlay Service"

Or from system settings:
- Settings → Apps → Screenshot Overlay → Force Stop

## 🔐 Permissions Required

The app requires these permissions:

- **SYSTEM_ALERT_WINDOW**: Draw overlay on top of other apps
- **INTERNET**: Send data over network
- **ACCESS_NETWORK_STATE**: Check network connectivity
- **FOREGROUND_SERVICE**: Run background service (Android 8.0+)
- **MediaProjection**: Capture screen content (requested at runtime)

## 🏗️ Architecture

```
MainActivity
    ↓ (starts)
OverlayService
    ↓ (uses)
    ├── ScreenshotCapture → MediaProjection API
    └── TcpClient → Network Socket
```

## 📱 Compatibility

- **Minimum SDK**: 21 (Android 5.0 Lollipop)
- **Target SDK**: 28 (Android 9.0 Pie)
- **Features**:
  - Screen capture requires Android 5.0+
  - Overlay permissions flow varies by Android version
  - Foreground service required for Android 8.0+

## 🔍 Troubleshooting

### Overlay button doesn't appear
- Check if overlay permission is granted
- Go to Settings → Apps → Screenshot Overlay → Display over other apps

### Screenshot capture fails
- Ensure MediaProjection permission was granted
- Restart the app and grant permission again
- Some system screens (like permission dialogs) cannot be captured

### TCP connection fails
- Verify server is running and accessible
- Check firewall settings
- Ensure device and server are on same network
- Try pinging the server IP from your device

### App crashes on Android 8.0+
- Foreground service requirements - the service needs notification
- Consider adding notification channel for the service

## 🔧 Customization

### Change overlay button appearance
Edit `OverlayService.java`:
```java
button.setText("📷");  // Change emoji or text
button.setTextSize(24);  // Change size
```

### Change image format or quality
Edit `TcpClient.java`:
```java
// PNG with 100% quality (default)
bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);

// Or use JPEG with compression
bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteStream);
```

### Add notification for foreground service
For Android 8.0+ compliance, add notification in `OverlayService.onCreate()`:
```java
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    NotificationChannel channel = new NotificationChannel(
        "overlay_service", "Screenshot Service",
        NotificationManager.IMPORTANCE_LOW
    );
    NotificationManager manager = getSystemService(NotificationManager.class);
    manager.createNotificationChannel(channel);
    
    Notification notification = new Notification.Builder(this, "overlay_service")
        .setContentTitle("Screenshot Overlay Active")
        .setSmallIcon(android.R.drawable.ic_menu_camera)
        .build();
    
    startForeground(1, notification);
}
```

## 🚀 Advanced Features (Ideas)

- Add encryption for secure transmission
- Support multiple image formats
- Implement retry mechanism for failed transfers
- Add screenshot history/gallery
- Support video recording
- Add authentication to TCP protocol
- Implement WebSocket for real-time streaming
- Add preview before sending
- Support multiple server endpoints

## 📝 Protocol Specification

### TCP Message Format

```
+------------------+------------------+
| Length (4 bytes) | PNG Data (N bytes) |
+------------------+------------------+
```

- **Length**: 32-bit big-endian integer
- **PNG Data**: Raw PNG file data

### Example Implementation (Node.js Server)

```javascript
const net = require('net');
const fs = require('fs');

const server = net.createServer((socket) => {
    console.log('Client connected');
    
    let lengthBuffer = Buffer.alloc(0);
    let imageBuffer = Buffer.alloc(0);
    let expectedLength = null;
    
    socket.on('data', (data) => {
        if (expectedLength === null) {
            lengthBuffer = Buffer.concat([lengthBuffer, data]);
            if (lengthBuffer.length >= 4) {
                expectedLength = lengthBuffer.readInt32BE(0);
                imageBuffer = lengthBuffer.slice(4);
                lengthBuffer = Buffer.alloc(0);
            }
        } else {
            imageBuffer = Buffer.concat([imageBuffer, data]);
        }
        
        if (expectedLength !== null && imageBuffer.length >= expectedLength) {
            const filename = `screenshot_${Date.now()}.png`;
            fs.writeFileSync(filename, imageBuffer.slice(0, expectedLength));
            console.log(`Saved ${filename}`);
            
            // Reset for next image
            expectedLength = null;
            imageBuffer = imageBuffer.slice(expectedLength);
        }
    });
});

server.listen(8888, '0.0.0.0', () => {
    console.log('Server listening on port 8888');
});
```

## 📄 License

This implementation follows the same license as the main project.
