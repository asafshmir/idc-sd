package com.idc.sd.t800;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class PolygonDetector {

    private static final Scalar         MARKER_COLOR_TRES   = new Scalar(25,70,70,0);
    private static final double         APPROX_FACTOR       = 0.3;

    private ColorBlobDetector           mDetector;
    private Scalar                      mColor;
    private int                         mNumVertices;


    public PolygonDetector(Scalar color, int numVertices) {
        mColor = color;
        mNumVertices = numVertices;
    }

    public void init() {
        mDetector = new ColorBlobDetector(mColor, MARKER_COLOR_TRES);
    }

    public List<MatOfPoint> detectPolygons(Mat rgbaImage) {
        mDetector.process(rgbaImage);
        List<MatOfPoint> contours = mDetector.getContours();
        smoothContours(contours);

        // Filter in only the right polygons
        return filterContours(contours, mNumVertices);
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

    private List<MatOfPoint> filterContours(List<MatOfPoint> contours, int numVertices) {
        List<MatOfPoint> filtered = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if ((contours.get(i)).total() == numVertices) {
                filtered.add(contours.get(i));
            }
        }
        return filtered;
    }

}
