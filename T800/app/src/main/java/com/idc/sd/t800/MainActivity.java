package com.idc.sd.t800;

import android.app.Activity;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
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
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements CvCameraViewListener2, View.OnTouchListener {
    private static final String     TAG                 = "T800::MainActivity";

    private static final Scalar     FACE_DRAW_COLOR = new Scalar(255,255,255,255);

    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;

    private FaceTracker             mFaceTracker;
    private Rect[]                  mAliveFacesRects;
    private Rect[]                  mDeadFacesRects;
    private Mat                     mDeadFaceImg;
    private int[]                   mTextures;

    private PolygonDetector         mPolyDetector;

    private RedVisionFilter         mRedFilter;
    private Boolean                 mEnableRedVision = false;

    private Random                  mRand;
 
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");

                    mFaceTracker.init();
                    mPolyDetector.init();
                    mRedFilter.init();
                    initResources();

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
        mRand = new Random(new Date().getTime());
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    // TODO remove unused code
    private void initResources() {
        try {
            mDeadFaceImg = Utils.loadResource(MainActivity.this, R.raw.skull1, CvType.CV_8UC4);
            mTextures = new int[1];
            GLES20.glGenTextures ( 1, mTextures, 0 );
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextures[0]);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        } catch (IOException e) {
            Log.e(TAG, "Can't find image resource");
            System.exit(1);
        }
    }


    private void initTextures() {

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

    public void onCameraViewStarted(int width, int height) {}

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

    // TODO remove menu?
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

    public boolean onTouch(View v, MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        mFaceTracker.handleScreenTouch(new Point(x, y));
        return true;
    }

    private void process() {

        // TODO use tone mapping to fix colors?
        // Apply red vision
        if (mEnableRedVision) mRedFilter.process(mRgba);
        
        // detect markers
        List<MatOfPoint> markers = mPolyDetector.detectPolygons(mRgba);

        // detect faces and store data
        mFaceTracker.process(mGray, markers);
        Pair<Rect[], Rect[]> facesRects = mFaceTracker.getFaceRectangles();
        mAliveFacesRects =  facesRects.first;
        mDeadFacesRects =  facesRects.second;
    }

    private void draw() {
        matToTexture(mRgba);
        // apply red vision
        if (mEnableRedVision) mRedFilter.process(mRgba);

        // Draw rectangles around detected faces that are 'alive'
        for (Rect faceRect : mAliveFacesRects) {
            Core.rectangle(mRgba, faceRect.tl(), faceRect.br(), FACE_DRAW_COLOR, 3);
            drawText(faceRect);
        }

        // Draw a skull on-top of detected faces that are 'dead'
        for (Rect faceRect : mDeadFacesRects) {
            drawDeadFace(faceRect);
            drawText(faceRect);
        }
    }

    private void matToTexture(Mat mat) {


            int size = (int) mat.total() * mat.channels();
            byte[] buff = new byte[size];
            mat.get(0, 0, buff);


            // Create the texture
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,     // Type of texture
                    0,                 // Pyramid level (for mip-mapping) - 0 is the top level
                    GLES20.GL_RGB,            // Internal colour format to convert to
                    mat.cols(),          // Image width  i.e. 640 for Kinect in standard mode
                    mat.rows(),          // Image height i.e. 480 for Kinect in standard mode
                    0,                 // Border width in pixels (can either be 1 or 0)
                    GLES20.GL_RGB, // Input image format (i.e. GL_RGB, GL_RGBA, GL_BGR etc.)
                    GLES20.GL_UNSIGNED_BYTE,  // Image data type
                    ByteBuffer.wrap(buff));        // The actual image data itself


    }

    // TODO use constants
    private void drawText(Rect face) {

        // Default values for text drawing
        int font = Core.FONT_HERSHEY_PLAIN;
        Scalar white = new Scalar(255,255,255);
        double scale = 1.0;

        String matchText = new String("Match");

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

        if (leftPoint.x > 0)
            for (int i = 0; i < leftText.length; i++ ) {
                Core.putText(mRgba, leftText[i],leftPoint, font,scale,white);
                leftPoint = new Point(leftPoint.x,leftPoint.y+20);
            }


        if (rightPoint.x < mRgba.cols() - getTextSize(rightText[0]) )
            for (int i = 0; i < rightText.length; i++ ) {
                Core.putText(mRgba, rightText[i],rightPoint, font,scale,white);
                rightPoint = new Point(rightPoint.x,rightPoint.y+20);
            }

    }

    private int getTextSize(String text) {
        // Assuming 11 pixels per character
        return text.length()*11;
    }

    private void drawDeadFace(Rect faceRect) {

        // TODO remove unused code
        // resize the dead face Mat
        //Size size = new Size(faceRect.width, faceRect.height);
        //Mat resizedImgMat = new Mat(size, CvType.CV_8UC4);
        //Imgproc.resize(mDeadFaceImg, resizedImgMat, size);

        // draw the face
        //ProcessUtils.overlayImage(mRgba, mDeadFaceImg, mRgba, new Point(faceRect.x, faceRect.y));

        List<MatOfPoint> pts = new ArrayList<>();
        pts.add(new MatOfPoint( new Point(faceRect.x, faceRect.y),
                new Point(faceRect.x, faceRect.y + faceRect.height),
                new Point(faceRect.x + faceRect.width, faceRect.y + faceRect.height),
                new Point(faceRect.x + faceRect.width, faceRect.y)));

        Core.fillPoly(mRgba, pts, new Scalar(255,0,0,255)); //TODO color constant
    }
}
