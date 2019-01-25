package com.example.mark.bluetooth_option_1;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseError;

import java.util.ArrayList;


public class BluetoothDiscoveriesScreen extends AppCompatActivity {


    private ListView listView;                                                  //Used to list unique BT devices
    private DatabaseReference database;                                         //Reference to Firebase
    public ArrayList<String> uniqueBT = new ArrayList<String>();                     //Stores the unique BT devices
    private ArrayAdapter<String> adapter;                                       //used to update ListView

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unique_bluetooth_discoveries_screen);
        database = FirebaseDatabase.getInstance().getReference().child("UniqueBT").getRef();  //Reference to my Firebase database

        listView = findViewById(R.id.uniqueBlueList);                                         //Reference to list view for layout file
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, uniqueBT);   // adapter is initialised,
        listView.setAdapter(adapter);                                                                             //and then assigned here.


        database.addChildEventListener(new ChildEventListener() {          // Adding a listener to listen to changes in the database for unique BT devices
            @Override
            public void onChildAdded(DataSnapshot ds, String s) {

                for (DataSnapshot BTdeviceReference : ds.getChildren()) {             //For each entry:
                    String device = BTdeviceReference.getValue().toString();          //Store BT device info as String

                    if (device != null) {
                        device = device.replaceAll("\\[", "").replaceAll("\\]","");
                        uniqueBT.add(device);
                    }
                }
                adapter.notifyDataSetChanged();                                         //Notify ListView of changes made
            }


            @Override
            public void onChildChanged(DataSnapshot  dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }



}
