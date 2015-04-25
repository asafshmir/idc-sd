package com.idc.sd.t800;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG                 = "T800::MainActivity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(255, 255, 255, 255);

    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;
    private Mat                     mRedVisionMat;
    private FaceDetector            mFaceDetector;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mFaceDetector.initFaceDetector();
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        mFaceDetector = new FaceDetector(this);
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.main_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        initRedVision();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        /* Detect faces */
        Rect[] facesArray = mFaceDetector.detectFaces(mRgba, mGray);

        /* Apply red vision */
        applyRedVision(mRgba);

        /* Color faces rect */
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        return mRgba;
    }

    private void initRedVision() {
        // Fill red vision conversion matrix
        mRedVisionMat = new Mat(4, 4, CvType.CV_32F);
        mRedVisionMat.put(0, 0, /* R */0.999f, 0.999f, 0.999f, 0f);
        mRedVisionMat.put(1, 0, /* G */0.168f, 0.686f, 0.349f, 0f);
        mRedVisionMat.put(2, 0, /* B */0.131f, 0.534f, 0.272f, 0f);
        mRedVisionMat.put(3, 0, /* A */0.000f, 0.000f, 0.000f, 1f);
    }

    private void applyRedVision(Mat rgba) {
        Size sizeRgba = rgba.size();

        Mat rgbaInnerWindow;

        int rows = (int) sizeRgba.height;
        int cols = (int) sizeRgba.width;

        int left = 0;//cols / 200;
        int top = 0;//rows / 200;

        int width = cols;// * 99 / 100;
        int height = rows;// * 99 / 100;

        rgbaInnerWindow = rgba.submat(top, top + height, left, left + width);
        Core.transform(rgbaInnerWindow, rgbaInnerWindow, mRedVisionMat);
        rgbaInnerWindow.release();
    }
}
