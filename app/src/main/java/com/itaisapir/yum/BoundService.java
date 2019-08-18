package com.itaisapir.yum;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.logging.Logger;

public class BoundService extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    private Sensor mRotationSensor;

    private static final int SENSOR_DELAY = 500 * 1000; // 500ms
    private static final int RESUME_SENSOR_DELAY = 2000; // 2sec

    private static final int FROM_RADS_TO_DEGS = -57;
    private long lastSampleTime;

    private final IBinder mBinder = new SensorBinder();
    private RotationAlertListener mListener;


    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null) {
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY);
        lastSampleTime = System.currentTimeMillis();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e("Sensor", "Bind!");
        return mBinder;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Logger.getLogger("Rotation").info("sensor change");
        float roll = 0;
        float pitch = 0;
        if (event.sensor == mRotationSensor) {
            if (event.values.length > 4) {
                float[] truncatedRotationVector = new float[4];
                System.arraycopy(event.values, 0, truncatedRotationVector, 0, 4);
                roll = update(truncatedRotationVector)[0];
                pitch = update(truncatedRotationVector)[1];
            } else {
                roll = update(event.values)[0];
                pitch = update(event.values)[1];
            }
        }

        boolean isNotFlat = (pitch > -24 && pitch < 24); // if device is laid flat on a surface, we don't want to change the orientation

        if(isNotFlat && (( roll > 80 && roll < 120) || (roll < -80 && roll > -120))){
            long time  = System.currentTimeMillis();
            if (time > lastSampleTime+RESUME_SENSOR_DELAY) {
                lastSampleTime = time;
                mListener.rotationAlert();
            }
        }
    }

    private float[] update(float[] vectors) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, vectors);
        int worldAxisX = SensorManager.AXIS_X;
        int worldAxisZ = SensorManager.AXIS_Z;
        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisX, worldAxisZ, adjustedRotationMatrix);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);
        float pitch = orientation[1] * FROM_RADS_TO_DEGS;
        float roll = orientation[2] * FROM_RADS_TO_DEGS;
        float rotations[] = {roll, pitch};
        Logger.getLogger("Rotation").info("roll = "+ roll);
        return rotations;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public class SensorBinder extends Binder {
        void registerListener(RotationAlertListener listener){
            mListener = listener;
        }
    }

    public interface RotationAlertListener {
        void rotationAlert();
    }

}
