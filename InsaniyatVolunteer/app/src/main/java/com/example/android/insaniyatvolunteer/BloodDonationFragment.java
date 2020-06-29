package com.example.android.insaniyatvolunteer;

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
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

import static androidx.core.content.PermissionChecker.checkSelfPermission;

public class BloodDonationFragment extends Fragment
{
    public BloodDonationFragment()
    {
        // Required empty public constructor
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
        checkForSmsPermission();
        View view = inflater.inflate(R.layout.fragment_blood_donation, container, false);

//        ImageView refresh = view.findViewById(R.id.refresh);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference BloodRef = db.collection("BloodRequests");
        final CollectionReference currentVolunteerCollection = db.collection("Volunteers");
        final String[] currentUserName = new String[1];
        final BloodRequest[] newDonation = {new BloodRequest()};
        final DocumentReference currentVolunteer = currentVolunteerCollection.document(FirebaseAuth.getInstance().getCurrentUser().getEmail());

        final String[] bloodGrp = {""};
        final ArrayList<BloodRequest> list = new ArrayList<BloodRequest>();
        final BloodRequestAdapter listAdapter = new BloodRequestAdapter(getActivity(),list,R.color.darkred);
        final ListView listView = view.findViewById(R.id.BloodList);
        listView.setAdapter(listAdapter);

        currentVolunteer.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
        {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot)
            {
                currentUserName[0] = documentSnapshot.getString("name");
                bloodGrp[0] =documentSnapshot.getString("bloodGroup");
            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {

            }
        });

        BloodRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>()
        {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots)
            {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                {
                    String username = documentSnapshot.getString("name");
                    String phn = documentSnapshot.getString("phonenumber");
                    String type = documentSnapshot.getString("type");
                    String bldGrp = documentSnapshot.getString("bloodGroup");
                    GeoPoint loc = documentSnapshot.getGeoPoint("pickupLocation");
                    BloodRequest newApproval = new BloodRequest(username,bldGrp, phn,type, loc);
                    if(bloodGrp[0].equalsIgnoreCase(bldGrp))
                    {
                        list.add(newApproval);
                        listView.setAdapter(listAdapter);
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
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                final BloodRequest newApproval = list.get(position);
                btn_showDialog(newApproval,"blood",currentUserName[0]);
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

    public void btn_showDialog(final BloodRequest newApproval, final String type, final String currentUserName)
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        View mView = getLayoutInflater().inflate(R.layout.approval_custom_dialog, null);

        Button callButton,locationButton,cancelButton,acceptButton,completedButton;
        callButton = mView.findViewById(R.id.call);
        acceptButton = mView.findViewById(R.id.accepted);
        locationButton = mView.findViewById(R.id.location);
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
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED)
                {
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CALL_PHONE},1);
                }
                else
                {
                    Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + newApproval.getPhonenumber()));
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

        acceptButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Request")
                        .setMessage("Do you want to accept this request?")
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int which)
                            {
                                refresh();

                                try
                                {
                                    SmsManager smgr = SmsManager.getDefault();
                                    smgr.sendTextMessage(newApproval.getPhonenumber(),null,"We have found a donor who has accepted your request.\nName: "+currentUserName+".\nContact on this number for further information.",null,null);
                                    Toast.makeText(getActivity(), "SMS Sent Successfully", Toast.LENGTH_SHORT).show();

                                    createNotificationChannel();
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                            .setSmallIcon(R.drawable.kindness)
                                            .setContentTitle("Insaniyat Volunteer Blood donation.")
                                            .setContentText("Request Accepted! \nSwipe to Cancel.")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                    // notificationId is a unique int for each notification that you must define
                                    int notificationId=1;
                                    notificationManager.notify(notificationId, builder.build());

                                    final CollectionReference mealDonationsRef= FirebaseFirestore.getInstance().collection("BloodRequests");
                                    mealDonationsRef.document(newApproval.getPhonenumber()).delete();

                                    startActivity(new Intent(getActivity(),HomeActivity.class));

                                    refresh();
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(getActivity(), "SMS Failed to Send, Please try again", Toast.LENGTH_SHORT).show();
                                }

                                final CollectionReference pendingRequestsRef = FirebaseFirestore.getInstance().collection("PendingVolunteerRequests");
                                pendingRequestsRef.document(""+newApproval.getPhonenumber()).delete();
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

    }

    private void checkForSmsPermission()
    {
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{Manifest.permission.SEND_SMS},
                    1);
        }
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
