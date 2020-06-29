package com.example.android.insaniyat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class WelfareDonationFragment extends Fragment 
{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String CHANNEL_ID = "001";

    private ProgressBar progressbar;
    private EditText itemType, quantity;
    private FusedLocationProviderClient client;
    private double latitude,longitude;
    
    private String mParam1;
    private String mParam2;

    public WelfareDonationFragment()
    {
        // Required empty public constructor
    }
    
    public static WelfareDonationFragment newInstance(String param1, String param2)
    {
        WelfareDonationFragment fragment = new WelfareDonationFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_welfare_donation, container, false);
        
        Button submitWelfare;
        submitWelfare = view.findViewById(R.id.submitWelfare);

        itemType = view.findViewById(R.id.itemType);
        quantity = view.findViewById(R.id.itemQuantity);

        progressbar = view.findViewById(R.id.progressbar);
        progressbar.setVisibility(View.GONE);
        requestPermission();
        client = LocationServices.getFusedLocationProviderClient(getActivity());

        submitWelfare.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                progressbar.setVisibility(View.VISIBLE);
                sendRequestWelfare();
            }
        });
        
        // Inflate the layout for getActivity() fragment
        return view;
    }

    private void sendRequestWelfare()
    {
        progressbar.setVisibility(View.VISIBLE);
        if (ActivityCompat.checkSelfPermission(getActivity(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            return;
        }

        client.getLastLocation().addOnSuccessListener(getActivity(), new OnSuccessListener<Location>()
        {
            @Override
            public void onSuccess(Location location)
            {
                if (location != null)
                {
                    progressbar.setVisibility(View.VISIBLE);
                    longitude = location.getLongitude();
                    latitude = location.getLatitude();
                    final GeoPoint pickupLocation  = new GeoPoint(latitude,longitude);
                    final String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
                    final FirebaseFirestore db = FirebaseFirestore.getInstance();
                    final DocumentReference userRef = db.collection("Users").document(""+currentUserEmail);

                    userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
                    {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot)
                        {
                            if(documentSnapshot.exists())
                            {
                                final DocumentReference pendingMealRequestCRef = db.collection("PendingWelfareRequests").document(""+documentSnapshot.getString("phoneNumber"));
                                pendingMealRequestCRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
                                {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot)
                                    {
                                        if(documentSnapshot.exists())
                                        {
                                            createNotificationChannel();
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.icon)
                                                    .setContentTitle("Welfare Donation.")
                                                    .setContentText("Wait for Previous request's completion! \nSwipe to Cancel.")
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                            // notificationId is a unique int for each notification that you must define
                                            int notificationId=1;
                                            notificationManager.notify(notificationId, builder.build());

                                            Toast.makeText(getActivity(),"Wait for Previous request's completion!", Toast.LENGTH_SHORT).show();
                                            quantity.setText("");
                                            progressbar.setVisibility(View.GONE);
                                        }
                                        else
                                        {
                                            if(itemType.getText().toString().isEmpty())
                                            {
                                                Toast.makeText(getActivity(),"Enter Item name", Toast.LENGTH_SHORT).show();
                                                itemType.requestFocus();
                                                progressbar.setVisibility(View.GONE);
                                            }
                                            else if(quantity.getText().toString().isEmpty())
                                            {
                                                Toast.makeText(getActivity(),"Enter Quantity", Toast.LENGTH_SHORT).show();
                                                quantity.requestFocus();
                                                progressbar.setVisibility(View.GONE);
                                            }
                                            else
                                            {
                                                userRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
                                                {
                                                    @Override
                                                    public void onSuccess(DocumentSnapshot documentSnapshot)
                                                    {
                                                        if(documentSnapshot.exists())
                                                        {
                                                            String name = documentSnapshot.getString("name");
                                                            String phn  = documentSnapshot.getString("phoneNumber");
                                                            String serving = quantity.getText().toString();
                                                            DonationRequest pendingDonationRequest = new DonationRequest(name,serving,phn,itemType.getText().toString(),false,pickupLocation);
                                                            pendingMealRequestCRef.set(pendingDonationRequest);

                                                            createNotificationChannel();
                                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                                                    .setSmallIcon(R.drawable.icon)
                                                                    .setContentTitle("Welfare Donation.")
                                                                    .setContentText("Wait for Welfare's Approval! \nSwipe to Cancel.")
                                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                                            // notificationId is a unique int for each notification that you must define
                                                            int notificationId=1;
                                                            notificationManager.notify(notificationId, builder.build());

                                                            Toast.makeText(getActivity(),"Wait for approval", Toast.LENGTH_SHORT).show();
                                                            quantity.setText("");
                                                            startActivity(new Intent(getActivity(), HomeActivity.class));
                                                            getActivity().finish();
                                                        }
                                                    }
                                                }).addOnFailureListener(new OnFailureListener()
                                                {
                                                    @Override
                                                    public void onFailure(@NonNull Exception e)
                                                    {
                                                        progressbar.setVisibility(View.GONE);
                                                    }
                                                });
                                            }

                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener()
                                {
                                    @Override
                                    public void onFailure(@NonNull Exception e)
                                    {
                                        progressbar.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }
                    }).addOnFailureListener(new OnFailureListener()
                    {
                        @Override
                        public void onFailure(@NonNull Exception e)
                        {
                            progressbar.setVisibility(View.GONE);
                        }
                    });
                }
                else
                {
                    progressbar.setVisibility(View.GONE);
                    Toast.makeText(getActivity(), "Turn on GPS Or some other issue" + location, Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                progressbar.setVisibility(View.GONE);
                Toast.makeText(getActivity(), "Location not retrieved, ERROR!", Toast.LENGTH_SHORT).show();
            }
        });

        progressbar.setVisibility(View.GONE);

    }

    private void requestPermission()
    {
        ActivityCompat.requestPermissions(getActivity(), new String[]{ACCESS_FINE_LOCATION}, 1);
    }

    private void createNotificationChannel()
    {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
