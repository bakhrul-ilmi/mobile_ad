package com.example.abilm_000.activitydetection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;


public class ReadAccelerometer extends AppCompatActivity implements SensorEventListener{

    private SensorManager mSensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorProximity;
    private Sensor sensorLight;
    private Button btnStart;
    private TextView tv2, tv3, tv4;
    private float x,y,z;
    private boolean started = false;
    private boolean stopped = true;
    private RadioGroup radioGroup;
    private RadioButton radioButton;
    private File path;
    private int activity;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private ArrayList<Float> X, Y, Z;
    private int window = 20;
    CSVWriter writer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_accelerometer);
        if (shouldAskPermissions()) {
            verifyStoragePermissions(this);
        }
        path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tv2 = (TextView) findViewById(R.id.textView5);
        tv3 = (TextView) findViewById(R.id.textView6);
        tv4 = (TextView) findViewById(R.id.textView7);
        btnStart = (Button) findViewById(R.id.button);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        X = new ArrayList<Float>();
        Y = new ArrayList<Float>();
        Z = new ArrayList<Float>();
        addListenerOnButton();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER && started && !stopped) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            X.add(x);
            Y.add(y);
            Z.add(z);
            tv2.setText(String.valueOf(x));
            tv3.setText(String.valueOf(y));
            tv4.setText(String.valueOf(z));
            if(X.size() == window) {
                float sumX = 0, sumY = 0, sumZ = 0;
                float stdX = 0, stdY = 0, stdZ = 0;
                float stdDevX = 0, stdDevY = 0, stdDevZ = 0;
                float meanX = 0, meanY = 0, meanZ = 0;

                for (int j = 0; j < X.size(); j++) {
                    sumX += X.get(j);
                    sumY += Y.get(j);
                    sumZ += Z.get(j);
                }
                meanX = sumX/window;
                meanY = sumY/window;
                meanZ = sumZ/window;
                for (int j = 0; j < X.size(); j++) {
                    stdX += (X.get(j) - meanX) * (X.get(j) - meanX);
                    stdY += (Y.get(j) - meanY) * (Y.get(j) - meanY);
                    stdZ += (Z.get(j) - meanZ) * (Z.get(j) - meanZ);
                }
                Collections.max(X);
                stdDevX = (float) Math.sqrt(stdX / (window - 1));
                stdDevY = (float) Math.sqrt(stdY / (window - 1));
                stdDevZ = (float) Math.sqrt(stdZ / (window - 1));
                String data = String.valueOf(meanX) + ";" + String.valueOf(meanY) + ";" + String.valueOf(meanZ)+ ';' + String.valueOf(stdDevX) + ';' + String.valueOf(stdDevY) + ';' + String.valueOf(stdDevZ)
                        + ';' + String.valueOf(Collections.max(X)) + ';' + String.valueOf(Collections.max(Y)) + ';' + String.valueOf(Collections.max(Z)) + ';' + String.valueOf(Collections.min(X))
                        + ';' + String.valueOf(Collections.min(Y)) + ';' + String.valueOf(Collections.min(Z)) + ';' + radioButton.getText();
                Log.d("a", data);
                try
                {
                    writer = new CSVWriter(new FileWriter("/sdcard/acc.csv",true),',');
                    String[] entries = data.split(";"); // array of your values
                    Log.d("a", entries[0]);
                    writer.writeNext(entries);
                    writer.close();
                } catch (IOException e)
                {
                    e.printStackTrace();
                }
                X.clear();
                Y.clear();
                Z.clear();
            }

        }
        if (mySensor.getType() == Sensor.TYPE_PROXIMITY) {
            float data = event.values[0];
            Log.d("Proximity : ",String.valueOf(data));
            if(data < 1.0 && !stopped)
                started = true;
            else{
                started = false;
                tv2.setText("value x");
                tv3.setText("value y");
                tv4.setText("value z");
            }
            Log.d("Start : ",String.valueOf(started));
            Log.d("Stop : ",String.valueOf(stopped));
        }
    }
    public void addListenerOnButton() {
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CheckBox newCsv = (CheckBox) findViewById(R.id.checkBox);
                String data = "MeanX;MeanY;MeanZ;StdDevX;StdDevY;StdDevZ;MaxX;MaxY;MaxZ;MinX;MinY;MinZ;Class";
                if(newCsv.isChecked()){
                    try
                    {
                        writer = new CSVWriter(new FileWriter("/sdcard/acc.csv"),',');
                        String[] entries = data.split(";"); // array of your values
                        writer.writeNext(entries);
                        writer.close();

                        writer = new CSVWriter(new FileWriter("/sdcard/template.csv"),',');
                        entries = data.split(";"); // array of your values
                        writer.writeNext(entries);
                        writer.close();
                    } catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }

                if(stopped == false){
                    Toast.makeText(ReadAccelerometer.this,
                            "Stop Creating Dataset : " + radioButton.getText(), Toast.LENGTH_SHORT).show();
                    stopped = true;
                    btnStart.setText("Create Dataset");
                } else {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    radioButton = (RadioButton) findViewById(selectedId);
                    Toast.makeText(ReadAccelerometer.this,
                            "Start Create Dataset : " + radioButton.getText(), Toast.LENGTH_SHORT).show();
                    stopped = false;
                    btnStart.setText("Stop");
                }
            }

        });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_read_accelerometer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_classify) {
            startActivity(new Intent(this, Classify.class));
        }

        return super.onOptionsItemSelected(item);
    }

    protected boolean shouldAskPermissions() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
