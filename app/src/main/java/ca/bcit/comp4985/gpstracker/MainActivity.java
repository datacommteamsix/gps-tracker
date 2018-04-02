package ca.bcit.comp4985.gpstracker;

import android.Manifest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    String sIP;
    String sPort;
    int port;
    EditText IPEditText;
    EditText PortEditText;
    String deviceID;

    public static final float DISTANCE_RESOLUTION = 2.0f;
    public static final int TIME_RESOLUTION = 3 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocationServices();
        final Button ConnectBtn = (Button) findViewById(R.id.buttonConnect);
        IPEditText = (EditText) findViewById(R.id.IPEdit);
        PortEditText = (EditText) findViewById(R.id.portEdit);

        final LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        final String devicename = (android.os.Build.MANUFACTURER + "-" + android.os.Build.MODEL).replace(' ', '-');
        deviceID = md5(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));

        // May be useful if server require client ip address
        final String deviceip = getWifiAddress();

        ConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w("Roger", "Start connection " + deviceID);
                Connect(lm, devicename);
            }
        });
    }

    public String md5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void initLocationServices() {

        // check if permissions are granted
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // if not, ask for permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            Log.w("Roger", "Permission granted!");
        }
    }

    public String getWifiAddress() {
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String ip = intToIp(ipAddress);
        Log.w("Roger", "myipaddress is: " + ip);
        return ip;
    }

    private String intToIp(int ip) {
        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void Connect(LocationManager lm, String devicename) {

        sIP = IPEditText.getText().toString();
        sPort = PortEditText.getText().toString();
        Log.w("Roger", sIP + ":" + port);
        if (sIP != null && sIP.length() > 0 && sPort != null && sPort.length() > 0) {
            //port = iport;
            //port = 7000;
            //sIP = "10.0.2.1";
            //sIP = "142.232.122.3";
            port = Integer.parseInt(sPort);
            Log.w("Roger", sIP + " " + port);
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // if not, ask for permission
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                        1
                );
                Log.w("Roger", "Permission granted!");
            }
            final mLocationListener ll = new mLocationListener(lm, devicename, sIP, port);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,  TIME_RESOLUTION, DISTANCE_RESOLUTION, ll);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,  TIME_RESOLUTION, DISTANCE_RESOLUTION, ll);
        }
    }

    public class mLocationListener implements LocationListener {
        public long timestamp;
        public double latitude;
        public double longitude;
        public String devicename;
        public String s_ip;
        public int port;
        public Client connection;
        LocationManager lm = null;

        public mLocationListener(LocationManager manager, String dn, String ip, int p) {
            lm = manager;
            timestamp = System.currentTimeMillis() / 1000;
            latitude = -1;
            longitude = -1;
            devicename = dn;
            s_ip = ip;
            port = p;

            connection = new Client(s_ip, port);
            connection.longitude = longitude;
            connection.latitude = latitude;
            connection.timestamp = timestamp;
            connection.devicename = devicename;
        }

        @Override
        public void onLocationChanged(Location location) {
            Toast.makeText(MainActivity.this, timestamp + " data sent", Toast.LENGTH_SHORT).show();

            latitude = location.getLatitude();
            longitude = location.getLongitude();
            timestamp = location.getTime();

            connection.longitude = longitude;
            connection.latitude = latitude;
            connection.timestamp = timestamp;
            connection.devicename = devicename;

            new SendLocationTask().execute(connection);
        }

        //unused
        @Override
        public void onProviderDisabled(String provider) {
        }

        //unused
        @Override
        public void onProviderEnabled(String provider) {
        }

        //unused
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }

    class Client {
        Socket sClient;
        DataOutputStream outStream;
        String ipAddress;
        int port;
        public long timestamp;
        public double latitude;
        public double longitude;
        public String devicename;

        public Client(String s_ip, int p) {
            ipAddress = s_ip;
            port = p;
            sClient = null;
            outStream = null;
        }

        public void connectToServer() {
            try {
                if (ipAddress != null && port != -1) {
                    try {
                        sClient = new Socket(ipAddress, port);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (sClient != null) {
                        outStream = new DataOutputStream(sClient.getOutputStream());
                    }
                }
            } catch (Exception e) {
                Log.w("error", e.toString());
            }
        }

        public boolean sendData(byte[] data) {
            if (outStream != null) {
                try {
                    outStream.write(data, 0, data.length);
                } catch (IOException e) {
                    Log.w("error", e.toString());
                    return false;
                }
            }
            return true;
        }

        public void sendLocation() {
            if (sClient == null) {
                connectToServer();
                Log.w("Roger", "Reconnect to server");
            }

            ByteBuffer sendBuffer = ByteBuffer.allocate(59);

            final byte delimiter = ' ';

            sendBuffer.put(deviceID.getBytes());
            sendBuffer.put(delimiter);
            sendBuffer.putLong(timestamp);
            sendBuffer.put(delimiter);
            sendBuffer.putDouble(latitude);
            sendBuffer.put(delimiter);
            sendBuffer.putDouble(longitude);

            byte[] sendBuf = sendBuffer.array();

            if (sClient != null && outStream != null) {
                sendData(sendBuf);
            }
        }

        public boolean isConnected() {
            return !sClient.isClosed();
        }
    }

    private class SendLocationTask extends AsyncTask<Client, Void, Void> {

        @Override
        protected Void doInBackground(Client... clients) {
            Client connection = clients[0];

            connection.sendLocation();

            return null;
        }
    }
}
