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

public class FaceDetector {

    private static final String    TAG                 = "T800::FaceDetector";

    private Context                 mContext;
    private File                    mCascadeFile;
    private CascadeClassifier       mJavaDetector;

    private float                   mRelativeFaceSize   = 0.2f;
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
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }
    }

    public Rect[] detectFaces(Mat gray) {
        if (mAbsoluteFaceSize == 0) {
            int height = gray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
        }

        MatOfRect faces = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(gray, faces, 1.1, 2, 2,
                    new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());

        return faces.toArray();
    }
}

