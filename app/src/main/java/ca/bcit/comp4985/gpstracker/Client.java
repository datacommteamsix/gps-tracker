/*---------------------------------------------------------------------------------------
--	SOURCE FILE:		Client.java
--
--	PROGRAM:		gpsTracker
--
--	FUNCTIONS:		void *client_recv(void *ptr);
--					int client_send();
--					void sendPacket(char * sendbuf);
--					void intHandler(int sigint);
--					void endProgram();
--					static void SystemFatal(const char* message);
--
--	DATE:			April 04, 2018
--
--	REVISIONS:		NONE
--
--
--	DESIGNERS:		Angus Lam, Benny Wang, Roger Zhang
--
--
--	PROGRAMMER:		Benny Wang, Roger Zhang
--
--	NOTES:
--	This is the client class, it contains methods to update changes and send data to the
--  server. it contains the established socket information to the server.
---------------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    Client
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Benny Wang, Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   Client(String deviceId, String ipAddress, int port)
    --
    -- RETURNS: void
    --
    -- NOTES: This is the constructor of the class. Server's ipaddress and port are
    -- being passed to create a socket.
    ------------------------------------------------------------------------------*/
    Client(String deviceId, String ipAddress, int port) {
        this.deviceId = deviceId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.socket = null;
        this.outStream = null;
    }

    /*------------------------------------------------------------------------------
    -- FUNCTION:    connectToServer
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   private void connectToServer()
    --
    -- RETURNS: void
    --
    -- NOTES: This creates a tcp socket to the server with specified ip and port.
    -- then store the information using outputStream.
    ------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    sendData
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   private void sendData(byte[] data)
    --
    -- RETURNS: void
    --
    -- NOTES: Sends out the data as byte array. writes it to the outputstream defined
    -- in the connection.
    ------------------------------------------------------------------------------*/
    private void sendData(byte[] data) {
        if (outStream != null) {
            try {
                outStream.write(data, 0, data.length);
            } catch (IOException e) {
                Log.w("error", e.toString());
            }
        }
    }

    /*------------------------------------------------------------------------------
    -- FUNCTION:    setLocation
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Benny Wang, Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang, Benny Wang
    --
    -- INTERFACE:   setLocation(Location location)
    --
    -- RETURNS: void
    --
    -- NOTES: Sets the location and timestamp to send to the server.
    ------------------------------------------------------------------------------*/
    void setLocation(Location location) {
        this.lat = location.getLatitude();
        this.lng = location.getLongitude();
        this.timestamp = System.currentTimeMillis();
    }

    /*------------------------------------------------------------------------------
    -- FUNCTION:    sendLocation()
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Angus Lam, Benny Wang, Roger Zhang
    --
    -- PROGRAMMER:  Benny Wang, Roger Zhang
    --
    -- INTERFACE:   sendLocation()
    --
    -- RETURNS: void
    --
    -- NOTES: Sends the location to the server. Adds in a delimiter for the server
    -- to separate each fields.
    ------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    isConnected()
    --
    -- DATE:        April 4th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Angus Lam, Benny Wang, Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   isConnected()
    --
    -- RETURNS: bool
    --				true  if connected
    --              false if not connected
    --
    -- NOTES: This function detects if the socket is connected or not. If its been
    -- closed. Then false will be returned.
    ------------------------------------------------------------------------------*/
    boolean isConnected() {
        return this.socket != null && !socket.isClosed();
    }
}
