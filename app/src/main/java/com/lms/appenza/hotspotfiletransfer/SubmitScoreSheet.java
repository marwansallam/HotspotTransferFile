package com.lms.appenza.hotspotfiletransfer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubmitScoreSheet extends AppCompatActivity {
    public static final String LOG_TAG = "HOTSPOTMM";
    ArrayList<String> checkedStudents;
    List<ScanResult> scanResults;
    WifiManager manager;
    WifiConfiguration conf;
    int netId;
    Map<String, String> json = new HashMap<>();
    String currentTeacher, teacherMacAddress="16:36:c6:a8:45:87";
    Boolean sentFile, networkConnected ,foundTeacher= false;
    public static boolean isSending = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_score_sheet);
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        scanResults = new ArrayList<ScanResult>();
        conf = new WifiConfiguration();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanResults = manager.getScanResults();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        scan();
    }


    public void scanBtn(View view) {
        scan();
    }

    private void scan() {
        if (!manager.isWifiEnabled())
            manager.setWifiEnabled(true);

        manager.startScan();

        Log.d(LOG_TAG, "size ---> " + scanResults.size());

        for (int sr = 0; sr < scanResults.size(); sr++)
            Log.d(LOG_TAG, sr + " -----> " + scanResults.get(sr).SSID + " : " + scanResults.get(sr).BSSID);

        //Adding Students to Online and Offline Lists

    }
    public void submitAnswersToTeacher(View view) {
        while (true) {
            manager.startScan();
            for (int i = 0; i < scanResults.size(); i++) {
                if (scanResults.get(i).BSSID.contains(teacherMacAddress)) {
                    foundTeacher = true;
                    Log.d(LOG_TAG, "Found Teacher : " + scanResults.get(i));
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

                    Log.d(LOG_TAG, "--------------------Sending Answers Started -----------------");
                    if (isWifiConnected)
                        for (Map.Entry entry : json.entrySet()) {
                            if (scanResults.get(i).BSSID.equals(entry.getValue()))
                                currentTeacher = entry.getKey().toString();
                        }
                    Log.d(LOG_TAG, "Sending file to ----- " + currentTeacher);


                    new ClientTask().execute();
                    break;
                } else
                    Log.d(LOG_TAG, "Not a Teacher");
            }
            if(foundTeacher)
                break;
        }
    }


    private class ClientTask extends AsyncTask<Void, Void, Void> {
        private boolean isConencted = false;
        boolean fileCopied = false;
        Socket socket;

        @Override
        protected Void doInBackground(Void... params) {
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

                    OutputStream outputStream = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(outputStream);
                    dos.writeUTF("StartOfText" +'\n');
                    dos.writeUTF("Student:Marwan" +'\n');
                    dos.writeUTF("Question 1 : 10/5" +'\n');
                    dos.writeUTF("EndOfText");



                    outputStream.close();
                    socket.close();
                    isConencted = false;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, e.toString());
            }

            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
//            if (fileCopied)
//                Toast.makeText(getApplicationContext(), "File Sent", Toast.LENGTH_SHORT).show();
//            else
//                Toast.makeText(getApplicationContext(), "File Not Sent", Toast.LENGTH_SHORT).show();
            fileCopied = false;
            isSending = false;
        }
    }

}
