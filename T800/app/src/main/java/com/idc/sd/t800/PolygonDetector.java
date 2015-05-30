package com.idc.sd.t800;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class PolygonDetector {


    private static final int            POLYGON_VERTICES    = 3; // Use a triangle marker
    private static final double         APPROX_FACTOR       = 0.3;

    private ColorBlobDetector           mDetector;

    public void init() {
        mDetector = new ColorBlobDetector();
    }

    public List<MatOfPoint> detectPolygons(Mat rgbaImage) {
        mDetector.process(rgbaImage);
        List<MatOfPoint> contours = mDetector.getContours();
        smoothContours(contours);

        // Filter in only the polygons with mNumVertices
        return filterContours(contours);
    }

    private void smoothContours(List<MatOfPoint> contours) {

        MatOfPoint2f curCurve;
        MatOfPoint2f approxCurve = new MatOfPoint2f();

        for (int i = 0; i < contours.size(); i++) {
            MatOfPoint curCont = contours.get(i);
            curCurve = new MatOfPoint2f(curCont.toArray());
            int contourSize = (int)curCont.total();
            Imgproc.approxPolyDP(curCurve, approxCurve, contourSize * APPROX_FACTOR, true);
            approxCurve.convertTo(curCont, CvType.CV_32S);
        }
    }

    private List<MatOfPoint> filterContours(List<MatOfPoint> contours) {
        List<MatOfPoint> filtered = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if ((contours.get(i)).total() == POLYGON_VERTICES) {
                filtered.add(contours.get(i));
            }
        }
        return filtered;
    }

}
