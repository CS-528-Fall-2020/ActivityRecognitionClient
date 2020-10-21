package edu.wpi.activityrecognition;

public interface StepListener {
    public void step(long timeNs);
    public void neilStep(int updateCount);
    public void geoFenceTrigger(String fenceName);
    public void handleUserActivity(int type, int confidence);
}
