package com.example.insaniyatambulancedriver;


public class User
{
    public String email,password,name,phoneNumber,role,status;

    public Double latitude,longitude;

    public User()
    {

    }



    public User(String name, String phone, String email, String password,Double latitude,Double longitude)
    {
        this.name=name;
        this.phoneNumber=phone;
        this.email = email;
        this.password=password;
        this.role="Ambulance Driver";
        this.latitude = latitude;
        this.longitude = longitude;
        this.status="Unvailable";
    }

    public String getEmail()
    {
        return email;
    }

    public String getPassword()
    {
        return password;
    }

    public String getName()
    {
        return name;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public String getRole()
    {
        return role;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }
}