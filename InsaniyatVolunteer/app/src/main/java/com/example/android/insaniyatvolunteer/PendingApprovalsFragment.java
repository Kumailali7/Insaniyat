package com.example.android.insaniyatvolunteer;

import android.Manifest;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
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
import androidx.fragment.app.FragmentActivity;

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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class PendingApprovalsFragment extends Fragment
{
    public PendingApprovalsFragment() 
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
        View view =inflater.inflate(R.layout.fragment_pending_approvals, container, false);
        checkForSmsPermission();

        final ArrayList<PendingRequest> list = new ArrayList<>();
        final PendingRequestAdapter listAdapter = new PendingRequestAdapter(getActivity(),list,R.color.darkred);
        final ListView listView = view.findViewById(R.id.approvalsList);
        listView.setAdapter(listAdapter);

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference pendingRequestsRef=db.collection("PendingVolunteerRequests");
        final String[] currentUserName = new String[1];

        pendingRequestsRef.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>()
        {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots)
            {
                for(QueryDocumentSnapshot documentSnapshot: queryDocumentSnapshots)
                {
                    String username = documentSnapshot.getString("name");
                    String quantity = documentSnapshot.getString("quantity");
                    String phn = documentSnapshot.getString("phoneNumber");
                    String type = documentSnapshot.getString("type");
                    GeoPoint loc = documentSnapshot.getGeoPoint("location");
                    PendingRequest newApproval = new PendingRequest(username,quantity,phn,type,loc);
                    currentUserName[0]=username;
                    list.add(newApproval);
                    listView.setAdapter(listAdapter);
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
                final PendingRequest newApproval = list.get(position);
                btn_showDialog(newApproval,newApproval.getType(),currentUserName[0]);
            }
        });

        return view;
    }

    public void btn_showDialog(final PendingRequest newApproval, final String type, final String currentUserName)
    {
        final AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        View mView = getLayoutInflater().inflate(R.layout.approval_custom_dialog, null);

        Button callButton,locationButton,cancelButton,acceptButton;
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
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + newApproval.getPhonenumber()));
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
                                    smgr.sendTextMessage(newApproval.getPhonenumber(),null,"Your "+type +" donation request has been accepted by : "+currentUserName+".\nContact on this number for further information.",null,null);

                                    createNotificationChannel();
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                            .setSmallIcon(R.drawable.kindness)
                                            .setContentTitle("Insaniyat Volunteer.")
                                            .setContentText("Request Accepted! \nSwipe to Cancel.")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                    // notificationId is a unique int for each notification that you must define
                                    int notificationId=1;
                                    notificationManager.notify(notificationId, builder.build());

                                    Toast.makeText(getActivity(), "SMS Sent Successfully", Toast.LENGTH_SHORT).show();
                                    getActivity().finish();
                                    startActivity(new Intent(getActivity(),HomeActivity.class));
                                }
                                catch (Exception e)
                                {
                                    Toast.makeText(getActivity(), "SMS Failed to Send, Please give permissions and try again!", Toast.LENGTH_SHORT).show();
                                }
                                if(newApproval.getType().equals("meal"))
                                {
                                    MealRequest newRequest = new MealRequest(newApproval.getName(),newApproval.getQuantity(),newApproval.phonenumber,newApproval.getType(),newApproval.getPickupLocation(),true);
                                    newRequest.setvEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                    CollectionReference DastarkhwanRef = FirebaseFirestore.getInstance().collection("mealDonation");
                                    DastarkhwanRef.document(""+newApproval.getPhonenumber()).set(newRequest);
                                }
                                else
                                {
                                    DonationRequest newRequest = new DonationRequest(newApproval.getName(),newApproval.getQuantity(),newApproval.phonenumber,newApproval.getType(),true,newApproval.pickupLocation);
                                    newRequest.setvEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                                    CollectionReference WelfareRef = FirebaseFirestore.getInstance().collection("VolunteerDonations");
                                    WelfareRef.document(""+newApproval.getPhonenumber()).set(newRequest);
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