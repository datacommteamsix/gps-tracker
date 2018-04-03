package ca.bcit.comp4985.gpstracker;

import android.Manifest;
import android.annotation.SuppressLint;
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
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MainActivity extends AppCompatActivity {

    String deviceId;
    String serverIp;
    String serverPort;
    EditText ipEditText;
    EditText portEditText;
    TextView statusLabel;

    public static final float DISTANCE_RESOLUTION = 2.0f;
    public static final int TIME_RESOLUTION = 3 * 1000;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocationServices();

        final Button ConnectBtn = (Button) findViewById(R.id.buttonConnect);
        ipEditText = (EditText) findViewById(R.id.IPEdit);
        portEditText = (EditText) findViewById(R.id.portEdit);
        statusLabel = (TextView) findViewById(R.id.statusLabel);

        final LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        deviceId = hashToMD5(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));

        // May be useful if server require client ip address
        // final String deviceip = getWifiAddress();

        ConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w("Roger", "Start connection " + deviceId);
                Connect(lm);
            }
        });
    }

    public String hashToMD5(String s) {
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
        return String.format("%d.%d.%d.%d".toLowerCase(), (ip & 0xff), (ip >> 8 & 0xff), (ip >> 16 & 0xff), (ip >> 24 & 0xff));
    }

    public void Connect(LocationManager lm) {
        int port = -1;

        serverIp = ipEditText.getText().toString();
        serverPort = portEditText.getText().toString();
        Log.w("Roger", serverIp + ":" + port);

        if (serverIp != null && serverIp.length() > 0 && serverPort != null && serverPort.length() > 0) {
            port = Integer.parseInt(serverPort);
            Log.w("Roger", serverIp + " " + port);

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

            final mLocationListener ll = new mLocationListener(lm, serverIp, port);
            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,  TIME_RESOLUTION, DISTANCE_RESOLUTION, ll);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,  TIME_RESOLUTION, DISTANCE_RESOLUTION, ll);
        }
    }

    public class mLocationListener implements LocationListener {
        String serverIp;
        int serverPort;
        Client connection;
        LocationManager lm = null;

        mLocationListener(LocationManager manager, String ip, int p) {
            lm = manager;
            serverIp = ip;
            serverPort = p;

            connection = new Client(deviceId, serverIp, serverPort);
        }

        @Override
        public void onLocationChanged(Location location) {
            if (connection.isConnected())
            {
                connection.setLocation(location);
                new SendLocationTask().execute(connection);
            }
            else
            {
                Toast.makeText(MainActivity.this, "Not connected - Location not sent", Toast.LENGTH_SHORT).show();
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onProviderDisabled(String provider) {
            statusLabel.setText("Location Provider Disconnected");
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

    private static class SendLocationTask extends AsyncTask<Client, Void, Void> {
        @Override
        protected Void doInBackground(Client... clients) {

            for (Client client : clients)
            {
                client.sendLocation();
            }

            return null;
        }
    }
}
