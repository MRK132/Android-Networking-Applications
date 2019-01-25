package com.example.mark.bluetooth_option_1;

import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import static com.example.mark.bluetooth_option_1.R.id.map;

public class MapsActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback {

    private GoogleMap mMap;
    private Marker markerLocation;
    private DatabaseReference mDatabase;

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();          //Reference to Firebase
        int PERMISSION_CODE_1 = 15;

        try {
            if (Build.VERSION.SDK_INT >= 15)                                //Earlier SDK versions require permission when calling location api
            {
                if (ActivityCompat.checkSelfPermission(MapsActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
                {  ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE_1);
                }
            }
        } catch (SecurityException e) {
            Log.e("GPS", "exception occured " + e.getMessage());
        } catch (Exception e) {
            Log.e("GPS", "exception occured " + e.getMessage());
        }
        if (mMap == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(map)).getMapAsync(this);  // Obtain the map from the SupportMapFragment.
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap == null) {
            ((SupportMapFragment) getSupportFragmentManager().findFragmentById(map)).getMapAsync(this); // Obtain the map from the SupportMapFragment.
        }
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        // Setting up the map:
        if (mMap != null) {

            final DatabaseReference ref = mDatabase.child("BT with GPS Location").getRef(); //database reference
            ref.addValueEventListener(new ValueEventListener() {                            //Add a listener to read data

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot locSnapshot : dataSnapshot.getChildren()) {           //For each database entry:
                        LocationData location = locSnapshot.getValue(LocationData.class);        //Get LocationData entry from database

                        if (location != null) {                                                  //Assign map markers based on locations in Firebase database

                            double latitude = location.latitude;                                 //Retrieve latitude
                            double longitude = location.longitude;                               //Retrieve longitude
                            LatLng latLng = new LatLng(latitude, longitude);                     //Use both in new LatLng object

                                                                                            //Assign marker at the location specified, with a label showing the number of BT devices active in that location:
                            markerLocation = mMap.addMarker(new MarkerOptions().position(latLng).title("Number of BT devices active at this location: "+location.NumberBTDevices()));
                            System.out.println(latitude + " , " + longitude);
                        }
                    }
                    ref.removeEventListener(this);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });


            LatLng coordinate = new LatLng(53.283912, -9.063874);                    // Zoom in and centre map around Galway city:
            CameraUpdate zoomGalway = CameraUpdateFactory.newLatLngZoom(coordinate, 12);
            mMap.animateCamera(zoomGalway);
        }
    }

}
