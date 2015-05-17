package com.idc.sd.t800;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String     TAG                 = "T800::MainActivity";

    private static final Scalar     FACE_RECT_COLOR     = new Scalar(255,255,255,255);

    private static final Scalar     MARKER_COLOR        = new Scalar(0,210,210,0);
    private static final int        POLYGON_VERTICES    = 3; // Use a triangle marker
    private static final int        MARKER_RADIUS       = 10;


    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;

    private FaceDetector            mFaceDetector;
    private PolygonTracker          mPolyTracker;
    private RedVisionFilter         mRedFilter;

    private Rect[]                  mFacesRects;
    private List<Point>             mPolyCenters;

    private MenuItem                mItemEnableRedVision;
    private Boolean                 mEnableRedVision = false;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    mFaceDetector.init();
                    mPolyTracker.init();
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
        mPolyTracker = new PolygonTracker(MARKER_COLOR, POLYGON_VERTICES);
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

        this.process();
        this.drawMarkers();

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemEnableRedVision = menu.add("Toggle Red Vision");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemEnableRedVision)
            mEnableRedVision = !mEnableRedVision;
        return true;
    }

    private void process() {

        // Detect faces
        mFacesRects = mFaceDetector.detectFaces(mRgba, mGray);

        // Detect markers
        mPolyCenters = mPolyTracker.process(mRgba);

        // Apply red vision
        if (mEnableRedVision) mRedFilter.process(mRgba);
    }

    private void drawMarkers() {

        // Draw rectangles around detected faces
        for (Rect facesRect : mFacesRects) {
            Core.rectangle(mRgba, facesRect.tl(), facesRect.br(), FACE_RECT_COLOR, 3);
        }


        // Draw the markers centers
        for (Point center : mPolyCenters) {
            if (center != null) {
                Core.circle(mRgba, center, MARKER_RADIUS, MARKER_COLOR);
            }
        }
    }
}
