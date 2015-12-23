package com.lms.appenza.hotspotfiletransfer;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class CollectAnswers extends AppCompatActivity {
    String LOG_TAG = "HOTSPOTMM";
    WifiManager manager;
    ServerSocket serverSocket = null;
    boolean answerReceived = false;
    String QuizName;
    File quizFile;
    FileWriter fileWriter;
    String studentName;
    TextView studentsSubmittedAnswersTxt,studentsReceivedQuizTxt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collect_answers);
        studentsReceivedQuizTxt = (TextView)findViewById(R.id.srqShowTxt);
        studentsSubmittedAnswersTxt = (TextView)findViewById(R.id.ssaShowTxt);
        manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiConfiguration netConfig = new WifiConfiguration();
        netConfig.SSID = MainActivity.studentSSID;
        Log.d(LOG_TAG, netConfig.SSID + "--- this is SSID");
        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        setWifiApEnabled(netConfig, true);
        QuizName = "Quiz1.txt";
        quizFile = new File(Environment.getExternalStorageDirectory() + "/QuizzesAnswers/" + QuizName);
        try {
            File dirs = new File(quizFile.getParent());
            if (!dirs.exists())
                dirs.mkdirs();
            if (quizFile.createNewFile())
                Log.d(LOG_TAG, "file created");
            else
                Log.d(LOG_TAG, "file not created");

            fileWriter = new FileWriter(quizFile);
            fileWriter.append("This is a test :) " + System.getProperty("line.separator"));
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        new FileServerTask().execute();
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

    private class FileServerTask extends AsyncTask<Void, Void, File> {

        public static final String LOG_TAG = "HOTSPOTMM server";

        @Override
        protected File doInBackground(Void... params) {
            try {
                serverSocket = new ServerSocket(8000);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while (true) {
                try {

                    Log.d(LOG_TAG, "Server: socket opened");
                    Socket client = null;
                    client = serverSocket.accept();
                    BufferedReader st = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String temp ;
                    String studentAnswer="";
                   if(st.readLine().contains("StartOfText")) {
                       while (true) {
                           temp = st.readLine();
                           Log.d(LOG_TAG, temp);

                           if (temp.contains("EndOfText")) {
                               Log.d(LOG_TAG, temp);
                               answerReceived = true;
                               break;
                           }
                           studentAnswer += temp;

                       }
                       Log.d(LOG_TAG, "Student Answer : " + studentAnswer);

                       fileWriter.append("Student Answer <" + studentAnswer + ">" + System.getProperty("line.separator"));
                   }
                    client.close();
                    serverSocket.close();
                    Log.d(LOG_TAG, "Server Conn closed");
                    return quizFile;

                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(LOG_TAG, e.toString());
                }
                return null;
            }
        }


        @Override
        protected void onPostExecute(File f) {

            MainActivity.waitingForQuiz.setVisibility(View.INVISIBLE);
            if (answerReceived) {
                studentsSubmittedAnswersTxt.append(studentName + " has submitted the answers successfully ");
                Toast.makeText(getApplicationContext(), "Received Answers from : " +studentName, Toast.LENGTH_SHORT).show();
            }

            if (f != null) {
                Log.d(LOG_TAG, this.getStatus().toString() + "---- Stopping Service !!!!!");
                setWifiApEnabled(null, false);

            }
        }

    }
}

