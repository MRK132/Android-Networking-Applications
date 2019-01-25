package com.example.mark.bluetooth_option_1;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


// In this class, unique discovered Bluetooth devices are added to a list on a FireBase mDatabase,
// while co-ordinates and discovered BT devices of a location are added to a second list in the mDatabase.
// GPS and Network statuses are handled here also.


public class ConnectivityServices extends Service implements android.location.LocationListener {
    private LocationManager lm;
    private static final String TAG = "GPS_Service";
    private DatabaseReference mDatabase;                                                        //Setting up backend
    private Context context = this;
    private BluetoothAdapter BtAdapter;
    private ArrayList<String> allNewDevicesScanned = new ArrayList<String>();                       //Populated by any UNIQUE device discovered in a single Bluetooth scan
    public ArrayList<String> uniqueDevices = new ArrayList<String>();                               //Populated by all unique devices discovered
    private ArrayList<String> allDevicesFound = new ArrayList<String>();                            //Populated by every device discovered in a single Bluetooth scan


    private int fifteenMinStatusCheck = 900000; //1000*60*15  -  Every 15 mins - Check GPS/Network status
    private int fifteenMinLocUpdate = 900000; //1000*60*15   -   Every 15 mins - update location

    private boolean databaseDownloadComplete = false;                                                 //Will be asserted true when all first mDatabase query completed
    boolean isGPSEnabled = false;                                                               // Status flags:
    boolean isNetworkEnabled = false;
    private boolean gpsActive = false;
    private boolean networkActive = false;
    private Handler handler;


