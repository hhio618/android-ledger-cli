package net.ilammy.ledger.screenshot;

import android.graphics.Bitmap;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * TCP client for sending screenshot data over network.
 * Sends bitmap images as PNG data with a simple protocol:
 * - 4 bytes: data length (int)
 * - N bytes: PNG image data
 */
public class TcpClient {
    
    private static final String TAG = "TcpClient";
    private Socket socket;
    
    /**
     * Send a bitmap over TCP connection.
     * 
     * @param bitmap The bitmap to send
     * @param host Server hostname or IP address
     * @param port Server port number
     * @return true if successful, false otherwise
     */
    public boolean sendBitmap(Bitmap bitmap, String host, int port) {
        try {
            // Convert bitmap to PNG bytes
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            byte[] imageData = byteStream.toByteArray();
            
            Log.d(TAG, "Connecting to " + host + ":" + port);
            
            // Connect to server
            socket = new Socket(host, port);
            socket.setSoTimeout(10000); // 10 second timeout
            
            Log.d(TAG, "Connected. Sending " + imageData.length + " bytes");
            
            // Send data length followed by image data
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeInt(imageData.length);
            out.write(imageData);
            out.flush();
            
            Log.d(TAG, "Screenshot sent successfully");
            
            socket.close();
            return true;
            
        } catch (IOException e) {
            Log.e(TAG, "Failed to send screenshot: " + e.getMessage(), e);
            return false;
        } finally {
            close();
        }
    }
    
    /**
     * Close the TCP connection.
     */
    public void close() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket: " + e.getMessage());
            }
        }
    }
}
