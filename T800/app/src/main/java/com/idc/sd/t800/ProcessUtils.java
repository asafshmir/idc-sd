package com.idc.sd.t800;

import android.graphics.Color;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

/*
    A utilities class for processing - color space conversion, geometrical operations etc.
 */
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

    // wrapper to work with Scalar
    public static Scalar rgbToHsv(Scalar rgbColor) {
        return rgbToHsv((int) rgbColor.val[0], (int) rgbColor.val[1], (int) rgbColor.val[2]);
    }

    // convert RGB value to HSV values
    // the returned hsv value is in range: (0-255, 0-255, 0-255)
    public static Scalar rgbToHsv(int red, int green, int blue) {
        float[] hsv = new float[3];
        Color.RGBToHSV(red, green, blue, hsv);
        return new Scalar(hsv[0] * 255.0 / 360.0, hsv[1] * 255.0, hsv[2] * 255.0);
    }

    // wrapper to work with Scalar - assume the hsv is given in range: (0-255, 0-255, 0-255)
    public static Scalar hsvToRgb(Scalar hsvColor) {
        Scalar NormalizedRgb = hsvToRgb(hsvColor.val[0] / 255.0, hsvColor.val[1] / 255.0, hsvColor.val[2] / 255.0);
        return new Scalar(NormalizedRgb.val[0]*255.0, NormalizedRgb.val[1]*255.0, NormalizedRgb.val[2]*255.0);
    }

    // convert HSV value to RGB values
    // taken from http://stackoverflow.com/questions/7896280/converting-from-hsv-hsb-in-java-to-rgb-without-using-java-awt-color-disallowe
    private static Scalar hsvToRgb(double hue, double saturation, double value) {

        int h = (int)(hue * 6);
        double f = hue * 6 - h;
        double p = value * (1 - saturation);
        double q = value * (1 - f * saturation);
        double t = value * (1 - (1 - f) * saturation);

        switch (h) {
            case 0: return new Scalar(value, t, p);
            case 1: return new Scalar(q, value, p);
            case 2: return new Scalar(p, value, t);
            case 3: return new Scalar(p, q, value);
            case 4: return new Scalar(t, p, value);
            case 5: return new Scalar(value, p, q);
            default: throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }
    }

    // calc the mean color in the given Mat (works both on RGB and HSV space)
    public static Scalar findMeanColor(Mat mat) {
        Scalar meanColor = Core.sumElems(mat);
        int pixels = mat.width() * mat.height();
        for (int i = 0; i < meanColor.val.length; i++) {
            meanColor.val[i] /= pixels;
        }
        return meanColor;
    }
}

