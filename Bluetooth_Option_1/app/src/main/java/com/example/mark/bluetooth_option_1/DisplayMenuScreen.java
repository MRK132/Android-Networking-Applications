package com.example.mark.bluetooth_option_1;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import android.view.View;

public class DisplayMenuScreen extends AppCompatActivity {

    private BluetoothAdapter BTAdapter;                            //BluetoothAdapter will be used to check whether user has BT enabled
    private int REQUEST_ENABLE_BT = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_menu_screen);   //Setting up with first activity, to display menu screen


        int PERMISSION_CODE_1 = 15;                     //Make use of a constant for checking permission checking
        if (Build.VERSION.SDK_INT >= 15)                //Earlier sdk versions require permission when calling location api
        {
            if (ActivityCompat.checkSelfPermission(DisplayMenuScreen.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_1); //Checking permissions within the context of this activity
            }
        }


        BTAdapter = BluetoothAdapter.getDefaultAdapter();  //Switch on BT if necessary
        if(!BTAdapter.isEnabled()){                    //Request user to enable bluetooth if not on
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        Intent service = new Intent(getApplicationContext(), ConnectivityServices.class);     //Intent to start background service for GPS
        startService(service);

    }


    //Button actions for menu screen: tap to open map with GPS markers, or tap to open list view of unique BT devices discovered
    public void onButtonClick(View v) {
        if (v.getId() == R.id.GPSActivity) {
            Intent i = new Intent(DisplayMenuScreen.this, MapsActivity.class);
            startActivity(i);
        } else if (v.getId() == R.id.BluetoothActivity) {
            Intent i = new Intent(DisplayMenuScreen.this, BluetoothDiscoveriesScreen.class);
            startActivity(i);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
    }

    protected void onDestroy(){
        super.onDestroy();
        stopService(new Intent(this, ConnectivityServices.class));
        if (BTAdapter != null) {
            BTAdapter.cancelDiscovery();       //Turn off any ongoing discovery
        }
    }

}
