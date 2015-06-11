package com.idc.sd.t800;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import java.util.Date;
import java.util.Random;

/**
 * Created by jonatan on 6/7/15.
 */
public class TextDrawer {

    // Assuming 11 pixels per character on 320*240 frame size
    private final int               PIXEL_SIZE = 11;
    private final int               BOTTOM_MARGIN = 20;
    private final int               TEXT_MARGIN = 20;
    private final int               DEFAULT_WIDTH = 320;
    private final int               DEFAULT_HEIGHT = 240;
    private final double            DEFAULT_SCALE = 1.0;
    private final int               MAX_RAND = 8999;
    private final int               MIN_RAND = 1000;

    private Random                  mRand;
    private int                     mFont;
    private Scalar                  mWhite;
    private double                  mScale;
    private Size                    mFrameSize;

    public TextDrawer() {
        mRand = new Random(new Date().getTime());
        mFont = Core.FONT_HERSHEY_PLAIN;
        mWhite = new Scalar(255, 255, 255);
        mScale = DEFAULT_SCALE;
        // Set default frame size
        mFrameSize = new Size(320,240);
    }

    public  void drawDead(Mat mRgba, Rect face) {

        String bottomText = "Target Destroyed";

        String[] leftText = new String[] {};

        String[] rightText = new String[]{};
        drawText(mRgba,face,bottomText,leftText,rightText);
    }

    public  void drawTarget(Mat mRgba, Rect face) {

        String bottomText = "Match";

        String[] leftText = new String[] {"Threat",
                "Assesment",
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND),
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND),
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND)};

        String[] rightText = new String[]{"Analysis",
                "HEAD " + String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND)};
        drawText(mRgba,face,bottomText,leftText,rightText);
    }

    public  void drawInnocent(Mat mRgba, Rect face) {

        String bottomText = "No Match";

        String[] leftText = new String[] {"Threat",
                "Assesment",
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND),
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND),
                String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND)};

        String[] rightText = new String[]{"Analysis",
                "HEAD " + String.valueOf(mRand.nextInt(MAX_RAND) + MIN_RAND)};
        drawText(mRgba,face,bottomText,leftText,rightText);
    }



    private void drawText(Mat mRgba, Rect face, String bottomText,String[] leftText, String[] rightText ) {
        mScale = calcScale(face);
        Point bottomPoint = new Point(face.tl().x + (face.size().width / 2) - (bottomText.length()*10*mScale/2),
                face.br().y + BOTTOM_MARGIN*mScale);
        Point leftPoint = new Point((face.tl().x - getTextSize(leftText)) > 0 ? face.tl().x - getTextSize(leftText) : 0,
                face.br().y - (face.size().height / 2));
        Point rightPoint = new Point(face.br().x + 12*mScale,
                face.br().y - (face.size().height / 2));

        if (bottomPoint.y < mRgba.rows() - 20*mScale) {
            Core.putText(mRgba, bottomText, bottomPoint, mFont, mScale, mWhite);
        }

        if (leftPoint.x > 0) {
            for (String s : leftText) {
                Core.putText(mRgba, s, leftPoint, mFont, mScale, mWhite);
                leftPoint = new Point(leftPoint.x, leftPoint.y + TEXT_MARGIN*mScale);
            }
        }

        if (rightPoint.x < mRgba.cols() - getTextSize(rightText)) {
            for (String s : rightText) {
                Core.putText(mRgba, s, rightPoint, mFont, mScale, mWhite);
                rightPoint = new Point(rightPoint.x, rightPoint.y + TEXT_MARGIN*mScale);
            }
        }
    }

    public void setFrameSize(Size size) {
        mFrameSize = size;
    }

    private double calcScale(Rect faceRect) {
        double scale = DEFAULT_SCALE;
        if ((int)(mFrameSize.width / DEFAULT_WIDTH) > 0) {
            scale =  (mFrameSize.width / DEFAULT_WIDTH);
        } else if ((int)(mFrameSize.height / DEFAULT_HEIGHT) > 0) {
            scale = (mFrameSize.height / DEFAULT_HEIGHT);
        }

        return scale * Math.max(faceRect.size().width / (DEFAULT_WIDTH/2*scale),
                                faceRect.size().height / (DEFAULT_HEIGHT/2*scale));

    }

    private int getTextSize(String[] texts) {
        int maxTextSize = 0;
        for (int i = 0; i < texts.length; i++) {
            if (getTextSize(texts[i])>maxTextSize)
                maxTextSize = getTextSize(texts[i]);
        }
        return maxTextSize;
    }


    private int getTextSize(String text) {
        return (int)(text.length()*PIXEL_SIZE*mScale);
    }

}
