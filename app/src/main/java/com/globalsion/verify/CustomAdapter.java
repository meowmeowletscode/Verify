package com.globalsion.verify;


import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SimpleAdapter;

import com.globalsion.verify.helpers.DBHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomAdapter extends SimpleAdapter {

    private List<? extends Map<String, ?>> dataList; // Member variable to hold data

    public CustomAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
        super(context, data, resource, from, to);
        this.dataList = data; // Assign the data list to the member variable
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        HashMap<String, String> item = (HashMap<String, String>) getItem(position);

        CheckBox checkBox = view.findViewById(R.id.checkbox);

        if (item != null && checkBox != null) {
            boolean isChecked = Boolean.parseBoolean(item.get(DBHelper.Status));
            checkBox.setChecked(isChecked);
        }

        return view;
    }

    public void updateItemStatus(String code, boolean isChecked) {
        for (int i = 0; i < dataList.size(); i++) {
            HashMap<String, String> item = (HashMap<String, String>) getItem(i);
            if (item != null && item.get(DBHelper.Code).equals(code)) {
                item.put(DBHelper.Status, String.valueOf(isChecked));
                notifyDataSetChanged(); // Notify the adapter that the data has changed
                break;
            }
        }
    }
}
