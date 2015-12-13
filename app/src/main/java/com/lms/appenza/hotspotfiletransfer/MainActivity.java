package com.lms.appenza.hotspotfiletransfer;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = "HOTSPOTMM";
    public static final int CHOOSE_FILE_REQUEST_CODE = 10;
    public static  Uri uri ;
    ProgressDialog progress;
    WifiManager manager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progress= new ProgressDialog(this);
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }

    public void send(View view) {

        if(!manager.isWifiEnabled()) {
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
            Log.d(LOG_TAG, "Uri: " + uri.toString()) ;
            startActivity(new Intent(this, StudentList.class));
        }
    }

    public void receive(View view) {
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = "Test";
        Log.d(LOG_TAG,netConfig.SSID+ "--- this is SSID");
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        setWifiApEnabled(netConfig, true);
        
        progress.setMessage("Receiving...");
        progress.show();
        new FileServerTask().execute();
    }

    private boolean setWifiApEnabled(WifiConfiguration wifiConfig, boolean enabled) {
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

    private class FileServerTask extends AsyncTask<Void, Void, File> {

        public static final String LOG_TAG = "HOTSPOTMM server";

        @Override
        protected File doInBackground(Void... params) {
            try {
                ServerSocket serverSocket = new ServerSocket(8000);
                Log.d(LOG_TAG, "Server: socket opened");
                Socket client = serverSocket.accept();
                Log.d(LOG_TAG, "Server: connection accepted");
                InputStream inputStream = client.getInputStream();
                DataInputStream dis = new DataInputStream(inputStream);
                String fname = dis.readUTF();
                File file = new File(Environment.getExternalStorageDirectory() + "/HotspotSharedFiles/" + fname);
                File dirs = new File(file.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                if (file.createNewFile())
                    Log.d(LOG_TAG,"file created");
                else
                    Log.d(LOG_TAG, "file not created");

                FileOutputStream outputStream = new FileOutputStream(file);

                if(copyFile(inputStream, outputStream)) {
                    Log.d(LOG_TAG, "File received");
                } else {
                    Log.d(LOG_TAG, "File not copied");
                }
                client.close();
                serverSocket.close();
                Log.d(LOG_TAG, "Server Conn closed");
                return file;

            } catch (IOException e) {
                e.printStackTrace();
                Log.e(LOG_TAG, e.toString());
            }
            return null;

        }

        private boolean copyFile(InputStream inputStream, OutputStream out) {
            byte buf[] = new byte[1024];
            int len;
            try {
                while ((len = inputStream.read(buf)) != -1) {
                    out.write(buf, 0, len);
                }
                out.close();
                inputStream.close();
            } catch (IOException e) {
                Log.d(LOG_TAG, e.toString());
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(File f) {
            Log.d(LOG_TAG, "File Uri: " + Uri.fromFile(f));
            if (f != null) {
                progress.dismiss();
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setDataAndType(Uri.fromFile(f), "*/*");
//                startActivity(intent);
            }
        }

    }




}
