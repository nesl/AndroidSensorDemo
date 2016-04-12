package edu.ucla.nesl.android.sensordemo;

import android.app.DownloadManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener, SensorEventListener {
    private static final String TAG = "SensorDemo";
    private SensorManager sensorManager;
    private Sensor mAcc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);

        if (button1 != null) {
            button1.setOnClickListener(this);
        }
        if (button2 != null) {
            button2.setOnClickListener(this);
        }

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAcc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button1) {
            sensorManager.registerListener(this, mAcc, SensorManager.SENSOR_DELAY_UI);
        }

        if (v.getId() == R.id.button2) {
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
    private static final String FACE_UP_URL = "https://maker.ifttt.com/trigger/phone_up_down/with/key/bHOlWt_nu5xrCmTVUdoJar";

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "Acc data x=" + event.values[0] + " y=" + event.values[1] + " z=" + event.values[2]);

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
            try {

                HttpRequest request = HttpRequest.get(params[0]);
                if (request.ok()) {
                    Log.i(TAG, "HTTP Request OK");
                }
            }
            catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }
            return null;
        }
    }
}
