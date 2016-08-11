package com.raudhahproject.miot2;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;


public class ServerConfig extends ActionBarActivity {

    public static final String CONFIG_IP    = "miot.ip";
    public static final String CONFIG_PORT  = "miot.port";
    public static final String CONFIG_ID    = "miot.id";
    public static final String CONFIG_GROUP = "miot.group";

    EditText etIp, etPort, etId, etGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_config);

        etIp =(EditText) findViewById(R.id.etIP);
        etPort =(EditText) findViewById(R.id.etPort);
        etId =(EditText) findViewById(R.id.etId);
        etGroup =(EditText) findViewById(R.id.etGroup);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server_config, menu);
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


    public void returnServerConfig(View v){

        //configuration for these things is here
        String ipaddress = etIp.getText().toString();
        String port = etPort.getText().toString();
        String id = etId.getText().toString();
        String group = etGroup.getText().toString();

        //creating intent to be reveiced back to its vower
        Intent intentReturn = new Intent();
        intentReturn.putExtra(CONFIG_IP, ipaddress);
        intentReturn.putExtra(CONFIG_PORT, port);
        intentReturn.putExtra(CONFIG_ID, id);
        intentReturn.putExtra(CONFIG_GROUP, group);

        Log.d("SERVERCONFIG", "I;m called on the Server Config.... ");
        Log.d("RETURNED IP", "The IP is " + ipaddress);

        //returning the intent to its caller
        setResult(MainActivity.WS_CONFIG_REQUEST, intentReturn);
        finish();


    }

}
