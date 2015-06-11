package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;


public class ProcessUtils {

    // calc euclidean distance between two given points
    public static double pointDistance(Point p, Point q) {
        double xDiff = p.x - q.x;
        double yDiff = p.y - q.y;
        return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
    }

    // calc the center of a given rectangle
    public static Point findCenter(Rect rect) {
        Point center = new Point();
        center.x = rect.x + (rect.width / 2);
        center.y = rect.y + (rect.height / 2);
        return center;
    }

    // calc the centroid of a given polygon
    public static Point findCentroid(MatOfPoint polygon) {
        Moments moments = Imgproc.moments(polygon);
        Point centroid = new Point();
        centroid.x = moments.get_m10() / moments.get_m00();
        centroid.y = moments.get_m01() / moments.get_m00();
        return centroid;
    }

    // return the intersecting rectangle of two given rectangles, or null if they doesn't intersect
    public static Rect intersection(Rect r1, Rect r2) {

        // calc upper-left and lower-right coordinates of two rectangles
        int x1L = r1.x;
        int x1R = x1L + r1.width;
        int y1L = r1.y;
        int y1R = y1L + r1.height;
        int x2L = r2.x;
        int x2R = x2L + r2.width;
        int y2L = r2.y;
        int y2R = y2L + r2.height;

        // find intersection:
        int xL = Math.max(x1L, x2L);
        int xR = Math.min(x1R, x2R);
        if (xR <= xL)
            return null;
        else {
            int yL = Math.max(y1L, y2L);
            int yR = Math.min(y1R, y2R);
            if (yR <= yL)
                return null;
            else
                return new Rect(xL, yL, xR-xL, yR-yL);
        }
    }
}

