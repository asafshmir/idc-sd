package com.idc.sd.t800;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends ActionBarActivity
        implements CvCameraViewListener2, View.OnTouchListener  {
    private static final String     TAG                 = "T800::MainActivity";

    private static final Scalar     DRAW_COLOR = new Scalar(255,255,255,255);
    private static final int        COLOR_WIN_SIZE = 15;

    private ColoredMarkerDetector   mDetector;
    private PolygonDetector         mPolyDetector;
    private TargetDetector          mTargetDetector;
    private boolean                 mIsTargetDetector = true;

    private FaceTracker             mFaceTracker;
    private Rect[]                  mAliveFacesRects;
    private Rect[]                  mDeadFacesRects;
    private List<Point>             mMarkersCenters;
    private Random                  mRand;

    private RedVisionFilter         mRedFilter;
    private Boolean                 mEnableRedVision = false;
    private Boolean                 mTouchModeKill = true;
    private Scalar                  mSelectedColorRgba = new Scalar(255);
    private Scalar                  mSelectedColorHsv = new Scalar(0,205,220,0);

    private Mat                     mRgba;
    private Mat                     mGray;
    private Size                    mFrameSize;

    private CameraBridgeViewBase    mOpenCvCameraView;
    private BaseLoaderCallback      mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    init();

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
        mMarkersCenters = new ArrayList<>();
        mRand = new Random(new Date().getTime());
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    // init the rest of the members, called when opencv manager is ready
    private void init() {
        mFaceTracker = new FaceTracker(this);
        mFaceTracker.init();
        mPolyDetector = new PolygonDetector();
        mTargetDetector = new TargetDetector();
        mDetector = mIsTargetDetector ? mTargetDetector : mPolyDetector;
        mRedFilter = new RedVisionFilter();
    }

    // called when the activity is first created
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

    public void onCameraViewStarted(int width, int height) {
        mFrameSize = new Size(width, height);
    }

    public void onCameraViewStopped() {
        if (mGray != null) { mGray.release(); }
        if (mRgba != null) { mRgba.release(); }
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

        // set initial icons according to parameters
        (menu.findItem(R.id.action_red_vision)).setIcon(
                mEnableRedVision ?  R.drawable.ic_visibility_off_black_24dp :
                                    R.drawable.ic_visibility_black_24dp);
        (menu.findItem(R.id.action_touch_mode)).setIcon(
                mTouchModeKill ?    R.drawable.ic_colorize_black_24dp :
                                    R.drawable.ic_location_searching_black_24dp);
        (menu.findItem(R.id.action_marker_type)).setIcon(
                mIsTargetDetector ?     R.drawable.ic_details_black_24dp :
                                        R.drawable.ic_album_black_24dp);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            // toggle red vision mode and change icon accordingly
            case R.id.action_red_vision:
                mEnableRedVision = !mEnableRedVision;
                item.setIcon(mEnableRedVision ? R.drawable.ic_visibility_off_black_24dp :
                                                R.drawable.ic_visibility_black_24dp);
                showTooltip(mEnableRedVision ?  getString(R.string.tooltip_red_vision_on) :
                                                getString(R.string.tooltip_red_vision_off));
                return true;
            // toggle touch mode and change icon accordingly
            case R.id.action_touch_mode:
                mTouchModeKill = !mTouchModeKill;
                item.setIcon(mTouchModeKill ?   R.drawable.ic_colorize_black_24dp :
                                                R.drawable.ic_location_searching_black_24dp);
                showTooltip(mTouchModeKill ? getString(R.string.tooltip_touch_mode_kill) :
                        getString(R.string.tooltip_touch_mode_color_pick));
                return true;
            // toggle marker mode and change icon accordingly
            case R.id.action_marker_type:
                mIsTargetDetector = !mIsTargetDetector;
                item.setIcon(mIsTargetDetector ?    R.drawable.ic_details_black_24dp :
                                                    R.drawable.ic_album_black_24dp);
                showTooltip(mIsTargetDetector ?     getString(R.string.tooltip_target_marker) :
                                                    getString(R.string.tooltip_poly_marker));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    // display a tooltip on the screen for a short period of time+
    private void showTooltip(String msg) {
        final TextView txt = (TextView)this.findViewById(R.id.tooltip);
        txt.setText(msg);
        txt.setVisibility(View.VISIBLE);
        CountDownTimer timer = new CountDownTimer(2000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                txt.setVisibility(View.INVISIBLE);
            }
        }.start();
    }

    public boolean onTouch(View v, MotionEvent event) {
        // if touch mode is set to kill, handle kill action
        if (mTouchModeKill) {
            return handleTouchKill(event);
        // else, touch mode is set to adjust marker colors
        } else {
            return handleTouchAdjustMarkerColor(event);
        }
    }

    private boolean handleTouchKill(MotionEvent event) {
        mFaceTracker.handleScreenTouch(extractCoordinates(event));
        return true;
    }

    // change the color of the marker according to the touched point's average color
    private boolean handleTouchAdjustMarkerColor(MotionEvent event) {
        Point point = extractCoordinates(event);
        int x = (int)point.x;
        int y = (int)point.y;

        // find the rectangle around the touched point
        Rect touchedRegion = new Rect();
        touchedRegion.x = (x > COLOR_WIN_SIZE) ? x - COLOR_WIN_SIZE : 0;
        touchedRegion.y = (y > COLOR_WIN_SIZE) ? y - COLOR_WIN_SIZE : 0;
        touchedRegion.width = (x + COLOR_WIN_SIZE < mRgba.cols()) ?
                x + COLOR_WIN_SIZE - touchedRegion.x : mRgba.width() - touchedRegion.x;
        touchedRegion.height = (y + COLOR_WIN_SIZE < mRgba.rows()) ?
                y + COLOR_WIN_SIZE - touchedRegion.y : mRgba.height() - touchedRegion.y;

        Mat touchedRegionMatRgba = mRgba.submat(touchedRegion);
        Mat touchedRegionMatHsv = new Mat();
        Imgproc.cvtColor(touchedRegionMatRgba, touchedRegionMatHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // calc the mean color (hsv color space) in the selected area
        mSelectedColorHsv = Core.sumElems(touchedRegionMatHsv);
        int pointCount = touchedRegion.width * touchedRegion.height;
        for (int i = 0; i < mSelectedColorHsv.val.length; i++) {
            mSelectedColorHsv.val[i] /= pointCount;
        }

        mDetector.setHsvColor(mSelectedColorHsv);

        Log.i(TAG, "HSV: (" + mSelectedColorHsv.val[0] + ", " + mSelectedColorHsv.val[1] +
                ", " + mSelectedColorHsv.val[2] + ", " + mSelectedColorHsv.val[3] + ")");
        return true;
    }

    // translate touch point from screen coordinates to frame coordinates
    private Point extractCoordinates(MotionEvent event) {
        int cols = mRgba.cols();
        int rows = mRgba.rows();
        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;
        return new Point(x, y);
    }

    private void process() {

        // TODO use tone mapping to fix colors?

        // detect markers
        mDetector = mIsTargetDetector ? mTargetDetector : mPolyDetector;
        mMarkersCenters = mDetector.detect(mRgba);

        // detect faces, classify them using the detected markers, and store data
        mFaceTracker.process(mGray, mMarkersCenters);
        Pair<Rect[], Rect[]> facesRects = mFaceTracker.getFaceRectangles();
        mAliveFacesRects =  facesRects.first;
        mDeadFacesRects =  facesRects.second;
    }

    // TODO remove to different class?
    private void draw() {

        // Draw a skull on-top of detected faces that are 'dead'
        for (Rect faceRect : mDeadFacesRects) {
            drawDeadFace(faceRect);
            drawText(faceRect);
        }

        // apply red vision
        if (mEnableRedVision) mRedFilter.process(mRgba);

        // draw rectangles around detected faces that are 'alive'
        for (Rect faceRect : mAliveFacesRects) {
            Core.rectangle(mRgba, faceRect.tl(), faceRect.br(), DRAW_COLOR, 3);
            drawText(faceRect);
        }

        // draw markers centers
        for (Point center : mMarkersCenters) {
            Core.circle(mRgba, center, 3, DRAW_COLOR);
        }
   }

    private void drawText(Rect face) {

        // Default values for text drawing
        int font = Core.FONT_HERSHEY_PLAIN;
        Scalar white = new Scalar(255, 255, 255);
        double scale = 1.0;

        String matchText = "Match";

        String[] leftText = new String[] {"Threat Assesment",
                String.valueOf(mRand.nextInt(8999) + 1000),
                String.valueOf(mRand.nextInt(8999) + 1000),
                String.valueOf(mRand.nextInt(8999) + 1000)};

        String[] rightText = new String[]{"Analysis",
                "HEAD " + String.valueOf(mRand.nextInt(8999) + 1000)};

        Point matchPoint = new Point(face.tl().x + (face.size().width / 2) - (matchText.length()*10/2),
                face.br().y + 20);
        Point leftPoint = new Point((face.tl().x - getTextSize(leftText[0])) > 0 ? face.tl().x - getTextSize(leftText[0]) : 0,
                face.br().y - (face.size().height / 2));
        Point rightPoint = new Point(face.br().x + 12,
                face.br().y - (face.size().height / 2));

        // TODO use match string only if matched
        if (matchPoint.y < mRgba.rows() - 20) {
            Core.putText(mRgba, matchText, matchPoint, font, scale, white);
        }

        if (leftPoint.x > 0) {
            for (String s : leftText) {
                Core.putText(mRgba, s, leftPoint, font, scale, white);
                leftPoint = new Point(leftPoint.x, leftPoint.y + 20);
            }
        }

        if (rightPoint.x < mRgba.cols() - getTextSize(rightText[0])) {
            for (String s : leftText) {
                Core.putText(mRgba, s, rightPoint, font, scale, white);
                rightPoint = new Point(rightPoint.x, rightPoint.y + 20);
            }
        }
    }

    // TODO set text size according to mFrameSize
    private int getTextSize(String text) {
        // Assuming 11 pixels per character
        return text.length()*11;
    }

    private void drawDeadFace(Rect faceRect) {

        // add canny effect to the part in the frame where the face appears
        Mat faceMat = mRgba.submat(faceRect);
        Mat faceMatGray = new Mat();
        Imgproc.cvtColor(faceMat, faceMatGray, Imgproc.COLOR_RGBA2GRAY);
        double mean = Core.mean(faceMatGray).val[0];
        Imgproc.Canny(faceMatGray, faceMatGray, 0.66*mean, 1.33*mean);
        Mat faceMatThresh = new Mat();
        Imgproc.cvtColor(faceMatGray, faceMatThresh, Imgproc.COLOR_GRAY2RGBA);
        faceMatThresh.copyTo(faceMat);
    }

    // TODO white balance
    /*  capture white paper
        calculate its mean RGBA
        calc:
        tranform matrix = (real white)^(-1) * (mean capture pixel)
     */
}
