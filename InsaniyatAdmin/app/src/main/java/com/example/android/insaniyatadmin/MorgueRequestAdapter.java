package com.example.android.insaniyatadmin;

import android.app.Activity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MorgueRequestAdapter extends ArrayAdapter<MorgueRequest>
{
    int Color;
    public MorgueRequestAdapter(Activity context, ArrayList<MorgueRequest> approvals, int color)
    {
        super(context, 0, approvals);
        Color=color;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        // Check if the existing view is being reused, otherwise inflate the view
        View listItemView = convertView;
        if(listItemView == null)
        {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.morgue_list_item, parent, false);
            Log.d("yahan","hoon yahan");
        }

        // Get the object located at this position in the list
        MorgueRequest currentRequest= getItem(position);

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView userTextView = listItemView.findViewById(R.id.username);
        userTextView.setText(currentRequest.getName());

        TextView bodyNameTextView = listItemView.findViewById(R.id.deadBodyName);
        bodyNameTextView.setText(currentRequest.getBodyName());

        TextView itemTypeTextView = listItemView.findViewById(R.id.date);
        itemTypeTextView.setText(currentRequest.getDate());

        TextView numberTextView = listItemView.findViewById(R.id.phoneNumber);
        numberTextView.setText(currentRequest.getPhoneNumber());

        LinearLayout linearLayout = listItemView.findViewById(R.id.moneyListItem);
        int bgColor = ContextCompat.getColor(getContext(),Color);
        linearLayout.setBackgroundColor(bgColor);

        return listItemView;
    }
}