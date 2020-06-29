package com.example.insaniyatambulancedriver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SplashActivity extends AppCompatActivity
{
    private FirebaseAuth mFirebaseAuth;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    boolean loggedIn;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        getSupportActionBar().hide();

        mFirebaseAuth = FirebaseAuth.getInstance();
        final FirebaseUser mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if(mFirebaseUser!=null && mFirebaseUser.isEmailVerified())
        {
            DocumentReference drefUser = db.collection("Ambulance Drivers").document(""+mFirebaseUser.getEmail());
            drefUser.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>()
            {
                @Override
                public void onSuccess(DocumentSnapshot documentSnapshot)
                {
                    if(documentSnapshot.exists())
                    {
                        Toast.makeText(SplashActivity.this,"Driver Logged In", Toast.LENGTH_SHORT).show();
                        loggedIn=true;
                    }
                }
            }).addOnFailureListener(new OnFailureListener()
            {
                @Override
                public void onFailure(@NonNull Exception e)
                {
                    Toast.makeText(SplashActivity.this,"Error Occurred!", Toast.LENGTH_SHORT).show();
                    Log.d("Session error: ",""+e);
                    loggedIn=false;
                }
            });

        }
        else
        {
            loggedIn=false;
        }

        Thread myThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    sleep(2500);
                    if(loggedIn)
                    {
                        Intent intent = getIntent();
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        finish();
                        Intent intToHome= new Intent(SplashActivity.this, HomeActivity.class);
                        startActivity(intToHome);
                    }
                    else
                        {
                            Intent intent = getIntent();
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            finish();
                            Intent intent1= new Intent(SplashActivity.this, MainActivity.class);
                            startActivity(intent1);
                    }
                    finish();
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                super.run();
            }
        };
        myThread.start();

    }
}