package ca.bcit.comp4985.gpstracker;

import android.Manifest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
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
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    String sIP;
    String sPort;
    int port;
    EditText IPEditText;
    EditText PortEditText;
    String deviceID;

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
        deviceID = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceID = md5(deviceID);
        // May be useful if server require client ip address
        final String deviceip = getWifiAddress();

        ConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w("Roger", "Start connection" + deviceID);
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
//    public String getWifiAddress() {
//        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
//        return ip;
//    }

    private String intToIp(int ip) {
        return String.format("%d.%d.%d.%d", (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void Connect(LocationManager lm, String devicename) {

        sIP = IPEditText.getText().toString();
        sPort = PortEditText.getText().toString();
        Log.w("Roger", sIP + "-" + port);
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
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, ll);
        }
    }

    public class mLocationListener implements LocationListener {
        public long timestamp;
        public double latitude;
        public double longitude;
        public String devicename;
        public String s_ip;
        public int port;
        LocationManager lm = null;

        public mLocationListener(LocationManager manager, String dn, String ip, int p) {
            lm = manager;
            timestamp = System.currentTimeMillis() / 1000;
            latitude = -1;
            longitude = -1;
            devicename = dn;
            s_ip = ip;
            port = p;

            Client connection = new Client(s_ip, port);
            connection.longitude = longitude;
            connection.latitude = latitude;
            connection.timestamp = timestamp;
            connection.devicename = devicename;
            connection.start();
        }

        @Override
        public void onLocationChanged(Location location) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            timestamp = location.getTime();

            Client connection = new Client(s_ip, port);
            connection.longitude = longitude;
            connection.latitude = latitude;
            connection.timestamp = timestamp;
            connection.devicename = devicename;
            connection.start();

            lm.removeUpdates(this);
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

    class Client extends Thread {
        Socket sClient;
        DataOutputStream outStream;
        String ipAddress;
        int port;
        public long timestamp;
        private String message;
        public double latitude;
        public double longitude;
        public String devicename;

        public Client(String s_ip, int p) {
            ipAddress = s_ip;
            port = p;
            message = null;
            sClient = null;
            outStream = null;
        }

        public void connectToServer() {
            try {
                if (ipAddress != null && port != -1) {
                    try {
                        sClient = new Socket(ipAddress, port);
                    } catch (NetworkOnMainThreadException e) {

                    }
                    if (sClient != null) {
                        outStream = new DataOutputStream(sClient.getOutputStream());
                    }
                }
            } catch (Exception e) {
                Log.w("error", e.toString());
            }
        }

        public boolean sendData(String data) {
            if (outStream != null) {
                try {
                    outStream.writeUTF(data);
                } catch (IOException e) {
                    Log.w("error", e.toString());
                    return false;
                }
            }
            return true;
        }

        public void run() {
            if (sClient == null) {
                connectToServer();
                Log.w("Roger", "Reconnect to server");
            }

            message = deviceID + " " + timestamp + " " + latitude + " " + longitude + " " + devicename;
            if (sClient != null && outStream != null && message != null) {
                sendData(message);
                Log.w("Roger", "Client sending" + message.toString());
                message = null;
                try {
                    sClient.close();
                } catch (IOException e) {
                }
            }
        }

        public boolean isConnected() {
            return !sClient.isClosed();
        }

    }
}