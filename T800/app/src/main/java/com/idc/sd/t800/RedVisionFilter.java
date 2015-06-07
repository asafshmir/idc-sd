package com.idc.sd.t800;


import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class RedVisionFilter {

    private Mat                     mRedVisionMat;
    private ArrayList<Mat>          mChannels;


    public void init() {
        // Fill red vision conversion matrix
        // TODO remove unused
        //mRedVisionMat = new Mat(4, 4, CvType.CV_32F);
        //mRedVisionMat.put(0, 0, /* R */0.999f, 0.999f, 0.999f, 0f);
        //mRedVisionMat.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        //mRedVisionMat.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        //mRedVisionMat.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);

        mChannels = new ArrayList<>();
        mChannels.add(new Mat()); // Red
        mChannels.add(new Mat()); // Green
        mChannels.add(new Mat()); // Blue
    }

    public void process(Mat rgba) {

        // Red Processing optimisation - only calculate two channels.
        Core.extractChannel(rgba, mChannels.get(0), 0);
        Core.extractChannel(rgba, mChannels.get(1), 1);
        Core.extractChannel(rgba, mChannels.get(2), 2);
        mChannels.get(1).convertTo(mChannels.get(1),-1,0.349f,0);
        mChannels.get(2).convertTo(mChannels.get(2),-1,0.272f,0);
        Core.merge(mChannels, rgba);

        //Core.transform(rgba, rgba, mRedVisionMat);
    }
}
