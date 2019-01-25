package com.example.mark.bluetooth_option_1;

import java.util.ArrayList;

public class LocationData {

    public double longitude;
    public double latitude;
    public ArrayList<String> BTinfo;

    public LocationData() {

    }

    public LocationData(double latitude,double longitude, ArrayList<String> s) {
        this.longitude = longitude;
        this.latitude = latitude;
        BTinfo = s;
    }

    public int NumberBTDevices(){
        if (BTinfo == null){
            return 0;
        }
        return BTinfo.size();
    }


}
