package com.github.sug0.se1920project.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // load gyroscope
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        if (sensorManager == null) {
            final Toast toast = Toast.makeText(this, "No sensors found?!", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (gyroscopeSensor == null) {
            final Toast toast = Toast.makeText(this, "No gyroscope found.", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        gyroscopeEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                final float[] sensorValues = event.values;

                final TextView textView = findViewById(R.id.gyroDebug);
                final String debugText = String.format("%d\n%d\n%d\n",
                    (int)sensorValues[0],
                    (int)sensorValues[1],
                    (int)sensorValues[2]);

                textView.setText(debugText);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // do nothing for now
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(gyroscopeEventListener);
    }
}
