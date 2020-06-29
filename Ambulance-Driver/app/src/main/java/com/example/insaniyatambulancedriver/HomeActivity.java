package com.example.insaniyatambulancedriver;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        //open mapbox
        openMap();

        //hide title bar
        getSupportActionBar().hide();

        //pop up of enable GPS at the launch time of the application
        //GetGPS();
    }

    public void openMap()
    {
        Intent i = new Intent(this, LocationComponentActivity.class);
        startActivity(i);
    }
}