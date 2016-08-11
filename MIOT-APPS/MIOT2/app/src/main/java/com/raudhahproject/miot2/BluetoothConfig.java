package com.raudhahproject.miot2;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


public class BluetoothConfig extends ActionBarActivity {

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;

    private ListView devicelist;

    public static final String CONFIG_BTADDRESS = "miot.btaddress";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_config);

        devicelist = (ListView) findViewById(R.id.listView);

        initAdapter();
    }



    public void initAdapter(){
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        //check if such bluetooth does exist
        if(myBluetooth == null){
            //show user that no bluetooth device exist
            Toast.makeText(getApplicationContext(), "BLUETOOTH UNAVAILABLE IN YOUR DEVICE", Toast.LENGTH_LONG).show();

        }
        else{

            if(myBluetooth.isEnabled()){

            }
            else{
                //ask user to turn their bluetooth on
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }

        }
    }

    //show all the paired bluetooth
    public void pairBluetooth(View view){


        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if(pairedDevices.size() > 0){
            for(BluetoothDevice bt : pairedDevices){

                list.add(bt.getName() + "\n"+ bt.getAddress());

            }
        }
        else{
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        //populate to adapter
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemClickListener(myListClickListener);

    }

    private AdapterView.OnItemClickListener myListClickListener = new AdapterView.OnItemClickListener(){
        public void onItemClick (AdapterView av, View v, int arg2, long arg3){
            //get the mac address
            String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);        //address of the bluetooth

            Log.d("BLUETOOTH_CONFIG", "SAYA LAGI ADA DI BLUETOOTH CONFIG SAAT KAMU KLIK AKU....");
            //create intent to be redeived by the main..
            Intent intentReturn = new Intent();
            intentReturn.putExtra(CONFIG_BTADDRESS, address);
            setResult(MainActivity.BT_CONFIG_REQUEST, intentReturn);
            finish();
            //
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth_config, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
