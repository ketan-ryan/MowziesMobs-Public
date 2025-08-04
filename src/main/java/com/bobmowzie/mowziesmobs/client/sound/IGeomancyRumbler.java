package com.bobmowzie.mowziesmobs.client.sound;

public interface IGeomancyRumbler {
    public boolean isRumbling();
    public boolean isFinishedRumbling();

    public default float getRumbleVolume() {
        return 1;
    }
    public default float getRumblePitch(){
        return 1;
    }

    public float getRumblerX();
    public float getRumblerY();
    public float getRumblerZ();
}
