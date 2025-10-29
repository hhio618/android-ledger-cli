#!/usr/bin/env python3
"""
Simple TCP server for receiving screenshots from Android app.
Usage: python3 tcp_server_example.py [port]
"""

import socket
import struct
import sys
from datetime import datetime

def receive_screenshot(host='0.0.0.0', port=8888):
    """
    Start a TCP server that receives screenshots from the Android app.
    
    Protocol:
    1. Receive 4 bytes: image length (big-endian integer)
    2. Receive N bytes: PNG image data
    """
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    
    try:
        server.bind((host, port))
        server.listen(5)
        print(f"📱 Screenshot Server Started")
        print(f"🌐 Listening on {host}:{port}")
        print(f"⏳ Waiting for connections...\n")
        
        screenshot_count = 0
        
        while True:
            client, addr = server.accept()
            print(f"✅ Connection from {addr[0]}:{addr[1]}")
            
            try:
                # Read 4-byte length header
                length_data = b''
                while len(length_data) < 4:
                    chunk = client.recv(4 - len(length_data))
                    if not chunk:
                        raise Exception("Connection closed while reading length")
                    length_data += chunk
                
                length = struct.unpack('>I', length_data)[0]
                print(f"📊 Receiving {length:,} bytes ({length / 1024:.2f} KB)")
                
                # Read image data
                data = b''
                while len(data) < length:
                    remaining = length - len(data)
                    chunk = client.recv(min(remaining, 65536))  # 64KB chunks
                    if not chunk:
                        raise Exception("Connection closed while reading data")
                    data += chunk
                    
                    # Progress indicator
                    progress = (len(data) / length) * 100
                    print(f"⬇️  Progress: {progress:.1f}% ({len(data):,}/{length:,} bytes)", end='\r')
                
                print()  # New line after progress
                
                # Save screenshot with timestamp
                screenshot_count += 1
                timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
                filename = f"screenshot_{timestamp}_{screenshot_count}.png"
                
                with open(filename, 'wb') as f:
                    f.write(data)
                
                print(f"💾 Screenshot saved: {filename}")
                print(f"✨ Total screenshots received: {screenshot_count}\n")
            
            except Exception as e:
                print(f"❌ Error: {e}\n")
            finally:
                client.close()
    
    except KeyboardInterrupt:
        print("\n\n👋 Server stopped by user")
    except Exception as e:
        print(f"❌ Server error: {e}")
    finally:
        server.close()
        print("🔴 Server closed")

if __name__ == '__main__':
    port = 8888
    
    # Allow custom port from command line
    if len(sys.argv) > 1:
        try:
            port = int(sys.argv[1])
        except ValueError:
            print(f"Invalid port: {sys.argv[1]}")
            print(f"Usage: {sys.argv[0]} [port]")
            sys.exit(1)
    
    receive_screenshot(port=port)
