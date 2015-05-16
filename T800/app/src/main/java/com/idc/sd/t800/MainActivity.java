package com.idc.sd.t800;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String     TAG                 = "T800::MainActivity";

    private static final Scalar     FACE_RECT_COLOR     = new Scalar(255,255,255,255);

    private static final Scalar     MARKER_COLOR        = new Scalar(0,210,210,0);
    private static final int        MARKER_VERTICES     = 3; // Use a triangle marker
    private static final Scalar     MARKER_BORDER_COLOR = new Scalar(0,255,0,255);


    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;

    private FaceDetector            mFaceDetector;
    private PolygonDetector         mPolyDetector;
    private RedVisionFilter         mRedFilter;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    mFaceDetector.init();
                    mPolyDetector.init();
                    mRedFilter.init();

                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        mFaceDetector = new FaceDetector(this);
        mPolyDetector = new PolygonDetector(MARKER_COLOR, MARKER_VERTICES);
        mRedFilter = new RedVisionFilter();
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
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {}

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
        mRedFilter.process(mRgba);

        /* Color faces rect */
        for (int i = 0; i < facesArray.length; i++)
            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        showMarkers();

        return mRgba;
    }

    private void showMarkers() {
        List<MatOfPoint> markers = mPolyDetector.detectPolygons(mRgba);
        Imgproc.drawContours(mRgba, markers, -1, MARKER_BORDER_COLOR);
    }
}
