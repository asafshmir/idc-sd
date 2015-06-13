package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class TargetDetector extends ColoredMarkerDetector {

    private static final Scalar         DEFAULT_OUTER_HSV_COLOR = new Scalar(1.0,188.0,204.0,0);
    private static final Scalar         OUTER_COLOR_RADIUS = new Scalar(10,70,70,0);
    private static final Scalar         DEFAULT_INNER_HSV_COLOR = new Scalar(160.0,211.0,105.0,0);
    private static final Scalar         INNER_COLOR_RADIUS = new Scalar(15,70,70,0);

    private ColorBlobDetector           mOuterCircleDetector;
    private ColorBlobDetector           mInnerCircleDetector;
    private List<Point>                 mTargets;
    private List<Point>                 mOuterCenters;
    private List<Rect>                  mInnerRects;

    public TargetDetector() {
        this.mOuterCircleDetector = new ColorBlobDetector(DEFAULT_OUTER_HSV_COLOR, OUTER_COLOR_RADIUS);
        this.mInnerCircleDetector = new ColorBlobDetector(DEFAULT_INNER_HSV_COLOR, INNER_COLOR_RADIUS);
        mTargets = new ArrayList<>();
        mOuterCenters = new ArrayList<>();
        mInnerRects = new ArrayList<>();
    }

    public List<Point> detect(Mat rgbaImage) {

        // detect the outer contour
        mOuterCircleDetector.process(rgbaImage);
        List<MatOfPoint> outerContours = mOuterCircleDetector.getContours();
        mOuterCenters.clear();
        for (MatOfPoint contour : outerContours) {
            mOuterCenters.add(ProcessUtils.findCentroid(contour));
        }

        // detect the inner contour
        mInnerCircleDetector.process(rgbaImage);
        List<MatOfPoint> innerContours = mInnerCircleDetector.getContours();
        mInnerRects.clear();
        for (MatOfPoint contour : innerContours) {
            mInnerRects.add(Imgproc.boundingRect(contour));
        }

        // try to match outer centers with inner rectangles
        mTargets.clear();
        for (Point outerCenter : mOuterCenters) {
            for (Rect innerRect : mInnerRects) {
                if (innerRect.contains(outerCenter)) {
                    mTargets.add(ProcessUtils.findCenter(innerRect));
                    break;
                }
            }
        }

        return mTargets;
    }

    public void setHsvColor(Scalar hsvColor) {
        mOuterCircleDetector.setHsvColor(hsvColor);
    }

    public void adjustWhiteBalance(Scalar rgbColor) {
        mOuterCircleDetector.adjustWhiteBalance(rgbColor);
        mOuterCircleDetector.setHsvColor(DEFAULT_OUTER_HSV_COLOR);
        mInnerCircleDetector.adjustWhiteBalance(rgbColor);
        mInnerCircleDetector.setHsvColor(DEFAULT_INNER_HSV_COLOR);
    }
}
