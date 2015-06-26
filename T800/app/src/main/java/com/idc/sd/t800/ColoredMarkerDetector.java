package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.List;

/*
    An abstract class representing a detector of colored markers in an image.
    The main method - detect - is given a RGBA image, and returns a list of points representing
    the detected markers in the image.
 */
public abstract class ColoredMarkerDetector {
    abstract public List<Point> detect(Mat rgbaImage);
    abstract public void setHsvColor(Scalar hsvColor);
}
