package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

// TODO remove class

public class PolygonTracker {

    public static final double          MAX_DIST_FACTOR = 0.15;

    private PolygonDetector             mPolyDetector;
    private List<PolygonTrackingData>   mTrackingData;
    private double                      mMaxDist;

    public PolygonTracker() {
        mPolyDetector = new PolygonDetector();
        mTrackingData = new ArrayList<>();
        mMaxDist = -1;
    }

    public void init() {
        mPolyDetector.init();
    }

    public void process(Mat rgbaImage) {

        // calc the maximum distance for two consecutive points to be considered the same one.
        // the distance is calculated relatively to the image size
        if (mMaxDist == -1) {
            mMaxDist = MAX_DIST_FACTOR * (rgbaImage.cols());
        }

        // detect polygons in the given Mat
        List<MatOfPoint> polygons = mPolyDetector.detectPolygons(rgbaImage);

        // track the polygons
        updateTrackedPolygons(polygons);
    }

    // return the bounding rectangle of all valid polygons
    public Rect[] getBoundingRectangles() {

        // return only the polygons with high enough score
        List<Rect> rects = new ArrayList<>();
        for (PolygonTrackingData trackingData : mTrackingData) {
            if (trackingData.isValidPolygon()) {
                // Get bounding rectangle of contour
                Rect rect = Imgproc.boundingRect(trackingData.mTrackedPolygon);
                rects.add(rect);
            }
        }

        return rects.toArray(new Rect[rects.size()]);
    }

    // return the center of all valid polygons
    public List<Point> getValidTrackedCenters() {
        List<Point> centers = new ArrayList<>();
        for (PolygonTrackingData trackingData : mTrackingData) {
            if (trackingData.isValidPolygon()) {
                centers.add(trackingData.mTrackedPolygonCenter);
            }
        }
        return centers;
    }

    // get a list of polygons found by the PolygonDetector, and update the tracking data.
    private void updateTrackedPolygons(List<MatOfPoint> polygons) {

        // init all TrackingData
        for (PolygonTrackingData polygonTrackingData : mTrackingData) {
            polygonTrackingData.mMatched = false;
        }

        // try to match each given polygon to a previous tracked polygon
        for (MatOfPoint polygon : polygons) {
            boolean isMatched = false;

         for (PolygonTrackingData polygonTrackingData : mTrackingData) {
                if (!polygonTrackingData.mMatched) {
                    isMatched = polygonTrackingData.matchPolygon(polygon);
                }
            }

            // no matched polygon found - create a new tracked polygon
            if (!isMatched) {
                mTrackingData.add(new PolygonTrackingData(polygon));
            }
        }

        // remove polygons that doesn't have high enough score to be tracked
        List<PolygonTrackingData> toBeRemoved = new ArrayList<>();
        for (PolygonTrackingData polygonTrackingData : mTrackingData) {
            polygonTrackingData.handleIfUnmatched();
            if (!polygonTrackingData.isTrackedPolygon()) {
                toBeRemoved.add(polygonTrackingData);
            }
        }
        mTrackingData.removeAll(toBeRemoved);
    }

    // calc the centroid of a given polygon
    private Point findCentroid(MatOfPoint polygon) {
        Moments moments = Imgproc.moments(polygon);
        Point centroid = new Point();
        centroid.x = moments.get_m10() / moments.get_m00();
        centroid.y = moments.get_m01() / moments.get_m00();
        return centroid;
    }

    // calc euclidean distance between two given points
    public static double pointDistance(Point p, Point q) {
        double xDiff = p.x - q.x;
        double yDiff = p.y - q.y;
        return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
    }

    /*
        Holds relevant data for tracking a single polygon over several frames.
        Each polygon has a current score according to its appearance or lack of appearance in the
        previous frames. A polygon is considered to be valid, if it has high enough score.
     */
    protected class PolygonTrackingData {

        public static final int INITIAL_SCORE = 0; // Initial score for a new point
        public static final int SINGLE_STEP_SCORE = 1; // Addition to the score if a point is matched
        public static final int VALID_MAX_SCORE = 15; // Max score
        public static final int VALID_MIN_SCORE = 5; // Minimum score to report the point
        public static final int MIN_TRACK_SCORE = 0; // Minimum score to continue tracking the point

        protected MatOfPoint    mTrackedPolygon;
        protected Point         mTrackedPolygonCenter;
        private Integer         mTrackingScore;
        protected boolean       mMatched = true; // Is the point matched in current frame to a previous tracked point

        public PolygonTrackingData(MatOfPoint polygon) {
            this.mTrackedPolygon = polygon;
            this.mTrackedPolygonCenter = findCentroid(mTrackedPolygon);
            this.mTrackingScore = INITIAL_SCORE;
        }

        // check if the new polygon matches the current polygon, meaning their centers are close enough
        public boolean matchPolygon(MatOfPoint newPolygon) {

            Point newPolygonCenter = findCentroid(newPolygon);

            if (pointDistance(mTrackedPolygonCenter, newPolygonCenter) < mMaxDist) {
                mMatched = true;
                mTrackedPolygon = newPolygon;
                mTrackedPolygonCenter = newPolygonCenter;
                Integer newScore = mTrackingScore + SINGLE_STEP_SCORE;
                mTrackingScore = (newScore > VALID_MAX_SCORE) ? VALID_MAX_SCORE : newScore;
                return true;
            }
            return false;
        }

        // if a polygon is not matched in current frame, decrease score
        public void handleIfUnmatched() {
            if (!mMatched) {
                mTrackingScore -= SINGLE_STEP_SCORE;
            }
        }

        // check if the polygon should be continued to be tracked, or it's not there anymore
        public boolean isTrackedPolygon() {
            return (mTrackingScore >= MIN_TRACK_SCORE);
        }

        // check if the polygon has appeared "enough times" in lase previous frames, and should be reported
        public boolean isValidPolygon() {
            return (mTrackingScore >= VALID_MIN_SCORE);
        }
    }
}