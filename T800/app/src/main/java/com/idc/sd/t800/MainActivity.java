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
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class MainActivity extends Activity implements CvCameraViewListener2 {
    private static final String TAG                 = "T800::MainActivity";
    private static final Scalar FACE_RECT_COLOR     = new Scalar(255, 255, 255, 255);
    private final int               CONTOUR_INTERVAL = 10;

    private CameraBridgeViewBase    mOpenCvCameraView;
    private Mat                     mRgba;
    private Mat                     mGray;
    private Mat                     mRedVisionMat;
    private Mat                     mHierarchy;
    private Mat                     mIntermediateMat;
    private Mat                     mHsv;
    private Mat                     mHsv2;
    private Scalar                  CONTOUR_COLOR;

    private int                     mContourCounter;
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
        mGray = new Mat(height, width, CvType.CV_8UC4);
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mHierarchy = new Mat();
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mContourCounter = 0;
        CONTOUR_COLOR = new Scalar(255,0,0,255);
        mHsv = new Mat(height, width, CvType.CV_8UC3);
        mHsv2 = new Mat(height, width, CvType.CV_8UC3);

        initRedVision();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
        mHierarchy.release();
        mIntermediateMat.release();
        mHsv.release();
        mHsv2.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        /* Detect faces */
        //Rect[] facesArray = mFaceDetector.detectFaces(mRgba, mGray);

        /* Apply red vision */
        //applyRedVision(mRgba);

        /* Color faces rect */
//        for (int i = 0; i < facesArray.length; i++)
//            Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(), FACE_RECT_COLOR, 3);

        //drawText();


        findFiducial();


        return mRgba;
    }


    private void drawText() {
        Core.putText(mRgba,"Test Text",new Point(100,100), 3,1,new Scalar(255,255,255,255));
    }

    private void findFiducial() {
        if (mContourCounter == CONTOUR_INTERVAL) {
            mContourCounter = 0;
            findCircle();
            //findSquare();
            return;
            /*
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            mHierarchy = new Mat();
            Imgproc.Canny(mRgba, mIntermediateMat, 80, 100);
//        Imgproc.findContours(mIntermediateMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE, new Point(0, 0));

            Imgproc.findContours(mIntermediateMat, contours, mHierarchy, Imgproc.RETR_EXTERNAL,
                    Imgproc.CHAIN_APPROX_SIMPLE);
            findSquare(contours);
            mHierarchy.release();
            //Imgproc.drawContours(mRgba, contours, -1, new Scalar(Math.random()*255, Math.random()*255, Math.random()*255));//, 2, 8, hierarchy, 0, new Point());



*/
        } else {
            mContourCounter++;
        }

    }

    private void findCircle() {

        Imgproc.cvtColor(mRgba, mHsv, Imgproc.COLOR_BGRA2GRAY);

            /*Core.inRange(mHsv, new Scalar(0, 86, 72), new Scalar(39, 255, 255), mHsv); // red
            Core.inRange(mHsv, new Scalar(150, 125, 100), new Scalar(180,255,255), mHsv2); // red
            Core.bitwise_or(mHsv, mHsv2, mHsv);*/

        /// Reduce the noise so we avoid false circle detection
        Imgproc.GaussianBlur(mHsv, mHsv, new Size(9, 9), 2);
        double dp = 1.2; double minDist = 100; int minRadius = 10; int maxRadius = 100;
        double param1 = 70; double param2 = 72;
        Imgproc.HoughCircles(mHsv, mIntermediateMat, Imgproc.CV_HOUGH_GRADIENT, dp, minDist, param1, param2,
                minRadius, maxRadius);


/* get the number of circles detected */
        int numberOfCircles = (mIntermediateMat.rows() == 0) ? 0 : mIntermediateMat.cols();

              /* draw the circles found on the image */
        for (int i=0; i<numberOfCircles; i++) {


              /* get the circle details, circleCoordinates[0, 1, 2] = (x,y,r)
               * (x,y) are the coordinates of the circle's center*/
            double[] circleCoordinates = mIntermediateMat.get(0, 0);


            int x = (int) circleCoordinates[0], y = (int) circleCoordinates[1];

            Point center = new Point(x, y);

            int radius = (int) circleCoordinates[2];

/* circle's outline */
            Core.circle(mRgba, center, radius, new Scalar(0,
                    255, 0), 4);

/* circle's center outline */
            Core.rectangle(mRgba, new Point(x - 5, y - 5),
                    new Point(x + 5, y + 5),
                    new Scalar(0, 128, 255), -1);
        }



    }

    //private void findSquare(List<MatOfPoint> contours) {
    private void findSquare() {

        Vector<Point> squares;
        Mat pyr,timing ,gry =new Mat();
        pyr=new Mat(mRgba.size(),CvType.CV_8U);
        timing=new Mat(mRgba.size(),CvType.CV_8U);
        int thresh = 50, N = 11;
        List<Mat> grayO=new ArrayList<Mat>();
        List<Mat> timing1=new ArrayList<Mat>();
        Imgproc.pyrDown(mRgba, pyr,new Size(mRgba.cols()/2.0, mRgba.rows()/2));
        Imgproc.pyrUp(pyr, timing,mRgba.size());
//      Vector<Point> contours=new Vector<Point>();
        timing1.add(pyr);
        grayO.add(timing);
//      grayO.add(0,timing);
        for(int c=0;c<3;c++) {
            int ch[] = {1, 0};

            MatOfInt fromto = new MatOfInt(ch);
            Core.mixChannels(timing1, grayO, fromto); // Getting Exception here
//          Core.mixChannels(src, dst, fromTo)
            for (int i = 0; i < N; i++) {
                Mat output = grayO.get(0);
                if (i == 0) {

                    Imgproc.Canny(output, gry, 5, thresh);
                    Imgproc.dilate(gry, gry, new Mat(), new Point(-1, -1), 1);
                } else {
//                    Imgproc.threshold(output, gry, (l+1) * 255 / N, 255, Imgproc.THRESH_BINARY);
                }
//              sourceImage=gry;
                ArrayList <MatOfPoint>contours = new ArrayList<MatOfPoint>();
                Imgproc.findContours(gry, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                MatOfPoint2f approxCurve = new MatOfPoint2f();

                for (int j = 0; j < contours.size(); j++) {
                    MatOfPoint tempContour = contours.get(j);
                    MatOfPoint2f newMat = new MatOfPoint2f(tempContour.toArray());
                    int contourSize = (int) tempContour.total();

                    Imgproc.approxPolyDP(newMat, approxCurve, contourSize * 0.02, true);
                    MatOfPoint points = new MatOfPoint(approxCurve.toArray());
//                    if( approx.size() == 4 && fabs(contourArea(cv::Mat(approx))) > 1000 && cv::isContourConvex(cv::Mat(approx))) {
                    if (points.toArray().length == 4 && Imgproc.isContourConvex(points)) {// && (Math.abs(approxCurve.total())>1000) && Imgproc.isContourConvex(points)){
                        double maxCosine = 0;
                        int k;
                        for (k = 2; k < 5; k++) {
                            double cosine = Math.abs(angle(points.toArray()[k % 4], points.toArray()[k - 2], points.toArray()[k - 1]));
                            if (maxCosine > cosine) {
                                maxCosine = cosine;
                            }
                        }

                        if (maxCosine < 0.3) {
                            Core.rectangle(mRgba, points.toArray()[0], points.toArray()[points.toArray().length-1], FACE_RECT_COLOR, 3);
//                    Core.putText(mRgba,"Found",points.toArray()[0], 3,1,new Scalar(255,255,255,255));


                        }

                    }

                }
            }
        }
    }



    // helper function:
    // finds a cosine of angle between vectors
    // from pt0->pt1 and from pt0->pt2
    private double angle( Point pt1, Point pt2, Point pt0 )
    {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;
        return (dx1*dx2 + dy1*dy2)/Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
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
