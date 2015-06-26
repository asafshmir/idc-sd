package com.idc.sd.t800;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import android.content.Context;
import android.util.Log;

/*
    This class is used for detecting faces in a gray scale image. The class uses opencv's
    CascadeClassifier to detect face-alike objects. The cascade classifier is base on the
    Viola-Jones object detection algorithm, which boost up the face detection (by first using simple
    classifiers which reject most of the false positives).
    This class is based on opencv's face detection example:
    https://github.com/Itseez/opencv/blob/master/samples/android/face-detection/src/org/opencv/samples/facedetect/FdActivity.java
 */
public class FaceDetector {

    private static final String     TAG                 = "T800::FaceDetector";

    private static final float      RELATIVE_FACE_SIZE = 0.2f; // minimal face size relative to screen height

    private Context                 mContext;
    private CascadeClassifier       mJavaDetector;
    private int                     mAbsoluteFaceSize   = 0;

    public FaceDetector(Context context) {
        mContext = context;
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    public void init(){
        try {
            // load cascade file from application resources
            InputStream is = mContext.getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = mContext.getDir("cascade", Context.MODE_PRIVATE);
            File cascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else {
                Log.i(TAG, "Loaded cascade classifier from " + cascadeFile.getAbsolutePath());
            }
            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    public Rect[] detectFaces(Mat gray) {
        if (mAbsoluteFaceSize == 0) {
            int height = gray.rows();
            if (Math.round(height * RELATIVE_FACE_SIZE) > 0) {
                mAbsoluteFaceSize = Math.round(height * RELATIVE_FACE_SIZE);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(gray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        return faces.toArray();
    }
}

