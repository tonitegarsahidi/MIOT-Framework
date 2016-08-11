package com.raudhahproject.miot2;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.neovisionaries.ws.client.OpeningHandshakeException;
import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.logging.Handler;


public class MainActivity extends ActionBarActivity {

    //for intent purposes
    public static final String MYINTENT = "com.raudhahproject.miot";

    //for intent with result purpose
    public static final int WS_CONFIG_REQUEST = 1;
    public static final int BT_CONFIG_REQUEST = 2;

    public int globalDataSent, globalDataReceived;

    TextView tvid, tvgroup, tvserveraddress, tvbluetoothaddress;

    //hellooo... GPS GPS GPS GPS IS HERE
    boolean gpsbroadcast=true;
    LocationManager locationManager;
    LocationListener locationListener;

    //  CONFIG FOR ESTABLISHING WEBSOCKET CONNECTION
    String channelId, channelGroup,channelIP, channelPort;
    String wsurl;

    //CONFIG FOR BLUETOOTH CONNECTION
    String btAddress;

    private ProgressDialog progress;

    ConnectedThread mConnectedThread;

    final byte delimiter = 10; //This is the ASCII code for a newline character




    //Bluetooth
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;

    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    //WEBSOCKET
    WebSocketFactory factory;
    public static WebSocket ws;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //REGISTERING EVENT BUS ON THIS CLASS
        EventBus.getDefault().register(this); // this == your class instance

        //ON START THERE IS NONE
        wsurl = "";
        btAddress = "";


        tvid = (TextView)findViewById(R.id.tvid);
        tvgroup = (TextView)findViewById(R.id.tvGroup);
        tvserveraddress = (TextView)findViewById(R.id.tvServerAddress);
        tvbluetoothaddress = (TextView)findViewById(R.id.tvBluetoothAddress);

        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        //keep track number of data sent/received
        globalDataSent = globalDataReceived = 0;



    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /** *****************************************
     *      OPEN WEBSOCKET CONFIGURATION ACTIVITY
     ********************************************
     */
    public void openWsConfig(View v){

        Intent intent = new Intent(this, ServerConfig.class);
        startActivityForResult(intent,WS_CONFIG_REQUEST);
    }

    /** *****************************************
     *      OPEN BLUETOOTH CONFIGURATION ACTIVITY
     ********************************************
     */
    public void openBluetoothConfig(View v){

        Intent intent = new Intent(this, BluetoothConfig.class);
        startActivityForResult(intent, BT_CONFIG_REQUEST);
    }

