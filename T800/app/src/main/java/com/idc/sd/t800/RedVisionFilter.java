package com.idc.sd.t800;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class RedVisionFilter {

    private Mat                     mRedVisionMat;
    private ArrayList<Mat>          mChannels;


    public RedVisionFilter() {
        mChannels = new ArrayList<>();
        mChannels.add(new Mat()); // Red
        mChannels.add(new Mat()); // Green
        mChannels.add(new Mat()); // Blue
    }

    // red vision - transform the given image Mat in a way that highlights the red colors
    public void process(Mat rgba) {

        // extract the green and blue channels, and transform them so the red channel will be
        // the most apparent
        Core.extractChannel(rgba, mChannels.get(0), 0);
        Core.extractChannel(rgba, mChannels.get(1), 1);
        Core.extractChannel(rgba, mChannels.get(2), 2);
        mChannels.get(1).convertTo(mChannels.get(1), -1, 0.349f,0);
        mChannels.get(2).convertTo(mChannels.get(2), -1, 0.272f,0);
        Core.merge(mChannels, rgba);
    }
}
