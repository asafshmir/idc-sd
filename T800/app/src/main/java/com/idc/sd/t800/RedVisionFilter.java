package com.idc.sd.t800;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class RedVisionFilter {

    private Mat                     mRedVisionMat;

    public void init() {
        // Fill red vision conversion matrix

        mRedVisionMat = new Mat(4, 4, CvType.CV_32F);
        mRedVisionMat.put(0, 0, /* R */0.999f, 0.999f, 0.999f, 0f);
        mRedVisionMat.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mRedVisionMat.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mRedVisionMat.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    public void process(Mat rgba) {

        // Red Processing optimisation - only calculate two channels.
        ArrayList<Mat> channels = new ArrayList<Mat>();
        Core.split(rgba, channels);
        channels.get(1).convertTo(channels.get(1),-1,0.349f,0);
        channels.get(2).convertTo(channels.get(2),-1,0.272f,0);
        Core.merge(channels,rgba);
        
//        Core.transform(rgba, rgba, mRedVisionMat);

    }
}
