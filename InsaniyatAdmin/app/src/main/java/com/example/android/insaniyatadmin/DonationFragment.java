package com.example.android.insaniyatadmin;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;


public class DonationFragment extends Fragment
{
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private String mParam1;
    private String mParam2;

    public DonationFragment() {
        // Required empty public constructor
    }

    public static DonationFragment newInstance(String param1, String param2) {
        DonationFragment fragment = new DonationFragment();
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

    }

    private static final String CHANNEL_ID = "001";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        View view =inflater.inflate(R.layout.fragment_donation, container, false);

        ImageView refresh;
//        refresh = view.findViewById(R.id.refresh);

        final ArrayList<DonationRequest> list = new ArrayList<DonationRequest>();
        final DonationRequestAdapter listAdapter = new DonationRequestAdapter(getActivity(),list,R.color.darkskyblue);
        final ListView listView = view.findViewById(R.id.donationsList);
        listView.setAdapter(listAdapter);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference DonationsRef = db.collection("WelfareDonations");

        final String[] currentUserName = new String[1];
        final DonationRequest[] newDonation = {new DonationRequest()};

        DonationsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                {
                    if (documentSnapshot.getBoolean("collected").equals(false))
                    {
                        String username = documentSnapshot.getString("name");
                        String servings = documentSnapshot.getString("quantity");
                        String phn = documentSnapshot.getString("phoneNumber");
                        String type = documentSnapshot.getString("type");
                        GeoPoint loc = documentSnapshot.getGeoPoint("pickupLocation");
                        DonationRequest newApproval = new DonationRequest(username, servings, phn,type,true, loc);
                        currentUserName[0] = username;
                        list.add(newApproval);
                        listView.setAdapter(listAdapter);
                        newDonation[0] = newApproval;
                    }
                }
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final DonationRequest newApproval = list.get(position);
                btn_showDialog(newApproval);
            }
        });

//        refresh.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                refresh();
//            }
//        });
        return view;
    }

    public void btn_showDialog(final DonationRequest newApproval) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        View mView = getLayoutInflater().inflate(R.layout.action_custom_dialog, null);

        Button callButton, locationButton, completedButton, cancelButton;
        callButton = mView.findViewById(R.id.call);
        locationButton = mView.findViewById(R.id.location);
        completedButton = mView.findViewById(R.id.accepted);
        cancelButton = mView.findViewById(R.id.cancel);

        alert.setView(mView);

        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(true);

        callButton.setOnClickListener(new View.OnClickListener()
        {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view)
            {
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + newApproval.getPhoneNumber()));
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CALL_PHONE},1);
                }
                else
                {
                    startActivity(intent);
                }
            }
        });

        locationButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                GeoPoint pickupLocation = newApproval.getPickupLocation();
                double latitude = pickupLocation.getLatitude();
                double longitude = pickupLocation.getLongitude();
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query="+latitude+","+longitude));
                startActivity(intent);
                alertDialog.dismiss();
            }
        });

        completedButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Request Completion")
                        .setMessage("Have you completed this request?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                final CollectionReference DonationsRef= FirebaseFirestore.getInstance().collection("WelfareDonations");
                                DonationsRef.document(newApproval.getPhoneNumber()).delete();
                                refresh();
                            }
                        })

                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i)
                            {
                                alertDialog.dismiss();
                                refresh();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                alertDialog.dismiss();
            }
        });

        alertDialog.show();
    }

    private void refresh()
    {
        getActivity().finish();
        startActivity(getActivity().getIntent());
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
