package com.lms.appenza.hotspotfiletransfer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "HOTSPOTMM";
    public static final int CHOOSE_FILE_REQUEST_CODE = 10;
    public static Uri uri;
    static WifiManager manager;
    static String studentSSID;
    static Button receiveBtn;
    static TextView waitingForQuiz;
    static String teacherMacAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        studentSSID = "hStudent";
        receiveBtn = (Button)findViewById(R.id.receiveBtn);
        waitingForQuiz = (TextView) findViewById(R.id.waitForQuizTxt);
        waitingForQuiz.setVisibility(View.INVISIBLE);
        WifiInfo wifiInfo = manager.getConnectionInfo();
        teacherMacAddress = wifiInfo.getMacAddress();
        Log.d(LOG_TAG, "mac address 1: " + teacherMacAddress);

    }

    public void send(View view) {

        if (!manager.isWifiEnabled()) {
            setWifiApEnabled(null, false);
            manager.setWifiEnabled(true);
        }
        chooseFile();

    }

    public void chooseFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == CHOOSE_FILE_REQUEST_CODE) {
            uri = data.getData();
            Log.d(LOG_TAG, "Uri: " + uri.toString());
            Log.d(LOG_TAG, "Uri: " + data.toString());
            startActivity(new Intent(this, StudentList.class));
        }
    }

    public void receive(View view) {
        teacherMacAddress="00:00:00:00:00:00";
        Intent intent = new Intent(this, ReceiveFile.class);

        if(!receiveBtn.getText().toString().contains("Stop")) {
            //startActivity(new Intent(this, Receiving.class));
            receiveBtn.setText("Stop Receiving");
            startService(intent);
            waitingForQuiz.setVisibility(View.VISIBLE);

        }else{
            waitingForQuiz.setVisibility(View.INVISIBLE);

            stopService(intent);
            receiveBtn.setText("Start Receiving");
            waitingForQuiz.setText("Waiting for Quiz ...");
        }
    }


    public boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
        try {
            if (enabled) { // disable WiFi in any case
                manager.setWifiEnabled(false);
            }
            Method method = manager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            return (Boolean) method.invoke(manager, wifiConfig, enabled);
        } catch (Exception e) {
            Log.e(this.getClass().toString(), "", e);
            return false;
        }
    }

    public void goToSubmit(View view){
        startActivity(new Intent(this, SubmitScoreSheet.class));

    }

}