    /** *****************************************
     *      HANDLER AFTER RECEIVING INTENT
     *      FROM BOTH WS / BT CONFIG ACTIVITY
     ********************************************
     */

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request it is that we're responding to
        if (requestCode == WS_CONFIG_REQUEST) {
            // Make sure the request was successful
            if (resultCode == 1) {

                // Do something with the bluetooth data...
                //extracting data
                channelIP = data.getStringExtra(ServerConfig.CONFIG_IP);
                channelPort = data.getStringExtra(ServerConfig.CONFIG_PORT);
                channelGroup = data.getStringExtra(ServerConfig.CONFIG_GROUP);
                channelId = data.getStringExtra(ServerConfig.CONFIG_ID);



                //assembly the url
                wsurl = "ws://"+channelIP+":"+channelPort;

                //put and show it on the textview
                tvserveraddress.setText(wsurl.toString());
                tvid.setText(channelId.toString());
                tvgroup.setText(channelGroup.toString());

                Log.d("WSURL", "this is the WSURL "+wsurl);



            }
            else{
                Log.w("NOT OK", "ON RESULT CODE IS NOT OK, it is " + resultCode);
            }
        }
        else if(requestCode == BT_CONFIG_REQUEST){

            Log.d("ON_RESULT_BT","SAYA LAGI ADA DI BT_CONFIG REQUEST, BELUM MASUK RESULT CODE, result code nya "  + resultCode);

            if (resultCode == 2) {



                //get the address from intent
                btAddress = data.getStringExtra(BluetoothConfig.CONFIG_BTADDRESS);

                Log.d("RESULT CODE", "IN SAYA SUDAH DI RESULT CODE LHO, DAPET ADDRESS " + btAddress);

                //put on the textview to show to user
                tvbluetoothaddress.setText(btAddress.toString());
            }

        }
        else{
            Log.w("DIFFERENT_STATUS", "the REQUEST RESPONSE code is different. it is " + requestCode);
        }
    }


    /** *****************************************
     *      STARTING NOW
     *      PAIRING ALL BETWEEN WS AND BLUETOOTH
     ********************************************
     */

    public void startButton(View v){

        if(wsurl.equalsIgnoreCase("") && btAddress.equalsIgnoreCase("")){
            Toast.makeText(this,"YOU SHOULD SET ALL FIRST!",Toast.LENGTH_LONG).show();
        }

        if(!wsurl.equalsIgnoreCase("")){
            Toast.makeText(this,"STARTING WEBSOCKET SERVER",Toast.LENGTH_LONG).show();
            this.connectWSServerOnly();

        }
        if(!btAddress.equalsIgnoreCase("")){
            Toast.makeText(this,"STARTING BLUETOOTH SERVICE",Toast.LENGTH_LONG).show();
            this.connectBlutoothOnly();
        }

//        else{
 //           Toast.makeText(this,"NOW WE ARE READY... BE PREPARED",Toast.LENGTH_LONG).show();
  //          this.connectAll();
   //     }

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


    public void connectWSServerOnly(){
        //getting the websocket ready is here
        //WEBSOCKET PART IS HERE with default timeout 5 seconds
        factory = new WebSocketFactory().setConnectionTimeout(5000);
        this.connectToWS();
    }

    public void connectBlutoothOnly(){


        //calling connect to bluetooth...
        this.connectToBluetooth();

    }

    public void connectAll(){

        this.connectWSServerOnly();

        this.connectBlutoothOnly();


    }

    /**************************************************
     * GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS
     * GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS
     * GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS
     * GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS
     * GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS GPS
     * ************************************************
     */

    public void broadCastGPS(View v){
        TextView consoleLogger;
        consoleLogger = (TextView) findViewById(R.id.tvLogger);

        if(gpsbroadcast){   //still off
            //turn on and broadcast gps...
            //consoleLogger.append("\n TURNING GPS BROADCAST ON....");
            consoleLogger.setText("\n TURNING GPS BROADCAST ON....");

            // Define a listener that responds to location updates
            locationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    // Called when a new location is found by the network location provider.

                    Log.d("LOCATION_FOUND", "received new location : " + location.getLongitude());

                    TextView consoleLogger2;
                    consoleLogger2 = (TextView) findViewById(R.id.tvLogger);

                    String locationLog = "Lat = " + location.getLatitude() + "\t Long = " + location.getLongitude();

                    //createjson for lat and long
                    try{
                        JSONObject locationdata = new JSONObject();
                        locationdata.put("lat", location.getLatitude());
                        locationdata.put("lng", location.getLongitude());
                        locationdata.put("alt", location.getAltitude());
                        locationdata.put("acc", location.getAccuracy());
                        locationdata.put("prov", location.getProvider());

                        locationLog = locationdata.toString();
                    }
                    catch (JSONException e){
                        e.printStackTrace();
                    }



                    String jsonLog = encapsulateData(locationLog);
                    sendDataWS(jsonLog);
                    //consoleLogger2.append("\n " + locationLog);
                    consoleLogger2.setText("\n " + locationLog);


                    //THIS PART IS NEEDED TO BROADCAST DATA TO WEBSOCKET SERVER
                    // put your code here, compile loc data, then call an event bus to broadcast the data....

                }

                public void onStatusChanged(String provider, int status, Bundle extras) {}

                public void onProviderEnabled(String provider) {}

                public void onProviderDisabled(String provider) {}
            };

            // Register the listener with the Location Manager to receive location updates
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);

            gpsbroadcast = false;

        }
        else{
            //turn off gps
            //consoleLogger.append("\n STOPPING GPS BROADCAST....");
            consoleLogger.setText("\n STOPPING GPS BROADCAST....");
            // Remove the listener you previously added
            locationManager.removeUpdates(locationListener);

            gpsbroadcast = true;

        }

    }


    /**************************************************
     * BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH
     * BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH
     * BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH
     * BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH
     * BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH BLUETOOTH
     * *************************************************
     */

    //THIS PART IS WHEN WE CONNECT TO BLUETOOTH...
    public void connectToBluetooth(){

        //CONNECT BLUETOOTH IS HERE
        ConnectBT bt = new ConnectBT(this) ;
        bt.execute();

    }


    /** *************************************************
     *  EVENT BUS TO HANDLE INCOMING DATA MESSAGE FROM BLUETOOTH
     *  just received and forward them!
     *  *************************************************
     */


    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBluetoothMessageEvent(BluetoothMessageEvent event) {
        //Toast.makeText(getApplicationContext(), event.message.toString(), Toast.LENGTH_SHORT).show();

        TextView consoleLogger;
        consoleLogger = (TextView) findViewById(R.id.tvLogger);

        String receivedmessage = event.message.toString();
        String consolemessage = " \n BLUETOOTH : " + receivedmessage + " ";

        //consoleLogger.append(consolemessage);
        consoleLogger.setText(consolemessage);

        String wsSend = encapsulateData(receivedmessage);
        sendDataWS(wsSend);

        Log.d("EVENT_BUS_IN", "I'm inside event bus BLUETOOTH EVENT lho " + receivedmessage);
    }


    //this is the class to be used in event bus onBluetoothMessageEvent
    public class BluetoothMessageEvent {
        public final String message;

        public BluetoothMessageEvent(String message) {
            this.message = message;
        }
    }


    /**************************************
     * SENDING DATA TO BLUETOOTH IS HERE
     * ************************************
     */

    public void sendDataBluetooth(String message){
        if (btSocket!=null)
        {
            try
            {
                btSocket.getOutputStream().write(message.toString().getBytes());
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }



    /**************************************************
     * WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET
     * WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET
      * WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET WEBSOCKET
     ***********************************************
     */

    /** ************************************************
     *      CONNECTING TO WEBSOCKET IS HERE
     * ************************************************
     */
    public void connectToWS(){
        try{
            ws = factory.createSocket(wsurl);

            //ws.addListener(new SoketListner());


            //***********************************************************
            // Register a listener to receive web socket events.
            // *********************************************************
            ws.addListener(new WebSocketAdapter() {
                @Override
                public void onTextMessage(WebSocket websocket, String message) throws Exception {
                    // Received a text message., debug to log
                    Log.d("WS RECEIVE DATA : ", message);

                    //forward chatlog data to handle
                    EventBus.getDefault().post(new MessageEvent(message));

                }
            });

            //calling the ASYNC TASK handler to connect WebSocket Server
            new wsConnection().execute(wsurl);

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }


    /** ************************************************
     *      SENDING DATA THROUGH WEBSOCKET IS HERE
     *      ps : encapsulate your data before sending,
     *           since the data is sent as it is
     * ************************************************
     */

    public void sendDataWS(String message){
        globalDataSent++;
        TextView tvDataReceived = (TextView) findViewById(R.id.tvDataSent);
        tvDataReceived.setText(Integer.toString(globalDataSent));

        new wsSendData().execute(message);
    }


    /** ************************************************
     *      ENCAPSULATE DATA
     *  convert any plain received message from bluetooth / any data
     *  into MIOT Formatted JSON
     *  ************************************************
     */
    public String encapsulateData(String message){
        String dataCapsule = "";

        // CREATE JSON DATA
        // GENERATING JSON TO BE LATER SENT THROUGH SENDING WEBSOCKET DATA
        JSONObject bourne = new JSONObject();

        try {

            long timestamp = System.currentTimeMillis() / 1000L;
            bourne.put("type",   "message");
            bourne.put("text",   "" + message);
            bourne.put("id",     channelId);
            bourne.put("group",  channelGroup);
            bourne.put("time",   "" + timestamp);

            //LAUNCH THE MESSAGE HERE....
            dataCapsule = bourne.toString();

        }
        catch (JSONException ex){
            Log.e("JSON_ERROR", "Invalid creation of JSON data, please try to fix one");
            ex.printStackTrace();
        }

        return dataCapsule;
    }


    /** ************************************************
     *      GENERATE ID
     *  convert any plain received message from bluetooth / any data
     *  into MIOT Formatted JSON
     *  ************************************************
     */
    public String encapsulateId(){
        String dataCapsule = "";

        // CREATE JSON DATA
        // GENERATING JSON TO BE LATER SENT THROUGH SENDING WEBSOCKET DATA
        JSONObject bourne = new JSONObject();

        try {

            long timestamp = System.currentTimeMillis() / 1000L;
            bourne.put("type",   "id");
            bourne.put("text",   "IDENTITY");
            bourne.put("id",     channelId);
            bourne.put("group",  channelGroup);
            bourne.put("time",   "" + timestamp);

            //LAUNCH THE MESSAGE HERE....
            dataCapsule = bourne.toString();

        }
        catch (JSONException ex){
            Log.e("JSON_ERROR", "Invalid creation of JSON id, please try to fix one");
            ex.printStackTrace();
        }

        return dataCapsule;
    }


    /** *****************************************
     *  EVENT BUS WEBSOCKET MESSAGE HANDLE
     *  this part is for event bus handle
     *  which reveiced any data from webSocket
     *  *****************************************
     */

    public class MessageEvent {
        public final String message;

        public MessageEvent(String message) {
            this.message = message;
        }
    }

    // This method will be called when a MessageEvent is posted (in the UI thread for Toast)
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWSMessageEvent(MessageEvent event) {
        //Toast.makeText(getApplicationContext(), event.message.toString(), Toast.LENGTH_SHORT).show();

        //THIS IS THE RAW FORMAT OF MIOT JSON DATA,
        String receivedmessage = event.message.toString();

        //parsing JSON from the receivedmessage
        try{
            JSONObject bourne = new JSONObject(receivedmessage);

            String messageId = bourne.getString("id");
            String messageGroup = bourne.getString("group");
            String messageText = bourne.getString("text");
            String messageTime= bourne.getString("time");

            //compile text to be put on chatlog
            String chatLog = " \n MIOT: ## [" + messageId.toString() + " @ " + messageGroup + "] on "  + messageTime + " : \n" + messageText;

            Log.d("INBOX", chatLog);

            //show on console logger
            TextView consoleLogger;
            consoleLogger = (TextView) findViewById(R.id.tvLogger);
            //consoleLogger.append(chatLog);
            consoleLogger.setText(chatLog);


            //add the number of received data
            globalDataReceived++;
            TextView tvDataReceived = (TextView) findViewById(R.id.tvDataReceived);
            tvDataReceived.setText(Integer.toString(globalDataReceived));


            //===================================
            //THIS PART IS FORWARD DATA TO BLUETOOTH
            //=====================================
            this.sendDataBluetooth(messageText);

        }
        catch (JSONException rogue){
            Log.e("ERROR_JSON", "Cant convert JSON String to JSON Object : " + rogue);
            rogue.printStackTrace();
        }



        Log.d("EVENT_BUS_IN", "I'm inside event bus lho");


    }


    /**
     * MAKE TOAST
     * @param s
     */
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }



    /**
     * *************************************************************
     *      ALL ASYNC_TASK GOES BELOW HERE
     *  Please do not mix up with above function as it may cause confusion... and delay :D
     * ************************************************************
     */

    /**
     * WSCONNECTION CLASS ASYNCTASK
     * ASYNCTASK to handle connection to webSocket
     */
    private class wsConnection extends AsyncTask<String,Void, String> {
        boolean statusSuccess;

        protected String doInBackground(String... serverURL){     //why does this params need to be in array?

            String status = "";

            try{
                Log.e("ASYNC_PARAMS", serverURL[0]);
                Log.i("MIOT : WEBSOCKET ", "process to connect websocket server");

                //connectin is here
                ws.connect();
                Log.i("MIOT : WEBSOCKET ", "connection is succesffull");


                status = "MERDEKA";
            }
            catch (OpeningHandshakeException e)
            {
                // A violation against the WebSocket protocol was detected
                // during the opening handshake.
                Log.e("HANDSHAKE_EXCEPTION", e.toString());
                e.printStackTrace();

                status = "DIJAJAH";
            }
            catch (WebSocketException e)
            {
                // Failed to establish a WebSocket connection.
                Log.e("WEBSOCKET_EXCEPTION", e.toString());
                e.printStackTrace();

                status = "DIJAJAH";;
            }
            catch (Exception e){

                Log.e("OTHER_EXCEPTION", e.toString());
                e.printStackTrace();
                status = "DIJAJAH";

            }
            return status;
        }

        protected void onPostExecute(String status){
            Log.e("ON_POST_EXECUTE", status);

            String firstmessage = encapsulateId();
            sendDataWS(firstmessage);

        }

    }


    /**
     * WSSENDDATA - CLASS ASYNCTASK
     * ASYNCTASK to handle thread when sending data through webSocket
     */
    private class wsSendData extends AsyncTask<String, Void, String>{
        protected String doInBackground(String... data){
            String status;

            try{
                ws.sendText(data[0]);
                status = "ROCKET MELUNCUR DAN KENA : ";
                Log.e("SENDING DATA ", status + data[0]);
            }
            catch (Exception e){

                status = "ROCKET MELESET : ";
                Log.e("ERROR_SEND_DATA",status + e.toString());
            }

            return status;
        }

        protected void onPostExecute(String status){
            Log.e("ON_POST_EXECUTE", " STATUS PENGIRIMAN " + status);
        }

    }


    /**
     *  CONNECTBT - CLASS ASYNCTASK
     *  ASYNTASK to handle connection to Bluetooth Device!
     */
    private class ConnectBT extends AsyncTask<Void, Void, Void>{

        private boolean ConnectSuccess = true; //if it's here, it's almost connected
        private Context context;

        public ConnectBT(Context _context){
            this.context = _context;


        }

        @Override
        protected void onPreExecute()
        {
            //show a progress dialog
            progress = ProgressDialog.show(this.context, "Connecting...", "Please wait!!!");
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {

                    Log.d("CONNECTING BT", "start connecting to bluetooth");

                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(btAddress);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection

                    Log.d("CONNECTING BT", "finish connecting to bluetooth");
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
                e.printStackTrace();
                Log.e("ERROR CONNECTING BT", "there is problem on connecting bluetooth");
            }
            return null;
        }

        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {
                msg("Connected.");
                isBtConnected = true;

                //starting the Bluetooth data thread to run and listening...!
                mConnectedThread = new ConnectedThread(btSocket);
                mConnectedThread.start();

            }
            progress.dismiss();
        }

    }


    /*******************************************************************
     *      ALL THREAD ONLY TASK, GOES BELOW
     ********************************************************************
     */

    // THREAD TO SET CONNECTED BLUETOOTH RECEIVE DATA ON STRING FORMAT
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        boolean stopWorker = false;


        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            String text;
            int readBufferPosition = 0;
            byte [] readBuffer = new byte[1024];

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    int bytesAvailable = mmInStream.available();
                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];

                        //read the stream content and put on packetBytes
                        mmInStream.read(packetBytes);

                        for(int i = 0; i< bytesAvailable; i++){

                            byte b = packetBytes[i];

                            if(b == delimiter) {    //found end of line
                                byte [] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;


                                Log.d("INI DATANYA : ", "" + data);
                                EventBus.getDefault().post(new BluetoothMessageEvent(data));

                            }else {
                                readBuffer[readBufferPosition++] = b;
                            }   //end if delimiter
                        }   //end for

                    }   //end if bytes available





                    //convert
                    // Send the obtained bytes to the UI activity

                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }












}

