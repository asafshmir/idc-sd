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


public class RemoveAccountActivity extends Activity {

    MyCustomAdapter dataAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_remove_account);

        //Generate list View from ArrayList
        displayListView();

        checkButtonClick();

    }

    private void displayListView() {

        //Array list of countries
        ArrayList<Account> accountList = new ArrayList<Account>();
        HashMap<String,Boolean> users = KeyManager.getInstance().getUsers();


        for (String userName : users.keySet())
        {
            Log.i("RemoveAccount",userName);
            Account account = new Account(userName,userName,users.get(userName));
            accountList.add(account);
        }

        //create an ArrayAdaptar from the String Array
        dataAdapter = new MyCustomAdapter(this,
                R.layout.setup_account_info, accountList);
        ListView listView = (ListView) findViewById(R.id.remove_list);
        // Assign adapter to ListView
        listView.setAdapter(dataAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // When clicked, show a toast with the TextView text
                Account account = (Account) parent.getItemAtPosition(position);
                Toast.makeText(getApplicationContext(),
                        "Clicked on Row: " + account.getName(),
                        Toast.LENGTH_LONG).show();
            }
        });

    }

    private class MyCustomAdapter extends ArrayAdapter<Account> {

        private ArrayList<Account> accountList;

        public MyCustomAdapter(Context context, int textViewResourceId,
                               ArrayList<Account> accountList) {
            super(context, textViewResourceId, accountList);
            this.accountList = new ArrayList<Account>();
            this.accountList.addAll(accountList);
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
                holder.code = (TextView) convertView.findViewById(R.id.code);
                holder.name = (CheckBox) convertView.findViewById(R.id.checkBox1);
                convertView.setTag(holder);

                holder.name.setOnClickListener( new View.OnClickListener() {
                    public void onClick(View v) {
                        CheckBox cb = (CheckBox) v ;
                        Account account = (Account) cb.getTag();
                        Toast.makeText(getApplicationContext(),
                                "Clicked on Checkbox: " + cb.getText() +
                                        " is " + cb.isChecked(),
                                Toast.LENGTH_LONG).show();
                        if (cb.isChecked()) {
                            KeyManager.getInstance().authUser(account.getName());
                        } else {
                            KeyManager.getInstance().removeUser(account.getName());
                        }
                        account.setSelected(cb.isChecked());
                    }
                });
            }
            else {
                holder = (ViewHolder) convertView.getTag();
            }

            Account account = accountList.get(position);
            holder.code.setText(" (" +  account.getCode() + ")");
            holder.name.setText(account.getName());
            holder.name.setChecked(account.isSelected());
            holder.name.setTag(account);

            return convertView;

        }

    }

    private void checkButtonClick() {

//
//        Button myButton = (Button) findViewById(R.id.findSelected);
//        myButton.setOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//
//                StringBuffer responseText = new StringBuffer();
//                responseText.append("The following were selected...\n");
//
//                ArrayList<Account> countryList = dataAdapter.accountList;
//                for (int i = 0; i < countryList.size(); i++) {
//                    Account country = countryList.get(i);
//                    if (country.isSelected()) {
//                        responseText.append("\n" + country.getName());
//                    }
//                }
//
//                Toast.makeText(getApplicationContext(),
//                        responseText, Toast.LENGTH_LONG).show();
//
//            }
//        });

    }

    public class Account {

        String code = null;
        String name = null;
        boolean selected = false;

        public Account(String code, String name, boolean selected) {
            super();
            this.code = code;
            this.name = name;
            this.selected = selected;
        }

        public String getCode() {
            return code;
        }
        public void setCode(String code) {
            this.code = code;
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