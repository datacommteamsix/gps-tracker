/*---------------------------------------------------------------------------------------
--	SOURCE FILE:		MainActivity.java
--
--	PROGRAM:		gpsTracker
--
--	FUNCTIONS:		onCreate(savedInstanceState)
--                  hashToMD5(string s)
--                  initLocationServices()
--                  Connect(LocationManager lm)
--
--	DATE:			April 05, 2018
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
--	This is the main activity of the android application. It contains the main function
--  calls and location listener to listen for location changes. The program will send
--  current locations to the server upon location change.
---------------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    onCreate
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Angus Lam, Benny Wang, Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   protected void onCreate(Bundle savedInstanceState)
    --
    -- RETURNS: void
    --
    -- NOTES: main function, initialize all the text fields and buttons upon the
    -- start of application.
    ------------------------------------------------------------------------------*/
    @SuppressLint({"HardwareIds", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initLocationServices();

        final Button ConnectBtn = (Button) findViewById(R.id.buttonConnect);
        ipEditText = (EditText) findViewById(R.id.IPEdit);
        portEditText = (EditText) findViewById(R.id.portEdit);
        statusLabel = (TextView) findViewById(R.id.statusLabel);
        statusLabel.setText(getResources().getString(R.string.status) + " Server Disconnected");

        final LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        deviceId = hashToMD5(Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID));

        ConnectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.w("Roger", "Start connection " + deviceId);
                Connect(lm);
            }
        });
    }

    /*------------------------------------------------------------------------------
    -- FUNCTION:    hashToMD5
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Angus Lam, Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   public String hashToMD5(String s)
    --
    -- RETURNS: string : hashed string of the device ID
    --
    -- NOTES: Hash function to create a MD5 hash for the device ID sent to the server
    ------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    initLocationServices
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   public void initLocationServices()
    --
    -- RETURNS: void
    --
    -- NOTES: Check for user permission for COARSE and FINE Location. This is needed
    -- for the newer version of android.
    ------------------------------------------------------------------------------*/
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    Connect
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Roger Zhang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   public void Connect(LocationManager lm)
    --
    -- RETURNS: void
    --
    -- NOTES: Connection call to send location updates to the server, a new instance
    -- of the client is created here.
    ------------------------------------------------------------------------------*/
    public void Connect(LocationManager lm) {
        int port;

        serverIp = ipEditText.getText().toString();
        serverPort = portEditText.getText().toString();
        Log.w("Roger", serverIp + ":" + serverPort);

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

            Client connection = new Client(deviceId, serverIp, port);

            final mLocationListener ll = new mLocationListener(lm, connection);
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,  TIME_RESOLUTION, DISTANCE_RESOLUTION, ll);

            ll.onLocationChanged(lm.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
    }

    /*---------------------------------------------------------------------------------------
    --	SOURCE FILE:		MainActivity.java
    --
    --	PROGRAM:		public class mLocationListener implements LocationListener
    --
    --	FUNCTIONS:		onLocationChanged(Location location)
    --                  SendLocationTask extends AsyncTask<Client, Void, Void>
    --
    --	DATE:			April 05, 2018
    --
    --	REVISIONS:		NONE
    --
    --
    --	DESIGNERS:		Benny Wang, Roger Zhang
    --
    --
    --	PROGRAMMER:		Benny Wang, Roger Zhang
    --
    --	NOTES:
    --	This is the location listener class that updates and sends the location upon location
    --  change.
    ---------------------------------------------------------------------------------------*/
    public class mLocationListener implements LocationListener {
        Client connection;
        LocationManager lm = null;

        mLocationListener(LocationManager manager, Client client) {
            lm = manager;
            connection = client;
        }

    /*------------------------------------------------------------------------------
    -- FUNCTION:    onLocationChanged
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Roger Zhang, Benny Wang
    --
    -- PROGRAMMER:  Roger Zhang
    --
    -- INTERFACE:   public void onLocationChanged(Location location)
    --
    -- RETURNS: void
    --
    -- NOTES: onLocationChanged method gets called whenever there is a location
    -- change. The updates are being called in an async task in the back ground.
    ------------------------------------------------------------------------------*/
        @SuppressLint("SetTextI18n")
        @Override
        public void onLocationChanged(Location location) {
            if (connection.isConnected())
            {
                connection.setLocation(location);
                new SendLocationTask().execute(connection);
                statusLabel.setText(getResources().getString(R.string.status) + " Server Connected");
            }
            else
            {
                Toast.makeText(MainActivity.this, "Not connected - Location not sent", Toast.LENGTH_SHORT).show();
                statusLabel.setText(getResources().getString(R.string.status) + " Server Disconnected");
            }
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onProviderDisabled(String provider) {
            statusLabel.setText(getResources().getString(R.string.status) + "Location Provider Disconnected");
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

    /*------------------------------------------------------------------------------
    -- FUNCTION:    SendLocationTask
    --
    -- DATE:        April 5th, 2018
    --
    -- REVISIONS:
    --
    -- DESIGNER:    Benny Wang
    --
    -- PROGRAMMER:  Benny Wang
    --
    -- INTERFACE:   SendLocationTask()
    --
    -- RETURNS: void
    --
    -- NOTES: AsyncTask that updates the client's location to the server.
    ------------------------------------------------------------------------------*/
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
