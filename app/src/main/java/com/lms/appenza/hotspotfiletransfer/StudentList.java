package com.lms.appenza.hotspotfiletransfer;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StudentList extends AppCompatActivity {

    public static final String LOG_TAG = "HOTSPOTMM";
    static ArrayList<String> checkedMacAddress;
    static List<ScanResult> scanResults;
    ListView onlineList, offlineList;
    static Map<String, String> json = new HashMap<>();
    StudentAdapter onlineAdapter, offlineAdapter;
    StudentItem student;
    static WifiConfiguration conf;
    static WifiManager manager;
    boolean isStudentOnline;
    ProgressDialog progressDialog;
    ArrayList<StudentItem> studentsReceivedQuiz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_list);

        //Creating List of Students Manualy
        json.put("lenovo Small", "16:36:c6:a8:45:87");
        json.put("huawei", "58:2a:f7:a9:7f:20");
        json.put("lenovo Large", "ee:89:f5:3c:f7:3c");
        json.put("Samsung", "24:4b:81:9e:e9:c2");

        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        scanResults = new ArrayList<ScanResult>();
        conf = new WifiConfiguration();
        progressDialog = new ProgressDialog(this);
        checkedMacAddress = new ArrayList<String>();
        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                scanResults = manager.getScanResults();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        onlineList = (ListView) findViewById(R.id.online_list);
        offlineList = (ListView) findViewById(R.id.offline_list);

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

        for (int sr = 0; sr < scanResults.size(); sr++)
            Log.d(LOG_TAG, sr + " -----> " + scanResults.get(sr).SSID + " : " + scanResults.get(sr).BSSID);

        //Adding Students to Online and Offline Lists
        onlineAdapter.clear();
        offlineAdapter.clear();
        for (Map.Entry<String, String> entry : json.entrySet()) {
            isStudentOnline = false;
            String studentMAC = entry.getValue();
            for (int j = 0; j < scanResults.size(); j++) {
                if (studentMAC.equals(scanResults.get(j).BSSID)) {
                    onlineAdapter.add(new StudentItem(entry.getKey(), entry.getValue(), false ,false));
                    isStudentOnline = true;
                    break;
                }
            }
            if (!isStudentOnline)
                offlineAdapter.add(new StudentItem(entry.getKey(), entry.getValue(), false , false));
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
        Intent intent = new Intent(this, SendFile.class);
        intent.putExtra("hashmap", (Serializable) json);
        intent.putStringArrayListExtra("Students", checkedMacAddress);
        startService(intent);
    }


    public void collectAnswers(View view) {

        startActivity(new Intent(this, CollectAnswers.class));

    }
}