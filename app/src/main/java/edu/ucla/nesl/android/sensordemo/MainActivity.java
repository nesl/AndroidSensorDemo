package edu.ucla.nesl.android.sensordemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener, SensorEventListener {
    private static final String TAG = "SensorDemo";
    private SensorManager sensorManager;
    private TextView mAccViewX, mAccViewY, mAccViewZ;
    private static final int MY_PERMISSIONS_REQUEST_INTERNET = 1;
    private static boolean hasInternetPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Set button listeners for start/stop
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);
        if (button1 != null) {
            button1.setOnClickListener(this);
        }
        if (button2 != null) {
            button2.setOnClickListener(this);
        }

        // Get handle to text fields
        mAccViewX = (TextView) findViewById(R.id.textView1);
        mAccViewY = (TextView) findViewById(R.id.textView2);
        mAccViewZ = (TextView) findViewById(R.id.textView3);

        // Check and request permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET},
                    MY_PERMISSIONS_REQUEST_INTERNET);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_INTERNET: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasInternetPermission = true;
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        // Start sensor sampling
        if (v.getId() == R.id.button1) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Sensor mAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_UI);
        }

        // Stop sensor sampling
        if (v.getId() == R.id.button2) {
            sensorManager.unregisterListener(this);
            sensorManager = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private static boolean phoneFacedDown = false;
    private static final double FACE_DOWN_THRESHOLD = -9.5;
    private static final double FACE_UP_THRESHOLD = 9.5;
    private static final String FACE_DOWN_URL = "https://maker.ifttt.com/trigger/phone_face_down/with/key/bHOlWt_nu5xrCmTVUdoJar";
    private static final String FACE_UP_URL = "https://maker.ifttt.com/trigger/phone_face_up/with/key/bHOlWt_nu5xrCmTVUdoJar";

    @Override
    public void onSensorChanged(SensorEvent event) {
        mAccViewX.setText("Acc X = " + event.values[0]);
        mAccViewY.setText("Acc Y = " + event.values[1]);
        mAccViewZ.setText("Acc Z = " + event.values[2]);

        if (!phoneFacedDown && event.values[2] < FACE_DOWN_THRESHOLD) {
            Toast.makeText(MainActivity.this, "FACE DOWN!", Toast.LENGTH_LONG).show();
            new HTTPRequestTask().execute(FACE_DOWN_URL);
            phoneFacedDown = true;
        }

        if (phoneFacedDown && event.values[2] > FACE_UP_THRESHOLD) {
            Toast.makeText(MainActivity.this, "FACE UP!", Toast.LENGTH_LONG).show();
            new HTTPRequestTask().execute(FACE_UP_URL);
            phoneFacedDown = false;
        }
    }

    private class HTTPRequestTask extends AsyncTask<String, Void, Void>{
        @Override
        protected Void doInBackground(String... params) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.INTERNET)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    HttpRequest request = HttpRequest.get(params[0]);
                    if (request.ok()) {
                        Log.i(TAG, "HTTP Request OK");
                    }
                } catch (Exception ex) {
                    Log.e(TAG, ex.toString());
                }
            }
            else{
                Log.e(TAG, "Cannot send IFTTT request: no Internet permission");
            }
            return null;
        }
    }
}
