package com.idc.sd.t800;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String     TAG                 = "T800::MainActivity";

    private static final Scalar     FACE_DRAW_COLOR = new Scalar(255,255,255,255);

    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;

    private FaceTracker             mFaceTracker;
    private List<Point>             mFaceCenters;
    private Rect[]                  mFacesRects;

    private PolygonDetector         mPolyDetector;

    private RedVisionFilter         mRedFilter;
    private Boolean                 mEnableRedVision = false;

    private Scalar                  mBlobColorHsv;
    private Scalar                  mBlobColorRgba;

    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");


                    mFaceTracker.init();
                    mPolyDetector.init();
                    mRedFilter.init();

                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    mOpenCvCameraView.enableView();
                } break;
                default: {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public MainActivity() {
        mFaceTracker = new FaceTracker(this);
        mPolyDetector = new PolygonDetector();
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
        this.draw();

        return mRgba;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item.getItemId() == R.id.action_red_vision)
            mEnableRedVision = !mEnableRedVision;
        return true;
    }

    private void process() {

        // TODO use tone mapping to fix colors?

        // Detect markers
        List<MatOfPoint> markers = mPolyDetector.detectPolygons(mRgba);

        // Detect faces
        mFaceTracker.process(mGray, markers);
        mFaceCenters = mFaceTracker.getValidTrackedCenters();
        mFacesRects = mFaceTracker.getBoundingRectangles();

        // TODO match faces with markers

        // Apply red vision
        if (mEnableRedVision) mRedFilter.process(mRgba);
    }

    private void draw() {

        // TODO remove change of colors
        Scalar color = new Scalar(FACE_DRAW_COLOR.val[0], FACE_DRAW_COLOR.val[1],
                                    FACE_DRAW_COLOR.val[2], FACE_DRAW_COLOR.val[3]);

        // Draw rectangles around detected faces
        for (Rect facesRect : mFacesRects) {
            Core.rectangle(mRgba, facesRect.tl(), facesRect.br(), color, 3);
            color.val[0] -= 20;
            color.val[1] -= 20;
        }

    }

    // TODO remove this
    public boolean onTouch(View v, MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>4) ? x-4 : 0;
        touchedRect.y = (y>4) ? y-4 : 0;

        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
        Log.i(TAG, "Touched hsv color: (" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")");

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
}
