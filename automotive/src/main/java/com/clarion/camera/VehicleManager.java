package com.clarion.camera;

import android.content.Context;

public class VehicleManager {
    private double m_steeringAngle = 0;

    public VehicleManager() {
    }

    public double getSteeringAngle() {
        return m_steeringAngle;
    }

    public void setSteeringAngle(float angle) {
         m_steeringAngle = angle;
    }
}
