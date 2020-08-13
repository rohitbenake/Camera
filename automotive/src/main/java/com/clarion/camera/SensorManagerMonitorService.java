package com.clarion.camera;

import android.app.Service;
import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SensorManagerMonitorService extends Service {
    private static final String TAG = "SensorManagerMonitorSer";
    private Car mCarService;
    private Object mCarSensorManagerReady = new Object();
    private CarSensorManager mCarSensorManager;
    private final Handler mHandler = new Handler();
    public static final String ACTION_GEAR_UNREVERSED =
            "com.clarion.camera.GEAR_UNREVERSED";

    /**
     * Callback for receiving updates from the sensor manager. A Callback can be
     * registered using {@link #registerCallback}.
     */
    public static abstract class Callback {

        public void OnGearPosValChange(int propValue) {
        }
    }

    @GuardedBy("mCallbacks")
    private List<SensorManagerMonitorService.Callback> mCallbacks = new ArrayList<>();

    public void registerCallback(SensorManagerMonitorService.Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(SensorManagerMonitorService.Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }


    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            Log.d(TAG, "onServiceConnected: Connected to Car Service");
            initializeCallbacks();
            onCarServiceReady();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "onServiceDisconnected: Disconnected from Car Service");
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: SensorManagerMonitorService");
        EstablishCarServiceConnection();
        startService();
    }

    private void onCarServiceReady() {
        Log.d(TAG, "onCarServiceReady: entry");

        synchronized (mCarSensorManagerReady) {
            try {
                initSensorManager((CarSensorManager) mCarService.getCarManager(
                        Car.SENSOR_SERVICE));

                mCarSensorManagerReady.notifyAll();
                Log.d(TAG, "onCarServiceReady: mCarSensorManagerReady notified");
            } catch (CarNotConnectedException e) {
                Log.e(TAG, "Car not connected in onServiceConnected");
            }
        }
    }

    private void EstablishCarServiceConnection() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            Log.w(TAG, "EstablishCarServiceConnection: FEATURE_AUTOMOTIVE not available");
            return;
        }

        if(mCarService == null) {
            Log.d(TAG, "EstablishCarServiceConnection:  mCarService is NULL");
            mCarService = Car.createCar(this,mConnection);
            //requestRefresh();
        }
        else{
            Log.d(TAG, "EstablishCarServiceConnection: mCarService is already created");
        }
    }

    private void startService() {
            if(mCarService == null) {
                Log.d(TAG, "EstablishCarServiceConnection:  mCarService is NULL");
                return;
            }

            if (!mCarService.isConnected() && !mCarService.isConnecting()) {
                mCarService.connect();
            }
    }

    CarSensorManager.OnSensorChangedListener   mSenserListener = new CarSensorManager.OnSensorChangedListener() {
        @Override
        public void onSensorChanged(CarSensorEvent carSensorEvent) {
            Log.d(TAG, "onSensorChanged: callback carSensorEvent.sensorType = " + carSensorEvent.sensorType);
            switch (carSensorEvent.sensorType) {
                case CarSensorManager.SENSOR_TYPE_GEAR:
                   // if(carSensorEvent.intValues.length > 0)  { handleGearPosUpdate(carSensorEvent.intValues[0]);}
                    break;
                default:
                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Unhandled Sensor event, id: " + carSensorEvent.sensorType);
                    }
            }
        }
    };

    private void initSensorManager(CarSensorManager carSensorManager) {
        mCarSensorManager = carSensorManager;
        try {
            mCarSensorManager.registerListener(mSenserListener,
                    CarSensorManager.SENSOR_TYPE_GEAR,CarSensorManager.SENSOR_RATE_NORMAL);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Can not connected in SENSOR");
        }
    }

    private void handleGearPosUpdate(int pos) {
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                mCallbacks.get(i).OnGearPosValChange(pos);
            }
        }
    }

    private SensorManagerMonitorService.Callback mValueCallback = new SensorManagerMonitorService.Callback() {
        @Override
        public void OnGearPosValChange(int propValue) {
            super.OnGearPosValChange(propValue);
            Log.d(TAG, "OnGearPosValChange: GearPosition = "+propValue);
//            final int gearPosition  = propValue;
//            mHandler.post(new Runnable() {
//                public void run() {
//                    if(gearPosition == 2&&
//                            !CameraFeedActivity.isRunning()){
//                        Log.i(TAG, "run: calling startRearviewCameraActivity ");
//                        startRearviewCameraActivity();
//                    } else if(gearPosition !=
//                            2&&
//                            CameraFeedActivity.isRunning()) {
//                        Log.i(TAG, "run: calling sendVehicleUnreversedBroadcast ");
//                        sendVehicleUnreversedBroadcast();
//                    }
//                }
//
//                private void startRearviewCameraActivity() {
//                    Intent intent = new Intent(
//                            SensorManagerMonitorService.this,
//                            CameraFeedActivity.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    SensorManagerMonitorService.this.startActivity(intent);
//                }
//
//                private void sendVehicleUnreversedBroadcast() {
//                    Intent unreversedIntent = new Intent(
//                            ACTION_GEAR_UNREVERSED);
//                    sendBroadcast(unreversedIntent);
//                }
//            });
        }
    };

    private void initializeCallbacks() {
        registerCallback(mValueCallback);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterCallback(mValueCallback);

        if (mCarSensorManager != null) {
            mCarSensorManager.unregisterListener(mSenserListener);
        }
        if (mCarService != null) {
            mCarService.disconnect();
        }
    }
}
