package com.clarion.camera;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.car.Car;
import android.car.CarNotConnectedException;
import android.car.hardware.CarSensorEvent;
import android.car.hardware.CarSensorManager;
import android.content.ComponentName;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    RearviewCameraView mCameraView;
    private Car mCarService;
    private final String[] permissions = new String[]{Car.PERMISSION_POWERTRAIN,Car.PERMISSION_SPEED};
    //    private final String[] permissions = new String[]{Car.PERMISSION_SPEED};
    //PERMISSION_READ_STEERING_STATE android.car.permission.READ_CAR_STEERING
    private Object mCarSensorManagerReady = new Object();
    private CarSensorManager mCarSensorManager;


    /**
     * Callback for receiving updates from the sensor manager. A Callback can be
     * registered using {@link #registerCallback}.
     */
    public static abstract class Callback {

        public void OnGearPosValChange(int propValue) {
        }


        public void OnSpeedValChange(Float propValue) {
        }
    }
    @GuardedBy("mCallbacks")
    private List<Callback> mCallbacks = new ArrayList<>();

    public void registerCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.add(callback);
        }
    }

    public void unregisterCallback(Callback callback) {
        synchronized (mCallbacks) {
            mCallbacks.remove(callback);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mCameraView = (RearviewCameraView) findViewById(R.id.camera_view_id);

        EstablishCarServiceConnection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterCallback(mValueCallback);

        if (mCarSensorManager != null) {
            mCarSensorManager.unregisterListener(mSenserListener);
        }
        if (mCarService != null) {
            mCarService.disconnect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService();//Can this be done in OnCreate
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (permissions[0].equals(Car.PERMISSION_SPEED)&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "onRequestPermissionsResult: Permission for"+ permissions[0]+" GRANTED");
            startService();
        }
    }

    private void startService() {
        if(checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "startService: Permission for"+permissions[0]+" is GRANTED");

            if(mCarService == null) {
                Log.d(TAG, "EstablishCarServiceConnection:  mCarService is NULL");
                return;
            }

            if (!mCarService.isConnected() && !mCarService.isConnecting()) {
                mCarService.connect();
            }
        }
        else
        {
            Log.d(TAG, "startService: Permission for "+permissions[0]+" is NOT GRANTED");
            requestPermissions(permissions, 0);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: Connected to Car Service");
            initializeCallbacks();
            onCarServiceReady();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Disconnected from Car Service");
        }
    };

    private void initializeCallbacks() {
        registerCallback(mValueCallback);
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

    CarSensorManager.OnSensorChangedListener   mSenserListener = new CarSensorManager.OnSensorChangedListener() {
        @Override
        public void onSensorChanged(CarSensorEvent carSensorEvent) {
            Log.d(TAG, "onSensorChanged: callback carSensorEvent.sensorType = " + carSensorEvent.sensorType);
            switch (carSensorEvent.sensorType) {
                case CarSensorManager.SENSOR_TYPE_GEAR:
                    if(carSensorEvent.intValues.length > 0)  { handleGearPosUpdate(carSensorEvent.intValues[0]);}
                    break;
                case CarSensorManager.SENSOR_TYPE_CAR_SPEED:
                    if(carSensorEvent.floatValues.length > 0)  { handleSpeedUpdate(carSensorEvent.floatValues[0]);}
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
            mCarSensorManager.registerListener(mSenserListener,
                    CarSensorManager.SENSOR_TYPE_CAR_SPEED,CarSensorManager.SENSOR_RATE_NORMAL);
        } catch (CarNotConnectedException e) {
            Log.e(TAG, "Can not connected in SENSOR");
        }
    }

    private MainActivity.Callback mValueCallback = new MainActivity.Callback() {
        @Override
        public void OnGearPosValChange(int propValue) {
            super.OnGearPosValChange(propValue);
            Log.d(TAG, "OnGearPosValChange: GearType = "+propValue*15);
            mCameraView.getVehicleManager().setSteeringAngle(propValue*15);
        }

        @Override
        public void OnSpeedValChange(Float propValue) {
            super.OnSpeedValChange(propValue);
            Log.d(TAG, "OnSpeedValChange: Speed = "+propValue);
            mCameraView.getVehicleManager().setSteeringAngle(propValue);
        }
    };

    private void handleGearPosUpdate(int pos) {
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                mCallbacks.get(i).OnGearPosValChange(pos);
            }
        }
    }
    private void handleSpeedUpdate(float speed) {
        synchronized (mCallbacks) {
            for (int i = 0; i < mCallbacks.size(); i++) {
                mCallbacks.get(i).OnSpeedValChange(speed);
            }
        }
    }
}