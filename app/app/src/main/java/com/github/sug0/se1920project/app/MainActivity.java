package com.github.sug0.se1920project.app;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.*;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {
    // mqtt client stuff
    private final AtomicReference<MqttAndroidClient> atomicMqttClient = new AtomicReference<>();
    private final AtomicBoolean mqttLoaded = new AtomicBoolean();

    // mqtt consts
    private static final String MQTT_CLIENT_PREFIX = "sugo";
    private static final String MQTT_BROKER = "tcp://broker.hivemq.com:1883";
    private static final String MQTT_TOPIC = "minecraft/fcup/accelerometer-data";

    // gyroscope stuff
    private SensorManager sensorManager;
    private Sensor gyroscopeSensor;
    private SensorEventListener gyroscopeEventListener;

    // gyroscope counter
    private int gyroscopeCounter = 0;
    private static final int GYRO_PUBLISH_FREQ = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // try to connect to message broker
        MqttAndroidClient mqttClient = new MqttAndroidClient(getApplicationContext(), MQTT_BROKER, generateClientId());
        atomicMqttClient.set(mqttClient);
        installMqttCallback();
        connectMqtt();

        // load gyroscope
        sensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);

        if (sensorManager == null) {
            toast("Failed to load sensor manager.");
            finish();
        }

        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (gyroscopeSensor == null) {
            toast("Failed to load gyroscope.");
            finish();
        }

        gyroscopeEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (gyroscopeCounter == 0) {
                    // fetch event values
                    final float[] sensorFloatValues = event.values;
                    final int[] sensorIntValues = {
                        (int)sensorFloatValues[0],
                        (int)sensorFloatValues[1],
                        (int)sensorFloatValues[2],
                    };

                    final TextView textView = findViewById(R.id.gyroDebug);
                    final String debugText = Arrays.toString(sensorIntValues);

                    // update debug view
                    textView.setText(debugText);

                    // send data to mqtt
                    sendMqtt(debugText);
                }
                gyroscopeCounter = (gyroscopeCounter + 1) % GYRO_PUBLISH_FREQ;
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
        connectMqtt();
        sensorManager.registerListener(gyroscopeEventListener, gyroscopeSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        disconnectMqtt();
        sensorManager.unregisterListener(gyroscopeEventListener);
    }

    private void installMqttCallback() {
        MqttAndroidClient mqttClient = atomicMqttClient.get();
        if (mqttClient == null) {
            toast("The hell?");
            try {
                finalize();
            } catch (Throwable e) {
                toast("Failed to finalize: " + e);
            } finally {
                return;
            }
        }
        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                mqttLoaded.set(true);
            }

            @Override
            public void connectionLost(Throwable cause) {
                mqttLoaded.set(false);
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // do nothing for now
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // do nothing for now
            }
        });
    }

    private void connectMqtt() {
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setConnectionTimeout(60);
        try {
            MqttAndroidClient mqttClient = atomicMqttClient.get();
            if (mqttClient != null) {
                mqttClient.connect(connectOptions, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // nothing
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                        toast("Failed to connect to MQTT :" + e);
                    }
                });
            }
        } catch (MqttException e) {
            toast("Failed to connect to MQTT: " + e);
        }
    }

    private void disconnectMqtt() {
        try {
            MqttAndroidClient mqttClient = atomicMqttClient.get();
            if (mqttClient != null) {
                mqttClient.disconnect(60 * 1000, getApplicationContext(), new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // nothing
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable e) {
                        toast("Failed to disconnect to MQTT :" + e);
                    }
                });
            }
        } catch (MqttException e) {
            toast("Failed to disconnect from MQTT: " + e);
        }
    }

    private void sendMqtt(String payload) {
        if (mqttLoaded.get()) {
            MqttMessage message = new MqttMessage();
            message.setQos(0);
            message.setRetained(false);
            message.setPayload(payload.getBytes());
            try {
                MqttAndroidClient mqttClient = atomicMqttClient.get();
                if (mqttClient != null) {
                    mqttClient.publish(MQTT_TOPIC, message);
                }
            } catch (MqttException e) {
                toast("Failed to publish to MQTT: " + e);
            }
        }
    }

    private static String generateClientId() {
        final long x = (new Random()).nextLong();
        return MQTT_CLIENT_PREFIX + "-" + (x & 0xffffffff);
    }

    private void toast(String message) {
        final Toast toast = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        toast.show();
    }
}
