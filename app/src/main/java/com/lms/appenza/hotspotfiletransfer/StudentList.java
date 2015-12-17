package com.lms.appenza.hotspotfiletransfer;

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
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentList extends AppCompatActivity {

    public static final String LOG_TAG = "HOTSPOTMM";

    ArrayList<String> checkedMacAddress;
    Boolean sentFile,networkConnected =false;
    List<ScanResult> scanResults;
    ListView onlineList, offlineList;
    Map<String, String> json = new HashMap<>();
    String currentStudent;
    StudentAdapter onlineAdapter, offlineAdapter;
    StudentItem student;
    WifiConfiguration conf;
    WifiManager manager;
    int netId;
    boolean isStudentOnline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        //Creating List of Students Manualy
        json.put("lenovo Small", "16:36:c6:a8:45:87");
        json.put("huawei", "58:2a:f7:a9:7f:20");
        json.put("lenovo Large", "ee:89:f5:3c:f7:3c");
        json.put("Samsung","24:4b:81:9e:e9:c2");


        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        scanResults = new ArrayList<ScanResult>();
        conf = new WifiConfiguration();

        checkedMacAddress = new ArrayList<String>();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanResults = manager.getScanResults();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        onlineList = (ListView) findViewById(R.id.online_list);
        offlineList = (ListView) findViewById(R.id.offline_list);
//        onlineList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
////                StudentItem student = onlineAdapter.getItem(position);
////                student.toggleChecked();
////                StudentAdapter.ViewHolder viewHolder = (StudentAdapter.ViewHolder) view.getTag();
////                viewHolder.getCheckedTextView().setChecked(student.isChecked());
//            }
//        });
        onlineAdapter = new StudentAdapter(this, R.layout.list_item, R.id.checkedText, new ArrayList<StudentItem>());
        offlineAdapter = new StudentAdapter(this, R.layout.list_item, R.id.checkedText, new ArrayList<StudentItem>());
        onlineList.setAdapter(onlineAdapter);
        offlineList.setAdapter(offlineAdapter);

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

        for(int sr=0;sr<scanResults.size();sr++)
            Log.d(LOG_TAG,  sr + " -----> " + scanResults.get(sr).SSID + " : " + scanResults.get(sr).BSSID);

        //Adding Students to Online and Offline Lists
        onlineAdapter.clear();
        offlineAdapter.clear();
        for (Map.Entry<String, String> entry : json.entrySet()) {
            isStudentOnline = false;
            String studentMAC = entry.getValue();
            for (int j = 0; j < scanResults.size(); j++) {
                if (studentMAC.equals(scanResults.get(j).BSSID)) {
                    onlineAdapter.add(new StudentItem(entry.getKey(), entry.getValue(), false));
                    isStudentOnline = true;
                    break;
                }
            }
            if (!isStudentOnline)
                offlineAdapter.add(new StudentItem(entry.getKey(), entry.getValue(), false));

        }
        onlineAdapter.notifyDataSetChanged();
        offlineAdapter.notifyDataSetChanged();
    }

    public void selectAll(View view) {
        StudentAdapter.ViewHolder holder;
        for (int i = 0; i < onlineList.getCount(); i++) {
            student = onlineAdapter.getItem(i);
            student.setChecked(true);
            holder = (StudentAdapter.ViewHolder) onlineList.getChildAt(i).getTag();
            holder.getCheckedTextView().setChecked(true);
        }
    }

    public void sendQuizToStudents(View view) {

        for (int j = 0; j < onlineAdapter.getCount(); j++) {
            if (onlineAdapter.getItem(j).isChecked()) {
                checkedMacAddress.add(onlineAdapter.getItem(j).getMAC());
                Log.d(LOG_TAG, onlineAdapter.getItem(j).getName());
            }
        }

        for (int i = 0; i < scanResults.size(); i++) {
            if (checkedMacAddress.contains(scanResults.get(i).BSSID)) {
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
                Log.d(LOG_TAG, isWifiConnected + " Connected to " +conf.SSID);

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
        boolean fileCopied = false;
        Socket socket ;
        int ctr=30;
        @Override
        protected Void doInBackground(Uri... params) {
            Context context = getApplicationContext();

            try {
                socket = new Socket();
                Log.d(LOG_TAG, "Client: socket opened");

                //   socket.bind(null);
                Log.d(LOG_TAG, "Client: connection requested");

                Log.d(LOG_TAG, manager.getDhcpInfo().toString());
                socket.connect(new InetSocketAddress("192.168.43.1", 8000));
                Log.d(LOG_TAG, "Client: socket connected");

                ContentResolver cr = context.getContentResolver();
                InputStream inputStream = cr.openInputStream(params[0]);
                OutputStream outputStream = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(outputStream);
                dos.writeUTF(params[0].getLastPathSegment());

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
                Toast.makeText(StudentList.this, "File Sent", Toast.LENGTH_SHORT).show();
            else
                Toast.makeText(StudentList.this, "File Not Sent", Toast.LENGTH_SHORT).show();
            fileCopied=false;
        }
    }

}