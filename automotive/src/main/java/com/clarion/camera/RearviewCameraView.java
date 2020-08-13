package com.clarion.camera;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;

public class RearviewCameraView extends CameraPreview {
    private static final String TAG = "RearviewCameraView";

    private VehicleManager mVehicleManager;
    private static Bitmap sOverlayLinesBitmap = null;
    private static Bitmap sSteeringAngleLinesBitmap = null;

    public RearviewCameraView(Context context) {
        super(context);
        Log.d(TAG, "RearviewCameraView: constructed");
        init();
    }

    public RearviewCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "RearviewCameraView: constructed with attributes");
        init();
    }

    private void init() {
        Log.d(TAG, "init: start");
        if(sOverlayLinesBitmap == null) {
            sOverlayLinesBitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.overlay);
        }

        if(sSteeringAngleLinesBitmap == null) {
            sSteeringAngleLinesBitmap = BitmapFactory.decodeResource(getResources(),
                    R.drawable.dynamiclines);
        }

        //[ROHIT]
        mVehicleManager = new VehicleManager();
    }

    @Override
    public void drawOnCanvas(Canvas canvas, Bitmap videoBitmap) {
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(videoBitmap, createVideoFeedMatrix(videoBitmap),
                null);
        canvas.drawBitmap(sOverlayLinesBitmap, createOverlayMatrix(),
                createOverlayPaint());
        canvas.drawBitmap(sSteeringAngleLinesBitmap, createDynamicLinesMatrix(),
                createDynamicLinesPaint());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        Log.d(TAG, "surfaceCreated: start");
        // TODO this is being leaked, does surfaceDestroyed not get called?
//        getContext().bindService(new Intent(getContext(), VehicleManager.class),
//                mVehicleConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed: start");
        super.surfaceDestroyed(holder);
//        getContext().unbindService(mVehicleConnection);
    }

    private Matrix createVideoFeedMatrix(Bitmap bitmap) {
        Matrix videoFeedMatrix = new Matrix();

        videoFeedMatrix.preScale(-computeScreenToFeedWidthRatio(bitmap),
                computeScreenToFeedHeightRatio(bitmap));
        videoFeedMatrix.postTranslate((float)(0.5 * getScreenWidth() +
                        (0.5 * computeAdjustedVideoFeedWidth(bitmap))),
                (float)((0.5 * getScreenHeight()) -
                        (0.5 * computeAdjustedVideoFeedHeight(bitmap))));

        return videoFeedMatrix;
    }

    private Matrix createOverlayMatrix() {
        Matrix overlayMatrix = new Matrix();

        overlayMatrix.preScale(computeScreenToOverlayWidthRatio(),
                computeScreenToOverlayHeightRatio());
        overlayMatrix.postTranslate(computeOverlayHorizontalTranslation(),
                computeOverlayVerticalTranslation());

        return overlayMatrix;
    }

    private Matrix createDynamicLinesMatrix() {
        //place dynamic lines directly on top of overlay by using same
        //translations/ratios
        float screenToDynamicLinesHeightRatio =
                computeScreenToOverlayHeightRatio();
        float screenToDynamicLinesWidthRatio =
                computeScreenToOverlayWidthRatio();
        float dynamicLinesVerticalTranslation =
                computeOverlayVerticalTranslation();
        float dynamicLinesHorizontalTranslation =
                computeOverlayHorizontalTranslation();

        Matrix dynamicLinesMatrix = new Matrix();

        dynamicLinesMatrix.preScale(screenToDynamicLinesWidthRatio,
                screenToDynamicLinesHeightRatio);
        dynamicLinesMatrix.postTranslate(dynamicLinesHorizontalTranslation +
                        3*(float)getSteeringWheelAngle()/2,
                dynamicLinesVerticalTranslation);

        //number divided by must be larger than the maximum absolute value the
        //steering wheel can produce because the x skew must be less than 1
        dynamicLinesMatrix.postSkew((float) -getSteeringWheelAngle() / 480, 0);

        return dynamicLinesMatrix;
    }

    private Paint createOverlayPaint(){
        double steeringWheelAngle = getSteeringWheelAngle();
        Paint overlayPaint = new Paint();

        if (steeringWheelAngle == 0) {
            overlayPaint.setAlpha(255);
        } else if (steeringWheelAngle / 2 > 0 && steeringWheelAngle / 2 <= 255) {
            overlayPaint.setAlpha((int)(255 - steeringWheelAngle / 2));
        } else if (steeringWheelAngle / 2 < 0 && steeringWheelAngle / 2 >= -255) {
            overlayPaint.setAlpha((int)(255 + steeringWheelAngle / 2));
        } else {
            overlayPaint.setAlpha(0);
        }

        return overlayPaint;
    }

    private double getSteeringWheelAngle() {
        double steeringWheelAngle = 0;
//        try {
//            if(mVehicleManager != null) {
//                steeringWheelAngle = ((SteeringWheelAngle)mVehicleManager.get(
//                        SteeringWheelAngle.class)).getValue().doubleValue();
//            }
//        } catch(UnrecognizedMeasurementTypeException e) {
//        } catch(NoValueException e) {
//        }
        steeringWheelAngle = mVehicleManager.getSteeringAngle();
        return steeringWheelAngle;
    }

    private Paint createDynamicLinesPaint() {
        int steeringWheelAngle = (int) getSteeringWheelAngle();
        Paint paint = new Paint();

        if (steeringWheelAngle >= 0 && steeringWheelAngle < 255){
            paint.setAlpha(steeringWheelAngle);
        } else if (steeringWheelAngle < 0 && steeringWheelAngle > -255){
            paint.setAlpha(-steeringWheelAngle);
        } else {
            paint.setAlpha(255);
        }
        return paint;
    }

    /**overlay translation computation methods**/
    private float computeOverlayHorizontalTranslation() {
        return (float)((0.5 * getScreenWidth()) -
                (0.5 * computeAdjustedOverlayWidth()));
    }

    private float computeOverlayVerticalTranslation() {
        return (float)((0.5 * getScreenHeight()) -
                (0.3 * computeAdjustedOverlayHeight()));
    }

    /**screen to overlay ratio computation methods**/
    private float computeScreenToOverlayHeightRatio() {
        return (float)(0.5 * getScreenHeight() /
                sOverlayLinesBitmap.getHeight());
    }

    private float computeScreenToOverlayWidthRatio() {
        return (float)(0.85 * getScreenWidth() /
                sOverlayLinesBitmap.getWidth());
    }

    /**screen to video feed ratio computation methods**/
    private float computeScreenToFeedHeightRatio(Bitmap bitmap) {
        return getScreenHeight() / bitmap.getHeight();
    }

    private float computeScreenToFeedWidthRatio(Bitmap bitmap) {
        return getScreenWidth() / bitmap.getWidth();
    }

    /**adjusted video feed dimensions computation methods**/
    private float computeAdjustedVideoFeedHeight(Bitmap bitmap) {
        return computeScreenToFeedHeightRatio(bitmap) * bitmap.getHeight();
    }

    private float computeAdjustedVideoFeedWidth(Bitmap bitmap) {
        return computeScreenToFeedWidthRatio(bitmap) * bitmap.getWidth();
    }

    /**adjusted overlay dimensions computation methods**/
    private float computeAdjustedOverlayHeight() {
        return computeScreenToOverlayHeightRatio()*sOverlayLinesBitmap.getHeight();
    }

    private float computeAdjustedOverlayWidth() {
        return computeScreenToOverlayWidthRatio()*sOverlayLinesBitmap.getWidth();
    }

    /**get screen dimensions methods**/
    private float getScreenHeight() {
        return getContext().getResources().getDisplayMetrics().heightPixels;
    }

    private float getScreenWidth() {
        return getContext().getResources().getDisplayMetrics().widthPixels;
    }

    public  VehicleManager getVehicleManager(){
        return mVehicleManager;
    }

//    private ServiceConnection mVehicleConnection = new ServiceConnection() {
//        public void onServiceConnected(ComponentName className,
//                                       IBinder service) {
//            Log.i(TAG, "Bound to VehicleManager");
//            mVehicleManager = ((VehicleManager.VehicleBinder)service
//            ).getService();
//        }
//
//        public void onServiceDisconnected(ComponentName className) {
//            Log.w(TAG, "VehicleService disconnected unexpectedly");
//            mVehicleManager = null;
//        }
//    };
}
