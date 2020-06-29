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

public class PendingRequestAdapter extends ArrayAdapter<PendingRequest>
{
    int Color;
    public PendingRequestAdapter(Activity context, ArrayList<PendingRequest> approvals, int color)
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
                    R.layout.approval_list_item, parent, false);
            Log.d("yahan","hoon yahan");
        }

        // Get the object located at this position in the list
        PendingRequest currentRequest= getItem(position);

        // Find the TextView in the list_item.xml layout with the ID version_name
        TextView userTextView = listItemView.findViewById(R.id.userName);
        userTextView.setText(currentRequest.getName());

        if(currentRequest.getType().equals("meal"))
        {
            TextView textView =listItemView.findViewById(R.id.servingslabel);
            textView.setText(R.string.servings);

            TextView servingsTextView = listItemView.findViewById(R.id.quantity);
            servingsTextView.setText(currentRequest.getServings());

            TextView textView2 =listItemView.findViewById(R.id.typelabel);
            textView2.setText(R.string.dastarkhwan);

            TextView dastarkhwanTextView = listItemView.findViewById(R.id.itemType);
            dastarkhwanTextView.setText(currentRequest.getItemName());
        }
        else
        {
            TextView servingsTextView = listItemView.findViewById(R.id.quantity);
            servingsTextView.setText(currentRequest.getServings());

            TextView dastarkhwanTextView = listItemView.findViewById(R.id.itemType);
            dastarkhwanTextView.setText(currentRequest.getType());

        }
        TextView numberTextView = listItemView.findViewById(R.id.phoneNumber);
        numberTextView.setText(currentRequest.getPhoneNumber());

        LinearLayout linearLayout = listItemView.findViewById(R.id.listItem);
        int bgColor = ContextCompat.getColor(getContext(),Color);
        linearLayout.setBackgroundColor(bgColor);

        return listItemView;
    }
}