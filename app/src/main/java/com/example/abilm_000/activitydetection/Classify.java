package com.example.abilm_000.activitydetection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.lazy.IBk;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

//
//import net.sf.javaml.core.Dataset;
//import net.sf.javaml.core.DenseInstance;
//import net.sf.javaml.core.Instance;

public class Classify extends AppCompatActivity implements SensorEventListener{
    private Button btnStart;
    private TextView tv1, tv2, tv3, tvAct, tvHasil;
    private SensorManager mSensorManager;
    private Sensor sensorAccelerometer;
    private Sensor sensorProximity;
    private Sensor sensorLight;
    private boolean started = false;
    private boolean stopped = true;
    private ArrayList<Float> X, Y, Z;
    private float x,y,z;
    private int window = 20;
    private double aktivitas;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private Instances dataTrain, dataTest;
    private Classifier knn;
    private ArrayList<String> kelas;
    private NaiveBayes bayes;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classify);

        if (shouldAskPermissions()) {
            verifyStoragePermissions(this);
        }
        tv1 = (TextView) findViewById(R.id.textView8);
        tv2 = (TextView) findViewById(R.id.textView9);
        tv3 = (TextView) findViewById(R.id.textView10);
        tvAct = (TextView) findViewById(R.id.textView12);
        tvAct.setVisibility(View.INVISIBLE);
        tvHasil = (TextView) findViewById(R.id.textView11);
        tvHasil.setVisibility(View.INVISIBLE);
        btnStart = (Button) findViewById(R.id.button2);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorProximity = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorProximity, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, sensorLight, SensorManager.SENSOR_DELAY_NORMAL);
        X = new ArrayList<Float>();
        Y = new ArrayList<Float>();
        Z = new ArrayList<Float>();
        bayes = new NaiveBayes();
        knn = new IBk();
        try{
            ConverterUtils.DataSource source = new ConverterUtils.DataSource("/sdcard/acc.csv");
            dataTrain = source.getDataSet();
            if (dataTrain.classIndex() == -1)
                dataTrain.setClassIndex(dataTrain.numAttributes() - 1);
            knn.buildClassifier(dataTrain);
            bayes.buildClassifier(dataTrain);
            Log.d("h", String.valueOf(dataTrain.classIndex()));
            Log.d("i", String.valueOf(dataTrain.numAttributes()));
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            ConverterUtils.DataSource source = new ConverterUtils.DataSource("/sdcard/template.csv");
            dataTest = source.getDataSet();
            if (dataTest.classIndex() == -1)
                dataTest.setClassIndex(dataTest.numAttributes() - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Attribute att = dataTrain.classAttribute();
        for(int i = 0; i < dataTrain.numClasses();i++) {
            Log.d("g",att.value(i));
        }
        for(int i = 0 ; i < dataTrain.numAttributes();i++){
            Log.d("j", dataTrain.attribute(i).name());
        }
        addListenerOnButton();
    }

    public void addListenerOnButton() {
        btnStart.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(stopped == false){
                    Toast.makeText(Classify.this,
                            "Stop Detecting Activity", Toast.LENGTH_SHORT).show();
                    tvAct.setVisibility(View.INVISIBLE);
                    tvHasil.setVisibility(View.INVISIBLE);
                    stopped = true;
                    btnStart.setText("Create Dataset");
                } else {
                    Toast.makeText(Classify.this,
                            "Start Detecting Activity", Toast.LENGTH_SHORT).show();
                    tvAct.setVisibility(View.VISIBLE);
                    tvHasil.setVisibility(View.VISIBLE);
                    stopped = false;
                    btnStart.setText("Stop");
                }
            }

        });
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
            tv1.setText("X : " + String.valueOf(x));
            tv2.setText("Y : " + String.valueOf(y));
            tv3.setText("Z : " + String.valueOf(z));
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
                stdDevX = (float) Math.sqrt(stdX / (window - 1));
                stdDevY = (float) Math.sqrt(stdY / (window - 1));
                stdDevZ = (float) Math.sqrt(stdZ / (window - 1));
                double[] val = new double[] { meanX, meanY, meanZ,stdDevX, stdDevY, stdDevZ,Collections.max(X)
                        , Collections.max(Y), Collections.max(Z), Collections.min(X),  Collections.min(Y), Collections.min(Z),-1};
                Instance instance = new DenseInstance(1.0, val);
                dataTest.add(instance);
                dataTest.setClassIndex(dataTest.numAttributes() - 1);
                double[] aktivitas1 = new double[]{};
//                double[] aktivitas2 = new double[]{};
                try {
                    aktivitas1 = knn.distributionForInstance(dataTest.lastInstance());
//                    aktivitas2 = bayes.distributionForInstance(dataTest.lastInstance());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                double max1 = 0;
                int maxIndex1 = 0;

                for(int i=0; i < aktivitas1.length; i++){
                    if(aktivitas1[i] > max1){
                        maxIndex1 = i;
                        max1 = aktivitas1[i];
                    }
                }
//                double max2 = 0;
//                int maxIndex2 = 0;
//                for(int i=0; i < aktivitas1.length; i++){
//                    if(aktivitas1[i] > max2){
//                        maxIndex2 = i;
//                        max2 = aktivitas1[i];
//                    }
//                }
                Log.d("Hasil Classify", String.valueOf(maxIndex1));
                aktivitas = maxIndex1;
                tvHasil.setText(dataTrain.classAttribute().value((int)maxIndex1));
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
                tv1.setText("X : value x");
                tv2.setText("Y : value y");
                tv3.setText("Z : value z");
            }
            Log.d("Start : ",String.valueOf(started));
            Log.d("Stop : ",String.valueOf(stopped));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, sensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
