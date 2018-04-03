package ca.bcit.comp4985.gpstracker;

import android.location.Location;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;

class Client {
    private Socket socket;
    private DataOutputStream outStream;
    private String ipAddress;
    private int port;
    private String deviceId;

    private double lat;
    private double lng;
    private long timestamp;

    Client(String deviceId, String ipAddress, int port) {
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = null;
        this.outStream = null;
    }

    private void connectToServer() {
        try {
            if (ipAddress != null && port != -1) {
                try {
                    socket = new Socket(ipAddress, port);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (socket != null) {
                    outStream = new DataOutputStream(socket.getOutputStream());
                }
            }
        } catch (Exception e) {
            Log.w("error", e.toString());
        }
    }

    private void sendData(byte[] data) {
        if (outStream != null) {
            try {
                outStream.write(data, 0, data.length);
            } catch (IOException e) {
                Log.w("error", e.toString());
            }
        }
    }

    void setLocation(Location location) {
        this.lat = location.getLatitude();
        this.lng = location.getLongitude();
        this.timestamp = System.currentTimeMillis();
    }

    void sendLocation() {
        try {
            if (socket == null) {
                connectToServer();
                Log.w("Roger", "Reconnect to server");
            }

            ByteBuffer sendBuffer = ByteBuffer.allocate(59);

            final byte delimiter = ' ';

            sendBuffer.put(this.deviceId.getBytes());
            sendBuffer.put(delimiter);

            sendBuffer.putLong(this.timestamp);
            sendBuffer.put(delimiter);

            sendBuffer.putDouble(this.lat);
            sendBuffer.put(delimiter);

            sendBuffer.putDouble(this.lng);

            byte[] sendBuf = sendBuffer.array();

            sendData(sendBuf);
        } catch (Exception e) {
            Log.d("benny", e.getMessage());
            e.printStackTrace();
        }
    }

    boolean isConnected() {
        return this.socket != null && !socket.isClosed();
    }
}
