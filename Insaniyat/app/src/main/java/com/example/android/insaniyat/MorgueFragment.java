package com.example.android.insaniyat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class MorgueFragment extends Fragment
{
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final String CHANNEL_ID = "001";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public MorgueFragment()
    {
        // Required empty public constructor
    }
    // TODO: Rename and change types and number of parameters
    public static MorgueFragment newInstance(String param1, String param2) 
    {
        MorgueFragment fragment = new MorgueFragment();
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
        View view =inflater.inflate(R.layout.fragment_morgue, container, false);

        final EditText nameView, deadBodyNameView, dateView, phoneView;
        Button submit;
        final ProgressBar progressBar;

        nameView = view.findViewById(R.id.name);
        deadBodyNameView = view.findViewById(R.id.bodyName);
        dateView = view.findViewById(R.id.date);
        phoneView = view.findViewById(R.id.phone);
        submit = view.findViewById(R.id.submitWelfare);
        progressBar = view.findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);

        submit.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final String name = nameView.getText().toString();
                final String bodyName = deadBodyNameView.getText().toString();
                final String date = dateView.getText().toString();
                final String phone = phoneView.getText().toString();

                progressBar.setVisibility(View.VISIBLE);
                if (name.isEmpty() && bodyName.isEmpty() && date.isEmpty() && phone.isEmpty())
                {
                    progressBar.setVisibility(View.GONE);
                    nameView.setError("Fill all fields");
                    nameView.requestFocus();
                    deadBodyNameView.requestFocus();
                    dateView.requestFocus();
                    phoneView.requestFocus();
                }
                else if (name.isEmpty())
                {
                    progressBar.setVisibility(View.GONE);
                    nameView.setError("Enter name");
                    nameView.requestFocus();
                }
                else if (bodyName.isEmpty())
                {
                    progressBar.setVisibility(View.GONE);
                    deadBodyNameView.setError("Enter dead body name");
                    deadBodyNameView.requestFocus();
                } else if (date.isEmpty())
                {
                    progressBar.setVisibility(View.GONE);
                    dateView.setError("Enter date in dd/mm/yyyy format!");
                    dateView.requestFocus();
                }
                else if (phone.isEmpty())
                {
                    progressBar.setVisibility(View.GONE);
                    phoneView.setError("Enter phone number");
                    phoneView.requestFocus();
                }
                else
                    {
                    if (date.length() != 10)
                    {
                        progressBar.setVisibility(View.GONE);
                        dateView.setText("");
                        dateView.setError("Enter date in dd/mm/yyyy format!");
                        dateView.requestFocus();
                    }
                    else if (phone.length() != 11)
                    {
                        progressBar.setVisibility(View.GONE);
                        phoneView.setText("");
                        phoneView.setError("Enter correct phone number");
                        phoneView.requestFocus();
                    }
                    else
                        {
                        final FirebaseFirestore db = FirebaseFirestore.getInstance();
                        final DocumentReference pendingMorgueRequestCRef = db.collection("PendingMorgueRequests").document("" + phone);
                        pendingMorgueRequestCRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
                        {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot)
                            {
                                if (documentSnapshot.exists())
                                {
                                    createNotificationChannel();
                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                            .setSmallIcon(R.drawable.icon)
                                            .setContentTitle("Insaniyat Morgue.")
                                            .setContentText("Wait for Previous Request's Completion! \nSwipe to Cancel.")
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                    // notificationId is a unique int for each notification that you must define
                                    int notificationId=1;
                                    notificationManager.notify(notificationId, builder.build());

                                    Toast.makeText(getActivity(), "Wait for previous request's completion", Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.GONE);
                                }
                                else
                                    {
                                    MorgueRequest morgueRequest = new MorgueRequest(name, bodyName, phone, date);
                                    pendingMorgueRequestCRef.set(morgueRequest);
                                        createNotificationChannel();
                                        NotificationCompat.Builder builder = new NotificationCompat.Builder(getActivity(), CHANNEL_ID)
                                                .setSmallIcon(R.drawable.icon)
                                                .setContentTitle("Insaniyat Morgue.")
                                                .setContentText("Wait for Approval! \nSwipe to Cancel.")
                                                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getActivity());

                                        // notificationId is a unique int for each notification that you must define
                                        int notificationId=1;
                                        notificationManager.notify(notificationId, builder.build());

                                    Toast.makeText(getActivity(), "Wait for approval!", Toast.LENGTH_SHORT).show();
                                    getActivity().finish();
                                    startActivity(new Intent(getActivity(), HomeActivity.class));
                                }
                            }
                        });
                    }
                }
            }
        });
        // Inflate the layout for this fragment
        return view;
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
