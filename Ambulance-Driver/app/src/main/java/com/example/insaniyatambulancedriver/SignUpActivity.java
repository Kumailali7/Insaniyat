
package com.example.insaniyatambulancedriver;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;


public class SignUpActivity extends AppCompatActivity
{
    //new
    private FusedLocationProviderClient client;
    double latitude;
    double longitutde;


    private EditText name,phoneNumber, emailId,password;
    private Button signup;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseAuth mFirebaseAuth;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);
        mFirebaseAuth = FirebaseAuth.getInstance();
        getSupportActionBar().setTitle("Driver SignUp");
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.darkblue)));

        name=findViewById(R.id.signupName);
        phoneNumber=findViewById(R.id.signupPhone);
        emailId=findViewById(R.id.signupEmail);
        password=findViewById(R.id.signupPassword);
        signup=findViewById(R.id.signupSubmit);
        progressBar = findViewById(R.id.progressbar);
        progressBar.setVisibility(View.GONE);

        FINDLOC();

        signup.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                addUser();
            }
        });
    }

    void addUser()
    {
        String naam = name.getText().toString();
        String phone = phoneNumber.getText().toString();
        final String email = emailId.getText().toString();
        String pwd = password.getText().toString();
        final User user = new User(naam,phone,email,pwd,latitude,longitutde);


        if(email.isEmpty() && pwd.isEmpty())
        {
            Toast.makeText(SignUpActivity.this,"Fields are empty",Toast.LENGTH_SHORT);
            emailId.requestFocus();
            password.requestFocus();
        }
        else if(email.isEmpty())
        {
            emailId.setError("Please enter email id");
            emailId.requestFocus();
        }
        else if(pwd.isEmpty())
        {
            password.setError("Please enter password!");
            password.requestFocus();
        }
        else
        {
            progressBar.setVisibility(View.VISIBLE);
            mFirebaseAuth.createUserWithEmailAndPassword(email,pwd).addOnCompleteListener(SignUpActivity.this, new OnCompleteListener<AuthResult>()
            {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task)
                {
                    progressBar.setVisibility(View.GONE);
                    if(task.isSuccessful())
                    {
                        mFirebaseAuth.getCurrentUser().sendEmailVerification()
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task)
                                    {
                                        if(task.isSuccessful())
                                        {
                                            Toast.makeText(SignUpActivity.this,
                                                    "Verify email!", //ADD THIS
                                                    Toast.LENGTH_SHORT).show();
                                            db.collection("Ambulance Drivers").document(""+email).set(user);
                                            emailId.setText("");
                                            password.setText("");
                                            FirebaseAuth.getInstance().signOut();
                                            Intent i = new Intent(SignUpActivity.this, SignInActivity.class);
                                            switchActivity(i);
                                        }
                                        else
                                        {
                                            Toast.makeText(SignUpActivity.this,
                                                    "Signup unsuccessful : " + task.getException().getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                            Log.d("Exception in auth: ",""+task.getException().getMessage());
                                        }
                                    }
                                });
                    }
                    else
                    {
                        Toast.makeText(SignUpActivity.this,
                                "Signup unsuccessful: " + task.getException().getMessage(), //ADD THIS
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void switchActivity(Intent i)
    {
        Intent intent = getIntent();
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        finish();
        startActivity(i);
    }



    @Override
    public void onBackPressed()
    {
        finish();
        startActivity(new Intent(SignUpActivity.this,MainActivity.class));
    }


    public void FINDLOC(){

        if(ActivityCompat.checkSelfPermission(SignUpActivity.this , ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            return;
        }

        client = LocationServices.getFusedLocationProviderClient(SignUpActivity.this);
        client.getLastLocation().addOnSuccessListener(SignUpActivity.this ,new OnSuccessListener<Location>()
        {

            @Override
            public void onSuccess(Location location)
            {
                if(location!=null)
                {
                    longitutde = location.getLongitude();
                    latitude = location.getLatitude();
                    Log.d("MyLatitude:::::>",""+latitude);
                    Log.d("MyLongitude:::::>",""+longitutde);
                    //Toast.makeText(SignUpActivity.this, "Show Location "+latitude+" "+longitutde, Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(SignUpActivity.this, "Turn on GPS Or some other issue"+location, Toast.LENGTH_SHORT).show();
                }

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(SignUpActivity.this, "Location not retrieved, ERROR!", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
