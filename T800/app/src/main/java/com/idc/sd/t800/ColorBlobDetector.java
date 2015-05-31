package com.idc.sd.t800;

import android.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ColorBlobDetector {
    private static final String     TAG                 = "T800::ColorBlobDetector";

    private static final Scalar     HSV_COLOR = new Scalar(0,179,204,0);
    private static final Scalar     COLOR_RADIUS = new Scalar(20,76,51,0);
    //private static final Scalar     HSV_COLOR = new Scalar(0,205,220,0);
    //private static final Scalar     COLOR_RADIUS = new Scalar(15,50,35,0);

    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mFirstRangeLowerBound = new Scalar(0);
    private Scalar mFirstRangeUpperBound = new Scalar(0);
    private Scalar mSecondRangeLowerBound = new Scalar(0);
    private Scalar mSecondRangeUpperBound = new Scalar(0);
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(0);

    private List<MatOfPoint> mContours = new ArrayList<>();

    // Cache
    Mat mPyrDownMat;
    Mat mHsvMat;
    Mat mMask;
    Mat mDilatedMask;
    Mat mHierarchy;

    public ColorBlobDetector() {
        this.mColorRadius = COLOR_RADIUS;
        setHsvColor(HSV_COLOR);

        // Cache
        mPyrDownMat = new Mat();
        mHsvMat = new Mat();
        mMask = new Mat();
        mDilatedMask = new Mat();
        mHierarchy = new Mat();
    }

    public void setHsvColor(Scalar hsvColor) {
        mFirstRangeLowerBound.val[0] = (hsvColor.val[0] - mColorRadius.val[0] + 255) % 255;
        mFirstRangeUpperBound.val[0] = (hsvColor.val[0] + mColorRadius.val[0] + 255) % 255;

        mFirstRangeLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mFirstRangeUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mFirstRangeLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mFirstRangeUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mFirstRangeLowerBound.val[3] = 0;
        mFirstRangeUpperBound.val[3] = 255;

        mSecondRangeLowerBound = mFirstRangeLowerBound.clone();
        mSecondRangeUpperBound = mFirstRangeUpperBound.clone();

        // hsv is a cyclic range, so if the lower bound is higher than the upper bound, we need to
        // compute two ranges
        if (mFirstRangeLowerBound.val[0] >= mFirstRangeUpperBound.val[0]) {
            mFirstRangeUpperBound.val[0] = 255;
            mSecondRangeLowerBound.val[0] = 0;
            Log.i(TAG, "Got two ranges: "   + mFirstRangeLowerBound + "-" + mFirstRangeUpperBound + "   "
                                            + mSecondRangeLowerBound + "-" + mSecondRangeUpperBound);
        }
    }

    public void process(Mat rgbaImage) {
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);

        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        Mat firstRangeMask = new Mat();
        Mat secondRangeMask = new Mat();
        Core.inRange(mHsvMat, mFirstRangeLowerBound, mFirstRangeUpperBound, firstRangeMask);
        Core.inRange(mHsvMat, mSecondRangeLowerBound, mSecondRangeUpperBound, secondRangeMask);
        Core.bitwise_or(firstRangeMask, secondRangeMask, mMask);

        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }

        // TODO remove this
        for (int contourIdx = 0; contourIdx < mContours.size(); contourIdx++) {
            Imgproc.drawContours(rgbaImage, mContours, contourIdx, new Scalar(0, 0, 255), 2);
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
