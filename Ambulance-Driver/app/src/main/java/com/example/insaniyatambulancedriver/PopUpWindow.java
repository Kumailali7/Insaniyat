package com.example.insaniyatambulancedriver;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PopUpWindow extends Activity
{

    private FirebaseFirestore mFireStore;
    private String assignedDocID;

    private static final String CHANNEL_ID = "001";

    MediaPlayer alert;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pop_up_window);

        //play alert music
        alert =  MediaPlayer.create(PopUpWindow.this,R.raw.sms);
        alert.start();

        display();

        Button RideAccept = findViewById(R.id.accept);
        //Button RideReject = findViewById(R.id.reject);

        RideAccept.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                //Show details of customer
                sms();
                //music stops
                onPause();

                //remove the pop-up screen
                finish();
            }
        });
    }

    public void display()
    {
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        getWindow().setLayout((int)(width * .8), (int) (height * .4));

    }

    public void sms()
    {
        //U_PhoneNumber
        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        final CollectionReference collRef = db.collection("AssignedRides");
//        final DocumentReference userRef = .document(""+currentUserEmail);

        collRef.whereEqualTo("AmbulanceDriverID", ""+FirebaseAuth.getInstance().getCurrentUser().getEmail())
                            .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>()
        {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task)
            {
                if (task.isSuccessful())
                {
                    for (QueryDocumentSnapshot document : task.getResult())
                    {
                        assignedDocID = document.getId();
                        String number = document.getData().get("U_PhoneNumber").toString();
                        String d_name = document.getData().get("D_Name").toString();
                        String d_number = document.getData().get("D_PhoneNumber").toString();
                        try
                        {
                            checkForPhonePermission();
                            SmsManager smgr = SmsManager.getDefault();
//                            Toast.makeText(PopUpWindow.this, "D num : "+d_number +"\nU num : "+number, Toast.LENGTH_SHORT).show();
                            createNotificationChannel();
                            NotificationCompat.Builder builder = new NotificationCompat.Builder(PopUpWindow.this, CHANNEL_ID)
                                    .setSmallIcon(R.drawable.ambulance)
                                    .setContentTitle("Ambulance Confirmed.")
                                    .setContentText("Contact Number : "+number+" \nSwipe to Cancel.")
                                    .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(PopUpWindow.this);

                            // notificationId is a unique int for each notification that you must define
                            int notificationId=1;
                            notificationManager.notify(notificationId, builder.build());

                            NotificationManager notificationManager1 = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                            notificationManager1.notify(0,builder.build());
                            smgr.sendTextMessage(number,null,"Your  driver "+d_name+" is on his way.\nYou can contact him on : "+d_number,null,null);
                        }
                        catch (Exception e)
                        {
                            Toast.makeText(PopUpWindow.this, "D num : "+d_number +"\nU num : "+number, Toast.LENGTH_SHORT).show();
                            Log.d("SMS : ",""+e.toString());
                            Toast.makeText(PopUpWindow.this, "SMS Failed to Send! Give permissions or recharge your account.", Toast.LENGTH_SHORT).show();
                        }
                    }

                }
                else
                    {
                    Log.d("Status","Could not fetched doc Id");
                    Log.d("Status","Error getting documents: "+task.getException());
                }
            }

        });

    }

    @Override
    public void onPause ()
    {
        if (alert != null)
        {
            alert.pause();
            alert.stop();
        }
        super.onPause();
    }

    private void checkForPhonePermission()
    {
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    1);
        }
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) !=
                PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
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
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}

