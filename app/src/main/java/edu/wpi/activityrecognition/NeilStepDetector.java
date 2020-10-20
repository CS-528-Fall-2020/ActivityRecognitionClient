package edu.wpi.activityrecognition;

import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class NeilStepDetector {
    private static final int SAMPLE_SIZE_LIMIT = 50;
    private static int sampleCount = 0;
    // change this threshold according to your sensitivity preferences
    private static final float STEP_THRESHOLD = 15f;
    private long lastStepTimeNs = 0;
    private static final int STEP_MAX_NS = 2000000000;
    private static final int STEP_MIN_NS = 200000000;

    private Queue<Float> xArray;
    private Queue<Float> yArray;
    private Queue<Float> zArray;
    private float filteredX;
    private float filteredY;
    private float filteredZ;
    private static float sampleXNew = 0;
    private static float sampleYNew = 0;
    private static float sampleZNew = 0;
    private static float sampleXOld = 0;
    private static float sampleYOld = 0;
    private static float sampleZOld = 0;

    private static float thresholdX = -120;
    private static float thresholdY = -120;
    private static float thresholdZ = -120;

    private static float xmax = -120;
    private static float ymax = -120;
    private static float zmax = -120;
    private static float xmin = 120;
    private static float ymin = 120;
    private static float zmin = 120;

    private StepListener listener;

    public void registerListener(StepListener listener) {
        this.listener = listener;
    }

    public NeilStepDetector() {
        xArray = new LinkedList<>();
        yArray = new LinkedList<>();
        zArray = new LinkedList<>();
    }

    public void updateAccel(long timeNs, float x, float y, float z) {
        updateAndFilter(x, y, z);
        switch (activeAxis()) {
            case "X":
                sampleXOld = sampleXNew;
                sampleXNew = calculateNewSampleValue(filteredX, sampleXNew, timeNs);
                stepCheck(sampleXNew, sampleXOld, thresholdX);
                break;
            case "Y":
                sampleYOld = sampleYNew;
                sampleYNew = calculateNewSampleValue(filteredY, sampleYNew, timeNs);
                stepCheck(sampleYNew, sampleYOld, thresholdY);
                break;
            case "Z":
                sampleZOld = sampleZNew;
                sampleZNew = calculateNewSampleValue(filteredZ, sampleZNew, timeNs);
                stepCheck(sampleZNew, sampleZOld, thresholdZ);
                break;
            default:
                break;

        }
    }

    /**
     * check if step count is increased
     */
    private void stepCheck(float sampleNew, float sampleOld, float threshold) {
        if(sampleNew < sampleOld && sampleOld > threshold && sampleNew < threshold) {
            listener.neilStep(1);
        }
    }

    /**
     * dynamic precision
     * sampleNew -> sampleOld is done in the calling class @updateAccel
     * here we check if the sampleResult -> sample new conditions are met or not
     * (0.2s < timeNs < 2s) time window and interval counter do the same thing so used the timer condition alone
     * countRegulation
     * change in acceleration > threshold
     */
    private float calculateNewSampleValue(float sampleResult, float sampleNew, long timeNs) {
        long timeDiff = timeNs - lastStepTimeNs;
        if (Math.abs(sampleNew - sampleResult) > STEP_THRESHOLD && timeDiff < STEP_MAX_NS && timeDiff > STEP_MIN_NS) {
            Log.i(sampleResult+": Result/New ", sampleNew+" Old");
            sampleNew = sampleResult;
        } else if (lastStepTimeNs == 0 || timeDiff > STEP_MAX_NS){
            lastStepTimeNs = timeNs;
        }
        return sampleNew;
    }

    /**
     * determine the dominant axis
     * i.e. axis with the largest acceleration value
     */
    private String activeAxis() {
        float xdiff = xmax - xmin;
        float ydiff = ymax - ymin;
        float zdiff = zmax - zmin;

        if (xdiff > ydiff && xdiff > zdiff && xdiff > 1) {
            return "X";
        } else if (ydiff > zdiff && ydiff > zdiff && ydiff > 1) {
            return "Y";
        } else if (zdiff > xdiff && zdiff > ydiff && xdiff > 1){
            return "Z";
        } else {
            return "";
        }

    }

    /**
     * calculate the max and min values
     * save the new values into the shift registers
     * sampling counter
     * filtering
     * dynamic threshold calculation
     * reset counter
     * reset max min
     */
    private void updateAndFilter(float x, float y, float z) {
        xmax = Math.max(xmax, x);
        ymax = Math.max(ymax, y);
        zmax = Math.max(zmax, z);
        xmin = Math.min(xmin, x);
        ymin = Math.min(ymin, y);
        zmin = Math.min(zmin, z);

        xArray.add(x);
        yArray.add(y);
        zArray.add(z);

        if (xArray.size() > 4) {
            xArray.remove();
            filteredX = sumFilter(new LinkedList<Float>(xArray));
            yArray.remove();
            filteredY = sumFilter(new LinkedList<Float>(yArray));
            zArray.remove();
            filteredZ = sumFilter(new LinkedList<Float>(zArray));
        }

        sampleCount++;
        if (sampleCount >= SAMPLE_SIZE_LIMIT) {
            sampleCount = 0;
            thresholdX = (xmax + xmin) / 2;
            thresholdY = (ymax + ymin) / 2;
            thresholdZ = (zmax + zmin) / 2;
            xmin = ymin = zmin = 120;
            xmax = ymax = zmax = -120;
        }

    }

    private float sumFilter(Queue<Float> queue) {
        float sum = 0;
        float length = queue.size();
        while (!queue.isEmpty()) {
            sum += queue.remove();
        }
        return (sum / length);
    }
}
