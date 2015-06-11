package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.List;

// a detector of colored markers
public abstract class ColoredMarkerDetector {
    abstract public List<Point> detect(Mat rgbaImage);
    abstract public void setHsvColor(Scalar hsvColor);
}
