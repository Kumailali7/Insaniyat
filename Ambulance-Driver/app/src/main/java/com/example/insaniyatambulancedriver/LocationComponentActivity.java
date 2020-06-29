package com.example.insaniyatambulancedriver;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;

import org.w3c.dom.Text;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


/**
 * Use the LocationComponent to easily add a device location "puck" to a Mapbox map.
 */
public class LocationComponentActivity extends AppCompatActivity implements
        OnMapReadyCallback, PermissionsListener
{

    private PermissionsManager permissionsManager;
    private MapboxMap mapboxMap;
    private MapView mapView;

    private FirebaseAuth mFirebaseAuth;

    public Boolean indicator = true;

    //for storing document id
    private String assignedDocID;


    //for FINDLOC() which is not used
    private FusedLocationProviderClient client;
    double latitude;
    double longitutde;

    //new
    private FirebaseFirestore mFireStore;
    private ListenerRegistration noteListener;
    //new end

    //new location
    public double driverlat;
    public double driverLong;
    public double userLat;
    public double userLong;
    //public String userName;
    //public String UserPickUpArea;

    //for reject notification
    public DocumentChange obj = null;

    //new navigation
    // variables for calculating and drawing a route
    private DirectionsRoute currentRoute;
    private static final String TAG = "DirectionsActivity";
    private NavigationMapRoute navigationMapRoute;

    private TextView details;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token));

        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_location_component);

        //hide title bar
        getSupportActionBar().hide();
        details = findViewById(R.id.details);

        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        mFireStore = FirebaseFirestore.getInstance();

        //Toggle button starting the service of update location
        final ToggleButton toggle = findViewById(R.id.toggle);
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                if (isChecked)
                {
                    // The toggle is enabled
                    toggle.setBackgroundResource(R.drawable.button_border);
                    //logic for signout
                    indicator = false;

                    //FINDLOC();
                    Log.d("Before service starts:", "1");

                    //Start service for update in driver`s location
                    startService(new Intent(getBaseContext(), TheService.class));

                    //Listening to any update in the Assigned Driver Collection
                    attachListener();


                }
                else
                    {
                    // The toggle is disabled
                        toggle.setBackgroundResource(R.drawable.button_border2);
                    //logic for signout
                    indicator = true;


                    //Start of (End ride logic)

                    if (obj != null)
                    {

                        //getting document id for assigned ride
                        mFireStore.collection("AssignedRides")
                                .whereEqualTo("AmbulanceDriverID", "" + mFirebaseUser.getEmail())
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
                                {
                                    @Override
                                    public void onComplete(@NonNull Task<QuerySnapshot> task)
                                    {
                                        if (task.isSuccessful())
                                        {
                                            for (QueryDocumentSnapshot document : task.getResult())
                                            {
                                                assignedDocID = document.getId();

                                                Log.d("Status", "Fetched doc Id");
                                                Log.d("Status", "" + document.getId() + " => " + document.getData());

                                            }

                                            //querry 2
                                            //deleting document after ride is ended
                                            mFireStore.collection("AssignedRides").document(assignedDocID)
                                                    .delete()
                                                    .addOnSuccessListener(new OnSuccessListener<Void>()
                                                    {
                                                        @Override
                                                        public void onSuccess(Void aVoid)
                                                        {
                                                            Log.d("Status", "Document deleted " + assignedDocID);
                                                            Log.d("Status", "DocumentSnapshot successfully deleted!");

                                                        }
                                                    })
                                                    .addOnFailureListener(new OnFailureListener() {
                                                        @Override
                                                        public void onFailure(@NonNull Exception e) {
                                                            Log.d("Status", "Document could not be deleted");
                                                            Log.d("Status", "Error deleting document" + e);
                                                        }
                                                    });
                                            //delete end
                                            //querry 2 end


                                        }
                                        else
                                            {
                                            Log.d("Status", "Could not fetched doc Id");
                                            Log.d("Status", "Error getting documents: " + task.getException());
                                        }
                                    }
                                });
                        //end getting
                    } else {

                        Log.d("Ride status", "No rides " + assignedDocID);

                        //querry 3
                        // Set "Status" to "Unavailable" of ambulance driver when goes offline
                        DocumentReference washingtonRef = mFireStore.collection("Ambulance Drivers")
                                .document(mFirebaseUser.getEmail());

                        washingtonRef
                                .update("status", "Unavailable")
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d("Press Go Offline:", "Updated to Unvailable");

                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        Log.d("status:", "Error updating document to Unvailable");

                                    }
                                });
                        //end querry 3


                    }

                    restartActivity();

                    detachListener();
                    //End of (End ride logic)

                    //end service
                    Log.d("After service stops:", "1");
                    stopService(new Intent(getBaseContext(), TheService.class));

                }
            }
        });


        //log out logic
        Button LogOut = findViewById(R.id.LogOut);
        LogOut.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                if (indicator.equals(true))
                {
                    signOut();
                }
                else
                    {
                    Toast.makeText(LocationComponentActivity.this, "First Go Offline", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    //here onCreate ends

    //My Customize functions   ///////////////////////////////////////////
    public void attachListener()
    {
        final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        final DocumentReference washingtonRef = mFireStore.collection("Ambulance Drivers")
                .document(mFirebaseUser.getEmail());

        washingtonRef
                .update("status", "Available")
                .addOnSuccessListener(new OnSuccessListener<Void>()
                {
                    @Override
                    public void onSuccess(Void aVoid)
                    {
//                        Log.d("Press Go online:", "Updated to Available");

                        client = LocationServices.getFusedLocationProviderClient(LocationComponentActivity.this);

                        if (ActivityCompat.checkSelfPermission(LocationComponentActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(LocationComponentActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            return;
                        }
                        client.getLastLocation().addOnSuccessListener(LocationComponentActivity.this, new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    Log.d("Location : ", "Lat: " + location.getLatitude());
                                    washingtonRef
                                            .update("latitude", "" + location.getLatitude(), "longitude", "" + location.getLongitude())
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Toast.makeText(LocationComponentActivity.this, "Location updated!", Toast.LENGTH_SHORT).show();
                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(LocationComponentActivity.this, "Location not updated, ERROR!", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    Toast.makeText(LocationComponentActivity.this, "Turn on GPS Or give permissions! " + location, Toast.LENGTH_SHORT).show();
                                }
                            }

                        }).addOnFailureListener(new OnFailureListener()
                        {
                            @Override
                            public void onFailure(@NonNull Exception e)
                            {
                                Toast.makeText(LocationComponentActivity.this, "Location not retrieved, ERROR!", Toast.LENGTH_SHORT).show();
                            }
                        });



                        //querry2 Listening to respective AssignedRides document
                        noteListener= mFireStore.collection("AssignedRides")
                                .whereEqualTo("AmbulanceDriverID", ""+mFirebaseUser.getEmail())
                                .addSnapshotListener(new EventListener<QuerySnapshot>()
                                {
                                    @Override
                                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                                        @Nullable FirebaseFirestoreException e)
                                    {
                                        if (e != null)
                                        {
                                            Log.w("Listening 1 :>", "listen:error", e);
                                            return;
                                        }

                                        for (DocumentChange dc : snapshots.getDocumentChanges())
                                        {
                                            switch (dc.getType())
                                            {
                                                case ADDED:
                                                    obj=dc;
                                                    //Pop up notification for Ambulance Driver
                                                    openPopUpWindow();

                                                    //Storing useful info for map navigation and notification pop-up
                                                    driverlat= new Double( dc.getDocument().getData().get("D_Latitude").toString());
                                                    driverLong= new Double( dc.getDocument().getData().get("D_Longitude").toString());
                                                    userLat = new Double(dc.getDocument().getData().get("U_Latitude").toString());
                                                    userLong = new Double(dc.getDocument().getData().get("U_Longitude").toString());

                                                    Log.d("Data added:>", "New Driver: " + dc.getDocument().getData());
                                                    //Toast.makeText(LocationComponentActivity.this, ""+dc.getDocument().getData(), Toast.LENGTH_LONG).show();

                                                    //Start navigation for driver
                                                    StartNavigation();
                                                    break;

                                        /*case MODIFIED:
                                            Log.d("Data modified", "is  : " + dc.getDocument().getData());
                                            break;*/

                                                case REMOVED:
                                                    restartActivity();
                                                    Log.d("Data removed", "Driver Removed is : " + dc.getDocument().getData());
                                                    break;
                                            }
                                        }

                                    }
                                });

                    }
                })
                .addOnFailureListener(new OnFailureListener()
                {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {

                        Log.d("status:","Error updating document to Available");

                    }
                });

    }

    public void detachListener()
    {
        noteListener.remove();
        Log.d("Listener: ","Removed");
        //Toast.makeText(LocationComponentActivity.this, "Remove Listener", Toast.LENGTH_SHORT).show();
    }

    public void StartNavigation()
    {
        Point origin = Point.fromLngLat(driverLong,driverlat);
        Point destination = Point.fromLngLat(userLong,userLat);
        Log.d("Driver coord:",""+origin);
        Log.d("User coord:",""+destination);
        getRoute(origin, destination);
        Toast.makeText(this, "Navigation about to start", Toast.LENGTH_SHORT).show();
    }

    void getRoute(Point origin, Point destination)
    {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>()
                {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response)
                    {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null)
                        {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1)
                        {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        //for debugging
                        Log.d("Route",""+currentRoute);


                        // Draw the route on the map
                        if (navigationMapRoute != null)
                        {
                            navigationMapRoute.removeRoute();
                            //Toast.makeText(LocationComponentActivity.this, "yoyooyo", Toast.LENGTH_SHORT).show();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);
                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable)
                    {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });

    }

    public void FINDLOC()
    {

        if(ActivityCompat.checkSelfPermission(LocationComponentActivity.this , ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        client = LocationServices.getFusedLocationProviderClient(LocationComponentActivity.this);
        client.getLastLocation().addOnSuccessListener(LocationComponentActivity.this ,new OnSuccessListener<Location>()
        {

            @Override
            public void onSuccess(Location location) {
                if(location!=null)
                {
                    longitutde = location.getLongitude();
                    latitude = location.getLatitude();
                    Log.d("MyLatitude:::::>",""+latitude);
                    Log.d("MyLongitude:::::>",""+longitutde);
                    Toast.makeText(LocationComponentActivity.this, "Show Location "+latitude+" "+longitutde, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(LocationComponentActivity.this, "Turn on GPS"+location, Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(LocationComponentActivity.this, "Location not retrieved, ERROR!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void openPopUpWindow()
    {
        Intent i = new Intent(this, PopUpWindow.class);
        startActivity(i);
    }

    public void restartActivity()
    {
        Intent intent = getIntent();
        finish();
        startActivity(intent);
    }



    public void signOut(){
        mFirebaseAuth.signOut();
       openMain();
    }

    public void openMain(){
        Intent i = new Intent(LocationComponentActivity.this, MainActivity.class);
        startActivity(i);
    }


    //My Customize functions ends    //////////////////////////////////////////////

    @Override
    public void onMapReady(@NonNull final MapboxMap mapboxMap)
    {
        LocationComponentActivity.this.mapboxMap = mapboxMap;

        mapboxMap.setStyle(new Style.Builder().fromUri("mapbox://styles/mapbox/cjerxnqt3cgvp2rmyuxbeqme7"),
                new Style.OnStyleLoaded() {
                    @Override
                    public void onStyleLoaded(@NonNull Style style)
                    {
                        enableLocationComponent(style);
                    }
                });
    }

    @SuppressWarnings( {"MissingPermission"})
    private void enableLocationComponent(@NonNull Style loadedMapStyle)
    {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {

            // Get an instance of the component
            LocationComponent locationComponent = mapboxMap.getLocationComponent();

            // Activate with options
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, loadedMapStyle).build());

            // Enable to make component visible
            locationComponent.setLocationComponentEnabled(true);

            // Set the component's camera mode
            locationComponent.setCameraMode(CameraMode.TRACKING);

            // Set the component's render mode
            locationComponent.setRenderMode(RenderMode.COMPASS);
        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain)
    {
        Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted)
    {
        if (granted)
        {
            mapboxMap.getStyle(new Style.OnStyleLoaded()
            {
                @Override
                public void onStyleLoaded(@NonNull Style style)
                {
                    enableLocationComponent(style);
                }
            });
        }
        else
            {
            Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    @SuppressWarnings( {"MissingPermission"})
    protected void onStart()
    {
        super.onStart();
        mapView.onStart();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mapView.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState)
    {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory()
    {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onBackPressed()
    {
        Toast.makeText(LocationComponentActivity.this, "You need to stay on this page", Toast.LENGTH_SHORT).show();
    }


}