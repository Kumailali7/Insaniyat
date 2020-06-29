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

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
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

import java.util.ArrayList;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class BloodDonationFragment extends Fragment implements AdapterView.OnItemSelectedListener
{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String CHANNEL_ID = "001";

    private Button home,logout,submit;
    private Spinner bloodGroup;
    public ProgressBar progressbar;
    private String selected="";
    private double latitude,longitude;
    private FusedLocationProviderClient client;

    private String mParam1;
    private String mParam2;

    public BloodDonationFragment()
    {

    }

    public static BloodDonationFragment newInstance(String param1, String param2)
    {
        BloodDonationFragment fragment = new BloodDonationFragment();
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
        View view = inflater.inflate(R.layout.fragment_blood_donation, container, false);

        submit = view.findViewById(R.id.Submit);
        bloodGroup = view.findViewById(R.id.BloodGroupSpinner);
        progressbar = view.findViewById(R.id.progressbar);
        progressbar.setVisibility(View.GONE);
        client = LocationServices.getFusedLocationProviderClient(getActivity());

        final ArrayList<String> customList = new ArrayList<>();

        customList.add("Select Blood Group");
        customList.add("A+");
        customList.add("A-");
        customList.add("AB+");
        customList.add("AB-");
        customList.add("B+");
        customList.add("B-");
        customList.add("0+");
        customList.add("0-");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, customList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        CustomBloodGroupSpinnerAdapter customAdapter = new CustomBloodGroupSpinnerAdapter(getActivity(), customList);
        bloodGroup.setAdapter(customAdapter);
        if (bloodGroup!= null)
        {
//            bloodGroup.setAdapter(customAdapter);
            bloodGroup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
            {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
                {
                    selected=(String) parent.getItemAtPosition(position);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent)
                {

                }
            });
        }
        submit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if(selected.equalsIgnoreCase("select blood group"))
                {
                    Toast.makeText(getActivity(), "Incorrect Blood Group!", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    sendRequest(selected);
                }
            }
        });
        return view;
    }

    private void sendRequest(final String selected)
    {
        progressbar.setVisibility(View.VISIBLE);

        if (ActivityCompat.checkSelfPermission(getActivity(), ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            Toast.makeText(getActivity(), "Give Location Permission!", Toast.LENGTH_SHORT).show();
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
                                final DocumentReference bloodRequestCRef = db.collection("BloodRequests").document(""+documentSnapshot.getString("phoneNumber"));
                                bloodRequestCRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
                                {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot)
                                    {
                                        if(documentSnapshot.exists())
                                        {
                                            createNotificationChannel();
                                            NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                                    .setSmallIcon(R.drawable.ic_blood)
                                                    .setContentTitle("Insaniyat Blood Donation!")
                                                    .setContentText("Wait for Previous Request's Approval! \nSwipe to Cancel.")
                                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                            // notificationId is a unique int for each notification that you must define
                                            int notificationId=1;
                                            notificationManager.notify(notificationId, builder.build());

                                            NotificationManager notificationManager1 = (NotificationManager)getActivity().getSystemService(Context.NOTIFICATION_SERVICE);
                                            notificationManager1.notify(0,builder.build());
//                                            servings.setText("");
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
                                                        String bldGrp = selected;
                                                        BloodRequest bloodRequest = new BloodRequest(name,bldGrp,phn,"blood",pickupLocation);
                                                        bloodRequestCRef.set(bloodRequest);

                                                        createNotificationChannel();
                                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                                                .setSmallIcon(R.drawable.ic_blood)
                                                                .setContentTitle("Insaniyat Blood Donation.")
                                                                .setContentText("Wait for Volunteer's Approval! \nSwipe to Cancel.")
                                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                                        // notificationId is a unique int for each notification that you must define
                                                        int notificationId=1;
                                                        notificationManager.notify(notificationId, builder.build());

                                                        getActivity().finish();
                                                        startActivity(new Intent(getActivity(), HomeActivity.class));
                                                        progressbar.setVisibility(View.GONE);
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
                                            progressbar.setVisibility(View.GONE);

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
                    Toast.makeText(getActivity(), "Turn on GPS Or give permissions! " + location, Toast.LENGTH_SHORT).show();
                    progressbar.setVisibility(View.GONE);
                }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {
                Toast.makeText(getActivity(), "Location not retrieved, ERROR!", Toast.LENGTH_SHORT).show();
                progressbar.setVisibility(View.GONE);
            }
        });

        progressbar.setVisibility(View.GONE);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
    {
        String items = (String) parent.getSelectedItem();
        selected = items;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent)
    {

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