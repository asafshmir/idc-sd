/*
 * Copyright (c) 2013 – 2015 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */
package at.bitfire.davdroid.ui.setup;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;

import at.bitfire.davdroid.R;
import at.bitfire.davdroid.crypto.KeyManager;

/**
 * A simple UI enabling the removal of users
 */
public class RemoveAccountActivity extends Activity {

    MyCustomAdapter dataAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_remove_account);

        //Generate list View from ArrayList
        displayListView();

    }

    private void displayListView() {

        //Array list of countries
        ArrayList<User> usersList = new ArrayList<User>();
        HashMap<String,Boolean> users = KeyManager.getInstance().getUsers();


        for (String userName : users.keySet())
        {
            Log.i("RemoveAccount",userName);
            Log.i("RemoveAccount","User Authorized " + users.get(userName));
            User user = new User(userName,users.get(userName));
            usersList.add(user);
        }

        //create an ArrayAdaptar from the String Array
        dataAdapter = new MyCustomAdapter(this,
                R.layout.setup_account_info, usersList);
        ListView listView = (ListView) findViewById(R.id.remove_list);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                User user = (User) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(),
                        "Clicked on Row: " + user.getName(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private class MyCustomAdapter extends ArrayAdapter<User> {

        private ArrayList<User> usersList;

        public MyCustomAdapter(Context context, int textViewResourceId,
                               ArrayList<User> usersList) {
            super(context, textViewResourceId, usersList);
            this.usersList = new ArrayList<User>();
            this.usersList.addAll(usersList);
        }

        private class ViewHolder {
            TextView code;
            CheckBox name;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            ViewHolder holder = null;
            Log.v("ConvertView", String.valueOf(position));

            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.setup_account_info, null);

                holder = new ViewHolder();
                //holder.code = (TextView) convertView.findViewById(R.id.code);
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
                convertView.setTag(holder);

                holder.name.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v ;
                        User user = (User) cb.getTag();
                        Toast.makeText(getApplicationContext(),
                                "Clicked on Checkbox: " + cb.getText() +
                                        " is " + cb.isChecked(),
                                Toast.LENGTH_LONG).show();
                        if (cb.isChecked()) {
                            KeyManager.getInstance().authUser(cb.getText().toString());
                        } else {
                            KeyManager.getInstance().removeUser(cb.getText().toString());
                        }
                        user.setSelected(cb.isChecked());
                    }
                });
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            User user = usersList.get(position);

            holder.name.setText(user.getName());
            holder.name.setChecked(user.isSelected());
            holder.name.setTag(user);

            return convertView;

        }

    }

    public class User {


        String name = null;
        boolean selected = false;

        public User(String name, boolean selected) {
            super();
            this.name = name;
            this.selected = selected;
        }

        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

        public boolean isSelected() {
            return selected;
        }
        public void setSelected(boolean selected) {
            this.selected = selected;
        }

    }
}