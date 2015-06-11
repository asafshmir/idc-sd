package com.idc.sd.t800;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class ColorBlobDetector {
    private static final String     TAG                 = "T800::ColorBlobDetector";

    private static final double     MIN_CONTOUR_AREA_FACTOR = 0.1;

    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mFirstRangeLowerBound = new Scalar(0);
    private Scalar mFirstRangeUpperBound = new Scalar(0);
    private Scalar mSecondRangeLowerBound = new Scalar(0);
    private Scalar mSecondRangeUpperBound = new Scalar(0);

    private Scalar mColorRadius;
    private List<MatOfPoint> mContours = new ArrayList<>();

    // Cache
    Mat mPyrDownMat;
    Mat mHsvMat;
    Mat mHsv1;
    Mat mHsv2;
    Mat mMask;
    Mat mDilatedMask;
    Mat mHierarchy;

    public ColorBlobDetector(Scalar hsvColor, Scalar colorRadius) {
        // cache
        mPyrDownMat = new Mat();
        mHsvMat = new Mat();
        mHsv1 = new Mat();
        mHsv2 = new Mat();
        mMask = new Mat();
        mDilatedMask = new Mat();
        mHierarchy = new Mat();

        // color configuration
        this.mColorRadius = colorRadius;
        setHsvColor(hsvColor);
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
        // downs-ample image
        Imgproc.pyrDown(rgbaImage, mPyrDownMat);
        Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

       // take only pixels withing selected range
        Core.inRange(mHsvMat, mFirstRangeLowerBound, mFirstRangeUpperBound, mHsv1);
        Core.inRange(mHsvMat, mSecondRangeLowerBound, mSecondRangeUpperBound, mHsv2);
        Core.bitwise_or(mHsv1, mHsv2, mMask);

        Imgproc.dilate(mMask, mDilatedMask, new Mat());

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);
            maxArea = Math.max(area, maxArea);
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        for (MatOfPoint contour : contours) {
            if (Imgproc.contourArea(contour) > MIN_CONTOUR_AREA_FACTOR * maxArea) {
                Core.multiply(contour, new Scalar(4,4), contour);
                mContours.add(contour);
            }
        }

        // draw the blob if on debug mode
        if (Debugger.DEBUG) {
            for (int contourIdx = 0; contourIdx < mContours.size(); contourIdx++) {
                Imgproc.drawContours(rgbaImage, mContours, contourIdx, new Scalar(0, 0, 255), 2);
            }
        }
    }

    public List<MatOfPoint> getContours() {
        return mContours;
    }
}