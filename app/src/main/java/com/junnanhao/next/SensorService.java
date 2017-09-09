package com.junnanhao.next;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Created by jonashao on 2017/7/17.
 * Sensor Service
 */

public class SensorService extends Service implements SensorEventListener {

    private long start = 0;
    private int count = 0;
    private static final long OFFSET = TimeUnit.SECONDS.toMillis(30);

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterSensor();
    }

    private void unregisterSensor() {
        //todo: assure unregister duplicate permitted
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.unregisterListener(this);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void registerSensor() {
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this,
                sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR),
                SensorManager.SENSOR_DELAY_NORMAL,
                (int) TimeUnit.MINUTES.toMicros(1));
        start = System.currentTimeMillis();
        count = 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        long now = sensorEvent.timestamp;
        long duration = now - start;
        count++;
        double frequency = duration / count;

        if (duration > OFFSET) {
            start = now;
            count = 0;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
