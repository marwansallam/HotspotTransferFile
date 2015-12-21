package com.lms.appenza.hotspotfiletransfer;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SendFile extends Service {
    public static final String LOG_TAG = "HOTSPOTMM";
    ArrayList<String> checkedStudents;
    List<ScanResult> scanResults;
    WifiManager manager;
    WifiConfiguration conf;
    int netId;
    Map<String, String> json = new HashMap<>();
    String currentStudent;
    Boolean sentFile,networkConnected =false;
    public static boolean isSending = true;

    public SendFile() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onCreate() {
        super.onCreate();

        manager = StudentList.manager;
        conf = StudentList.conf;
        json = StudentList.json;
        checkedStudents = StudentList.checkedMacAddress;
        scanResults = StudentList.scanResults;
        startSendingFile();

    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    public void startSendingFile(){
        for (int i = 0; i < scanResults.size(); i++) {
            if (checkedStudents.contains(scanResults.get(i).BSSID)) {
                Log.d(LOG_TAG, "Found Student : " + scanResults.get(i));
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.SSID = "\"" + scanResults.get(i).SSID + "\"";
                conf.status = WifiConfiguration.Status.ENABLED;

                // connect to and enable the connection
                netId = manager.addNetwork(conf);
                Log.d("HOTSPOTMM", String.valueOf(netId));
                manager.saveConfiguration();
                manager.disconnect();
                manager.enableNetwork(netId, true);
                manager.reconnect();

                boolean isWifiConnected = false;
                while (!isWifiConnected) {
                    ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                    if (mWifi.isConnected()) {
                        isWifiConnected = true;
                    }
                }
                Log.d(LOG_TAG, isWifiConnected + " Connected to " + conf.SSID);

                Log.d(LOG_TAG, "--------------------Sending File Started -----------------");
                if(isWifiConnected)
                    for(Map.Entry entry: json.entrySet()){
                        if(scanResults.get(i).BSSID.equals(entry.getValue()))
                            currentStudent=entry.getKey().toString();
                    }
                Log.d(LOG_TAG, "Sending file to ----- " + currentStudent);


                new ClientTask().execute(MainActivity.uri);

            } else
                Log.d(LOG_TAG, "Not a Student");
        }

    }

    private class ClientTask extends AsyncTask<Uri, Void, Void> {
        private boolean isConencted = false;
        boolean fileCopied = false;
        Socket socket ;
        int ctr=30;
        @Override
        protected Void doInBackground(Uri... params) {
            Context context = getApplicationContext();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            try {

                socket = new Socket("192.168.43.1", 8000);
                Log.d(LOG_TAG, "Client: socket opened");
                isConencted = true;
                while (isConencted) {
                    ContentResolver cr = context.getContentResolver();
                    InputStream inputStream = cr.openInputStream(params[0]);

                    OutputStream outputStream = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(outputStream);
                    Log.d(LOG_TAG,params[0].getPath());
                    Log.d(LOG_TAG,params[0].getLastPathSegment().substring(params[0].getPath().lastIndexOf("/")));

                    dos.writeUTF(params[0].getLastPathSegment()  +"\n");

                    if (sendFile(inputStream, outputStream)) {
                        Log.d(LOG_TAG, "File Sent to " + currentStudent);
                        sentFile = true;
                        fileCopied = true;
                    } else {
                        Log.d(LOG_TAG, "File Not Sent to " + currentStudent);
                        sentFile = true;
                    }

                    if (inputStream != null)
                        inputStream.close();
                    outputStream.close();
                    socket.close();
                    isConencted = false;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }

            return null;
        }

        private boolean sendFile(InputStream inputStream, OutputStream out) {
            byte []buf = new byte[1024];
            int len=0;
            try {
                while ((len = inputStream.read(buf,0,1024)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                inputStream.close();
            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (fileCopied)
                Toast.makeText(getApplicationContext(), "File Sent", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(getApplicationContext(), "File Not Sent", Toast.LENGTH_SHORT).show();
            fileCopied=false;
            isSending=false;
        }
    }

}