    public ConnectivityServices(){
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
      //  Log.e(TAG, "onCreate");
        mDatabase = FirebaseDatabase.getInstance().getReference();     //Reference to mDatabase

        //Now retrieve every unique BT device from Firebase and places in arraylist:

        final DatabaseReference ref = mDatabase.child("UniqueBT").getRef(); //Reference to Location Data
        System.out.println("set up map ref..." + ref);

        // New listener to read the data
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot deviceRef : dataSnapshot.getChildren()) {             //For each unique BT device in the mDatabase:
                    String BTdevice = deviceRef.getValue().toString();

                    if (BTdevice != null) {
                        BTdevice = BTdevice.replaceAll("\\[", "").replaceAll("\\]","");
                        uniqueDevices.add(BTdevice);
                    }
                }
                databaseDownloadComplete = true;                      //Identifies if any devices found in scan have been already discovered previously
                ref.removeEventListener(this);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }

        });
        try {

            Toast.makeText(getApplicationContext(), "GPS Service Active", Toast.LENGTH_SHORT);        //Lets user know GPS service is active
            lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            /* Requesting location updates   */
            lm.requestLocationUpdates(lm.GPS_PROVIDER,fifteenMinLocUpdate ,0,this);          //Start using GPS - update every 20 sec
            //lm.requestLocationUpdates(lm.NETWORK_PROVIDER,fifteenMinLocUpdate, 0,this );   //Start using Network - update every 20 sec

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);           // IntentFilter for BT Receiver
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(BtReceiver, filter);


            handler = new Handler();                                               //Status timer:
            StatusChecker.run();


        }
        catch (SecurityException e) {
            Log.e(TAG, "exception occurred" + e.getMessage());
        }
        catch (Exception e) {
            Log.e(TAG, "exception occurred " + e.getMessage());
        }
    }


    Runnable StatusChecker = new Runnable() {
        @Override
        public void run() {                                     //The timer checks the GPS and network statuses regularly
                handler.postDelayed(StatusChecker, fifteenMinStatusCheck);
                checkGPSandNetworkStatus();                             //Determine status of GPS location/network location services available
        }
    };


    public void checkGPSandNetworkStatus(){             // Determine status of providers available:
        //Check Permissions:
        if ( Build.VERSION.SDK_INT >= 15 &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            //Determine status of both GPS and network services
            try{isGPSEnabled= lm.isProviderEnabled(LocationManager.GPS_PROVIDER);}catch(Exception ex){}
            try{isNetworkEnabled= lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);}catch(Exception ex){}

            //Begin to use GPS, if it had been previously unavailable but is now enabled, update every 15 mins
            if (isGPSEnabled && !gpsActive){
                lm.requestLocationUpdates(lm.GPS_PROVIDER,fifteenMinLocUpdate ,0,this);
                gpsActive = true;
                networkActive = false;
            }
            //Use just the network services, update every 15 mins
            else if (isNetworkEnabled && !networkActive){
                lm.requestLocationUpdates(lm.NETWORK_PROVIDER,fifteenMinLocUpdate ,0,this);
                networkActive = true;
                gpsActive = false;
            }
        }
        else{
            System.out.println("Permissions denied");
        }
    }


    @Override
    public void onDestroy()
    {
        Log.e(TAG, "onDestroy");
        super.onDestroy();
        if (lm != null) {
            try {
                lm.removeUpdates(this);
            }
            catch (SecurityException e) {
                Log.e(TAG, "exception occurred " + e.getMessage());
            }
            catch (Exception ex) {
                Log.i(TAG, "fail to remove location listeners, ignore", ex);
            }
        }
        if (BtAdapter != null) {
            BtAdapter.cancelDiscovery();       //Cancel ongoing discovery
        }
        unregisterReceiver(BtReceiver);         //Remove bluetooth listener
        handler.removeCallbacks(StatusChecker);//Turn of the timer
    }




    public void onLocationChanged(Location arg0) {

        BtAdapter = BluetoothAdapter.getDefaultAdapter();          // Getting the Bluetooth adapter
        if(BtAdapter != null) {
            BtAdapter.startDiscovery();                            //Start searching for all nearby BT devices
            Toast.makeText(this, "Starting BT discovery...", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "Bluetooth disabled-Please re-enable bluetooth and try again.", Toast.LENGTH_LONG).show();
        }

        while(BtAdapter.isDiscovering()){}                               // While discovering devices:
        LocationData obj = new LocationData(arg0.getLatitude(),arg0.getLongitude(), allDevicesFound);   //Push to the mDatabase storing co-ords and list of BT devices discovered
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mDatabase.child("BT with GPS Location").push().setValue(obj);                                   //Push to corresponding mDatabase list in the DB
        Log.e(TAG, "Data recorded: " + obj.latitude + " " + obj.longitude);


        //Only required to add to the Firebase unique devices list if a new device has been identified in the latest BT scan
        if (!allNewDevicesScanned.isEmpty()) {                                     //If a new unique device is been found:
            int id = 1;

            String DateTime = DateFormat.getDateTimeInstance().format(new Date()); // Each unique BT device must be assigned a unique id:
            DateTime = DateTime.replace(":", "-");
            DateTime = DateTime.replace(".", "_");

            ArrayList<String> device = new ArrayList<String>();

                                                                                                        //For each new device found:
            for(String uniqueDevice : allNewDevicesScanned){
                device.add(uniqueDevice);
                mDatabase = FirebaseDatabase.getInstance().getReference().child("UniqueBT").getRef();              //Reference to the mDatabase
                mDatabase.child(DateTime+" "+id).setValue(device);                                                    //id =  date and time - push to mDatabase

                id++;                                                                                              //Increment if multiple new devices
                device.clear();
            }
            allNewDevicesScanned.clear();  //Reset list
        }
        allDevicesFound.clear();            //Reset list

    }


    public void onProviderDisabled(String arg0) {
        Log.e(TAG, "provider disabled " + arg0);
    }

    public void onProviderEnabled(String arg0) {
        Log.e(TAG, "provider enabled " + arg0);
    }

    public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        Log.e(TAG, "status changed to " + arg0 + " [" + arg1 + "]");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Background Service Running:",Toast.LENGTH_LONG).show();
        return START_STICKY;
    }


    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
        Log.i(TAG, "onStart");
    }


    private final BroadcastReceiver BtReceiver = new BroadcastReceiver() {  //Seeks ACTION_DISCOVERY_FINISHED in onCreate() - when Bluetooth discovery is over

        @Override
        public void onReceive(Context context, Intent intent) {          //Seeking an ACTION_DISCOVERY_FINISHED when BT discovery is over
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice BTDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                allDevicesFound.add("Name: " + BTDevice.getName() + " Address: " + BTDevice.getAddress());  //Add any device found

                String id = BTDevice.getName() + ", " + BTDevice.getAddress();                              //Store as format: name, address

                if (!uniqueDevices.contains(id) && databaseDownloadComplete) {                              //Determine whether current device id is in unique device list
                    uniqueDevices.add(BTDevice.getName() + ", " + BTDevice.getAddress());                   //Add to local list,
                    allNewDevicesScanned.add(BTDevice.getName() + ", " + BTDevice.getAddress());            //Once added it is then pushed to Firebase mDatabase

                }

            }
        }
    };

}
