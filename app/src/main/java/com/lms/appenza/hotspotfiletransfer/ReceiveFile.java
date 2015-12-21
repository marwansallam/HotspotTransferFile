package com.lms.appenza.hotspotfiletransfer;

import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveFile extends Service {
    public ReceiveFile() {
    }
    String LOG_TAG = "HOTSPOTMM";
    ProgressDialog progress;
    WifiManager manager;
    ServerSocket serverSocket = null;
    @Override
    public void onCreate() {
        super.onCreate();
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = MainActivity.studentSSID;
        Log.d(LOG_TAG, netConfig.SSID + "--- this is SSID");
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        setWifiApEnabled(netConfig, true);
        new FileServerTask().execute();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
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

    @Override
    public boolean stopService(Intent name) {
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setWifiApEnabled(null, false);
        Log.d(LOG_TAG,"Stoping service");
        this.stopSelf();
        this.onDestroy();
        return super.stopService(name);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setWifiApEnabled(null, false);
        Log.d(LOG_TAG,"Stoping service");
        this.stopSelf();
    }

    private class FileServerTask extends AsyncTask<Void, Void, File> {

        public static final String LOG_TAG = "HOTSPOTMM server";

        @Override
        protected File doInBackground(Void... params) {
            try {
                serverSocket = new ServerSocket(8000);
                Log.d(LOG_TAG, "Server: socket opened");
                Socket client = serverSocket.accept();
                Log.d(LOG_TAG, "Server: connection accepted");
                InputStream inputStream = client.getInputStream();
                DataInputStream dis = new DataInputStream(inputStream);
                //String fileName = dis.readUTF();
                BufferedOutputStream put=new BufferedOutputStream(client.getOutputStream());
                BufferedReader st=new BufferedReader(new InputStreamReader(client.getInputStream()));
                String s=st.readLine();

//                int splitIndex = s.indexOf("@");
//                String fileName = s.substring(0,splitIndex);

               // String fileName = dis.readLine();
                File file = new File(Environment.getExternalStorageDirectory() + "/HotspotSharedFiles/" + s);
                Log.d(LOG_TAG,"Filename is : " + s);
                File dirs = new File(file.getParent());
                if (!dirs.exists())
                    dirs.mkdirs();
                if (file.createNewFile())
                    Log.d(LOG_TAG, "file created");
                else
                    Log.d(LOG_TAG, "file not created");

                OutputStream outputStream = new FileOutputStream(file);

                if (copyFile(inputStream, outputStream)) {
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
            byte[] buf = new byte[1024];
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
//            Log.d(LOG_TAG, "File Uri: " + Uri.fromFile(f));
            if (f != null) {
                Log.d(LOG_TAG, this.getStatus().toString() + "---- Stopping Service !!!!!");
                setWifiApEnabled(null, false);
                  stopSelf();

            }
        }

    }
}
