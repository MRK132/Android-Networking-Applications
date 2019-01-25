package com.example.mark.gps_app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements LocationListener , OnMapReadyCallback, ValueEventListener {

    private GoogleMap mMap;
    private Marker markerLocation;
    private DatabaseReference mDatabase;
    private LocationManager lm;


    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Permission to access GPS")
                        .setMessage("Please allow the app to access you location.")
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        99);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        99);
            }
            return false;
        } else {
            return true;
        }
    }


    private void startGettingLocations() {



        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGPS = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetwork = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        boolean canGetLocation = true;
        int ALL_PERMISSIONS_RESULT = 101;
        long MIN_DISTANCE_CHANGE_FOR_UPDATES = 100;     //Distance in meters
        long MIN_TIME_BW_UPDATES = 1000 * 60 * 1;       //Time in milliseconds

        ArrayList<String> permissions = new ArrayList<>();
        ArrayList<String> permissionsToRequest;

        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);


        //Check if GPS and Network are on, if not asks the user to turn on
        if (!isGPS && !isNetwork) {
            showSettingsAlert();
        } else {
            // check permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (permissionsToRequest.size() > 0) {
                    requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]),
                            ALL_PERMISSIONS_RESULT);
                    canGetLocation = false;
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Permission not Granted", Toast.LENGTH_SHORT).show();


            return;
        }

        //Starts requesting location updates
        if (canGetLocation) {
            if (isGPS) {
                lm.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

            } else if (isNetwork) {
                // from Network Provider

                lm.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, this);

            }
        }
        else{
            Toast.makeText(this, "Can't get location", Toast.LENGTH_SHORT).show();
        }
    }
    private ArrayList findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList result = new ArrayList();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }
    private boolean hasPermission(String permission) {
        if (canAskPermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }
    private boolean canAskPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("GPS is not Enabled!");
        alertDialog.setMessage("Do you want to turn on GPS?");
        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }


    public void onLocationChanged(Location location) {
        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        Log.e("#TAG", String.valueOf(location.getLatitude()));
        addMarker(latLng);
        LocationData locationData = new LocationData(location.getLatitude(), location.getLongitude());      // lat and longi data in a LocationData object
        markerLocation = mMap.addMarker(new MarkerOptions().position(latLng));  //This keeps the previous marker added for a new location from the current session from disappearing when you add a second new location marker in the same session

        FirebaseDatabase database = FirebaseDatabase.getInstance();         //reference to Firebase
        DatabaseReference myRef = database.getReference("location");     //reference to my database
        myRef.child(Long.toString(System.currentTimeMillis())).setValue(locationData);  // use setValue() to write the lat and longi data to Firebase database

        long timestamp = System.currentTimeMillis();    // the time location was visited
        String ts = String.valueOf(timestamp);
        myRef.child(ts).setValue(locationData);         // update database with time location was visited

    }

    private void addMarker(LatLng latLng) {
        if (latLng == null) {
            return;
        }
        if (markerLocation != null) {
            markerLocation.remove();
        }
        Date now = new Date();
        SimpleDateFormat dt = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.US);
        String newLoc = dt.format(now);//

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("New Location: "+newLoc);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        if (mMap != null)
            markerLocation = mMap.addMarker(markerOptions);


        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(latLng.latitude, latLng.longitude))
                .zoom(16)
                .build();

        if (mMap != null)
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }



    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError){

    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot){
        //Post post = dataSnapshot.getValue(Post.class);

    }


    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        mDatabase = FirebaseDatabase.getInstance().getReference();
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        int PERMISSION_CODE_1 = 23;

        checkLocationPermission(); //Request the user to access GPS
        startGettingLocations();   //Start listening to new locations

            }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */





    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng engBuilding = new LatLng(53.283912, -9.063874);
        mMap.addMarker(new MarkerOptions().position(engBuilding).title("Original Marker in NUIG Engineering Building"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(engBuilding));

        final DatabaseReference locRef = mDatabase.child("location").getRef(); //Gets a Reference for the location at the specified path "location".

        locRef.addValueEventListener(new ValueEventListener() {     // Add a listener to read data at posts reference
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                for (DataSnapshot locSnapshot : dataSnapshot.getChildren()) {                     //For each database entry:
                    LocationData location = locSnapshot.getValue(LocationData.class);

                    if (location != null) {                      //Assign map markers based on locations in Firebase database

                        double latitude = location.latitude;                         //Retrieve latitude
                        double longitude = location.longitude;                       //Retrieve longitude
                        LatLng latLng = new LatLng(latitude, longitude);             //Use both in new LatLng object

                        String dateLong = locSnapshot.getKey();             //Retrieve the each key from each database entry
                        new Date(Long.valueOf(dateLong));                   //Convert each key to long
                        Date date = new Date(Long.valueOf(dateLong));       //Each key will be in the correct date format

                        //Place these previously visited locations that are stored in Firebase as green markers on map:
                        markerLocation = mMap.addMarker(new MarkerOptions().position(latLng).title(String.valueOf(date)).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

                        }
                }
                locRef.removeEventListener(this);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }


}


