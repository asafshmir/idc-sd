package com.idc.sd.t800;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class PolygonDetector extends ColoredMarkerDetector {


    private static final int            POLYGON_VERTICES    = 3; // Use a triangle marker
    private static final double         APPROX_FACTOR       = 0.3;

    private static final Scalar         HSV_COLOR = new Scalar(247.0,232.0,158.0,0);
    private static final Scalar         COLOR_RADIUS = new Scalar(5,70,70,0);

    private ColorBlobDetector           mDetector;
    private List<MatOfPoint>            mPolygons;

    public PolygonDetector() {
        this.mDetector = new ColorBlobDetector(HSV_COLOR, COLOR_RADIUS);
        this.mPolygons = new ArrayList<>();
    }

    // detect a colored polygon on a given image Mat
    public List<Point> detect(Mat rgbaImage) {
        mDetector.process(rgbaImage);
        List<MatOfPoint> contours = mDetector.getContours();
        List<MatOfPoint> polygons = getValidPolygons(contours);

        // draw the polygon if on debug mode
        if (Debugger.DEBUG) {
            for (int contourIdx = 0; contourIdx < polygons.size(); contourIdx++) {
                Imgproc.drawContours(rgbaImage, polygons, contourIdx, new Scalar(0, 255, 0), 3);
            }
        }

        // return the centers of the detected polygons
        List<Point> markers = new ArrayList<>();
        for (MatOfPoint polygon : polygons) {
            markers.add(ProcessUtils.findCentroid(polygon));
        }

        return markers;
    }

    public void setHsvColor(Scalar hsvColor) {
        mDetector.setHsvColor(hsvColor);
    }
    public void adjustWhiteBalance(Scalar rgbColor) {
        mDetector.adjustWhiteBalance(rgbColor);
        mDetector.setHsvColor(HSV_COLOR);
    }

    // gets a list of contours and returns only the valid polygons
    private List<MatOfPoint> getValidPolygons(List<MatOfPoint> contours) {
        if (mPolygons.size() > 0) { mPolygons.clear(); }
        for (MatOfPoint contour : contours) {
            if (isContourValidPolygon(contour)) {
                mPolygons.add(contour);
            }
        }
        return mPolygons;
    }

    // checks if a given contour is a valid polygon with 'POLYGON_VERTICES' vertices
    private boolean isContourValidPolygon(MatOfPoint contour) {

        Rect ret = null;

        MatOfPoint2f contour2f = new MatOfPoint2f();
        MatOfPoint approxContour = new MatOfPoint();
        MatOfPoint2f approxContour2f = new MatOfPoint2f();

        contour.convertTo(contour2f, CvType.CV_32FC2);

        int contourSize = (int)contour.total();
        Imgproc.approxPolyDP(contour2f, approxContour2f, contourSize * APPROX_FACTOR, true);

        approxContour2f.convertTo(approxContour, CvType.CV_32S);
        if (approxContour.total() == POLYGON_VERTICES) {
            ret = Imgproc.boundingRect(approxContour);
        }

        return (ret != null);
    }
}
