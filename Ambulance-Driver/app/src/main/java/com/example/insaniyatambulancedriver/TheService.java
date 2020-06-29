package com.example.insaniyatambulancedriver;


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Timer;

public class TheService extends Service
{
    public static final String BROADCAST_ACTION = "Hello World";
    private static final int TWO_MINUTES = 1000 * 60 * 2;
    public LocationManager locationManager;
    public MyLocationListener listener;
    public Location previousBestLocation = null;
    public Double Longitude;
    public Double Latitude;
    private static Timer timer = new Timer();

    Intent intent;
    int counter = 0;

    @Override
    public void onCreate()
    {
        super.onCreate();
        intent = new Intent(BROADCAST_ACTION);

    }

    @Override
    public void onStart(Intent intent, int startId)
    {
        Log.d("After service starts:","2");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new MyLocationListener();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        //locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 500, (LocationListener) listener);

        //update location after every 10 sec in 500m radius
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 50, listener);


    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public void onDestroy()
    {
        // handler.removeCallbacks(sendUpdatesToUI);
        super.onDestroy();
        Log.d("before service stops:","2");
        locationManager.removeUpdates(listener);
    }

    public class MyLocationListener implements LocationListener
    {

        public void onLocationChanged(final Location loc)
        {

            Log.i("*****", "Location changed"+loc.getLatitude()+","+loc.getLongitude());
            //Toast.makeText(TheService.this, "....", Toast.LENGTH_SHORT).show();
            Toast.makeText(TheService.this, "("+loc.getLatitude()+","+loc.getLongitude()+")", Toast.LENGTH_SHORT).show();

            FirebaseFirestore mFireStore;
            mFireStore = FirebaseFirestore.getInstance();

            FirebaseAuth mFirebaseAuth;
            mFirebaseAuth = FirebaseAuth.getInstance();
            final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();
            String email = mFirebaseUser.getEmail();

            Log.d("Email: ",""+email);

            DocumentReference Ref = mFireStore.collection("Ambulance Drivers")
                    .document(""+email);

            // update location
            Ref
                    .update("latitude", ""+loc.getLatitude(),"longitude",""+loc.getLongitude())
                    .addOnSuccessListener(new OnSuccessListener<Void>()
                    {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d("Update service","DocumentSnapshot successfully updated!");
                            //Toast.makeText(TheService.this, "DocumentSnapshot successfully updated!", Toast.LENGTH_SHORT).show();

                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d("Update service","Error updating document "+e);
                            //Toast.makeText(TheService.this, "Error updating document", Toast.LENGTH_SHORT).show();

                        }
                    });

            Log.d("Update service","Data added to Firecloud");
            //Toast.makeText(TheService.this, "Data added to Firecloud", Toast.LENGTH_SHORT).show();
            //end

        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {

        }

        public void onProviderDisabled(String provider)
        {
            Log.d("GPS:","Disabled");
            //Toast.makeText(TheService.this, "GPS Disabled", Toast.LENGTH_SHORT).show();
            Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
            //Toast.makeText(TheService.this, "", Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider)
        {
            Log.d("GPS:","Enabled");

            //Toast.makeText(TheService.this, "GPS Enabled", Toast.LENGTH_SHORT).show();
            Toast.makeText( getApplicationContext(), "Gps Enabled", Toast.LENGTH_SHORT).show();
        }
    }
}