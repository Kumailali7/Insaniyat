package com.example.android.insaniyatadmin.ui.home;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.android.insaniyatadmin.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class HomeFragment extends Fragment
{
    private HomeViewModel homeViewModel;

    private static final String CHANNEL_ID = "001";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {

        homeViewModel =
                ViewModelProviders.of(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);

        createNotificationChannel();

        final FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference pending = db.collection("PendingWelfareRequests");

        final int[] nots = {0};

        pending.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>()
        {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots)
            {
                for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots)
                {
                   nots[0]++;
                }
                if(nots[0]>0)
                {
                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                            .setSmallIcon(R.drawable.icon)
                            .setContentTitle("Pending Approvals!")
                            .setContentText("You have pending approvals. \nSwipe to Cancel")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                    // notificationId is a unique int for each notification that you must define
                    int notificationId=0;
                    notificationManager.notify(notificationId, builder.build());
                }

            }
        }).addOnFailureListener(new OnFailureListener()
        {
            @Override
            public void onFailure(@NonNull Exception e)
            {

            }
        });

        return root;
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
