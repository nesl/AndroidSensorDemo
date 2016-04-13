package ucla.nesl.beaconproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.github.kevinsawicki.http.HttpRequest;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "BeaconDemo";

    private final String IBEACON_PREFIX_STR = "0201061AFF4C000215";
    private final byte[] IBEACON_PREFIX = hexStringToByteArray(IBEACON_PREFIX_STR);
    private final String TARGET_UUID_STR = "B9407F30F5F8466EAFF925556B57FE6D";
    private final byte[] TARGET_UUID = hexStringToByteArray(TARGET_UUID_STR);
    private final short TARGET_MAJOR = 339;
    private final short TARGET_MINOR = 18391;

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner leScanner;

    private TextView textState;
    private TextView textUuid;
    private TextView textMajor;
    private TextView textMinor;
    private TextView textRssi;
    private TextView textStat;

    private final String IN_OFFICE_URL = "https://maker.ifttt.com/trigger/in_office/with/key/pRaRula5WOWexH30oz_FBuHSLPgK5inrEKFB55akVon";
    private final String OUT_OF_OFFICE_URL = "https://maker.ifttt.com/trigger/out_of_office/with/key/pRaRula5WOWexH30oz_FBuHSLPgK5inrEKFB55akVon";
    private final int IN_RSSI_THRES = -60;
    private final int OUT_RSSI_THRES = -70;
    private final int TRIGGER_CNT_THRES = 3;

    private int inCnt;
    private int outCnt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textState = (TextView) findViewById(R.id.textView2);
        textUuid = (TextView) findViewById(R.id.textView4);
        textMajor = (TextView) findViewById(R.id.textView5);
        textMinor = (TextView) findViewById(R.id.textView6);
        textRssi = (TextView) findViewById(R.id.textView7);
        textStat = (TextView) findViewById(R.id.textView8);

        // Permission checking
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            bleScanInitialization();
        } else {
            ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    bleScanInitialization();
                }
            }
        }
    }

    // ---- Bluetooth LE setup --------------------------------------------------------------------
    private void bleScanInitialization() {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getApplicationContext()
                        .getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        leScanner = bluetoothAdapter.getBluetoothLeScanner();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        List<ScanFilter> filters = new ArrayList<ScanFilter>();
        leScanner.startScan(filters, settings, leScanCallback);
    }

    // ---- Bluetooth LE callback -----------------------------------------------------------------
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            int rssi = result.getRssi();
            byte[] payload = result.getScanRecord().getBytes();

            // Reject if the payload's length is not correct
            if (payload.length < 30)
                return;

            // Reject if prefix does not match
            if (Arrays.equals(IBEACON_PREFIX, Arrays.copyOfRange(payload, 0, 9)) == false)
                return;

            // Reject if UUID is not correct
            if (Arrays.equals(TARGET_UUID, Arrays.copyOfRange(payload, 9, 25)) == false)
                return;

            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(payload, 25, 29);
                DataInputStream dis = new DataInputStream(bais);
                short curMajor = dis.readShort();
                short curMinor = dis.readShort();

                // Reject if major or minor are not correct
                if (curMajor != TARGET_MAJOR || curMinor != TARGET_MINOR)
                    return;

                textUuid.setText(
                        "UUID: " + byteArrayToHexString(Arrays.copyOfRange(payload, 9, 25)));
                textMajor.setText("Major: " + curMajor);
                textMinor.setText("Minor: " + curMinor);
                textRssi.setText("Rssi: " + rssi);

                // Handle finite state machine
                if (rssi > IN_RSSI_THRES) {
                    inCnt++;
                    outCnt = 0;
                }
                else if (rssi < OUT_RSSI_THRES) {
                    outCnt++;
                    inCnt = 0;
                }
                else {
                    inCnt = 0;
                    outCnt = 0;
                }

                if (inCnt == TRIGGER_CNT_THRES) {
                    Toast.makeText(MainActivity.this, "Distance: close", Toast.LENGTH_LONG).show();
                    new HTTPRequestTask().execute(IN_OFFICE_URL);
                    textState.setText("In office");
                }
                else if (outCnt == TRIGGER_CNT_THRES) {
                    Toast.makeText(MainActivity.this, "Distance: far", Toast.LENGTH_LONG).show();
                    new HTTPRequestTask().execute(OUT_OF_OFFICE_URL);
                    textState.setText("Out of office");
                }
                textStat.setText("In:" + inCnt + ", Out:" + outCnt);
            } catch (Exception e) {
                Log.e(TAG, "here is an exception", e);
            }
        }
    };

    // ---- HTTP request --------------------------------------------------------------------------
    private class HTTPRequestTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            try {
                HttpRequest request = HttpRequest.get(params[0]);
                if (request.ok()) {
                }
            }
            catch (Exception ex) {
                Log.e(TAG, ex.toString());
            }
            return null;
        }
    }

    // ---- My utility function (Hex <=> Byte array) ----------------------------------------------
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i+1), 16));
        }
        return data;
    }

    private String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
