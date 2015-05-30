package com.idc.sd.t800;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class PolygonTracker {

    public static final double      MAX_DIST_FACTOR = 0.25;

    private PolygonDetector             mPolyDetector;
    private List<PointTrackingData>     mTrackingData;
    private double                      mMaxDist;

    public PolygonTracker() {
        mPolyDetector = new PolygonDetector();
        mTrackingData = new ArrayList<>();
        mMaxDist = -1;
    }

    public void init() {
        mPolyDetector.init();
    }

    public List<Point> process(Mat rgbaImage) {

        // calc the maximum distance for two consecutive points to be considered the same one.
        // the distance is calculated relatively to the image size
        if (mMaxDist == -1) {
            mMaxDist = MAX_DIST_FACTOR * (rgbaImage.cols());
        }

        // detect polygons in the given Mat
        List<MatOfPoint> polygons = mPolyDetector.detectPolygons(rgbaImage);

        // calculate center for each polygon
        List<Point> centers = new ArrayList<>();
        for (int i = 0; i < polygons.size(); i++) {
            centers.add(findCentroid(polygons.get(i)));
        }

        // track the centers of the found polygons
        return updateTrackedPoints(centers);
    }

    // get a list of points found by the PolygonDetector, and update the tracking data.
    // return a list of point that
    private List<Point> updateTrackedPoints(List<Point> points) {

        // init all TrackingData
        for (PointTrackingData pointTrackingData : mTrackingData) {
            pointTrackingData.mMatched = false;
        }

        // try to match each given point to a previous tracked point
        for (Point point : points) {
            boolean isMatched = false;

            for (PointTrackingData pointTrackingData : mTrackingData) {
                if (!pointTrackingData.mMatched) {
                    isMatched = pointTrackingData.matchPoint(point);
                }
            }

            // no matched point found - create a new tracked point
            if (!isMatched) {
                mTrackingData.add(new PointTrackingData(point));
            }
        }

        // remove points that doesn't have high enough score to be tracked
        List<PointTrackingData> toBeRemoved = new ArrayList<>();
        for (PointTrackingData pointTrackingData : mTrackingData) {
            pointTrackingData.handleIfUnmatched();
            if (!pointTrackingData.isTrackedPoint()) {
                toBeRemoved.add(pointTrackingData);
            }
        }
        mTrackingData.removeAll(toBeRemoved);

        // return only the points with high enough score
        List<Point> trackedPoints = new ArrayList<>();
        for (PointTrackingData pointTrackingData : mTrackingData) {
            if (pointTrackingData.isValidPoint()) {
                trackedPoints.add(pointTrackingData.mTrackedPoint);
            }
        }
        return trackedPoints;
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
        Holds relevant data for tracking a single point over several frames.
        Each point has a current score according to its appearance or lack of appearance in the
        previous frames. A point is considered to be valid and reported if it has high enough score.
     */
    protected class PointTrackingData {

        public static final int INITIAL_SCORE = 0; // Initial score for a new point
        public static final int SINGLE_STEP_SCORE = 1; // Addition to the score if a point is matched
        public static final int VALID_MAX_SCORE = 15; // Max score
        public static final int VALID_MIN_SCORE = 5; // Minimum score to report the point
        public static final int MIN_TRACK_SCORE = 0; // Minimum score to continue tracking the point

        protected Point     mTrackedPoint;
        private Integer     mTrackedPointScore;
        protected boolean   mMatched = true; // Is the point matched in current frame to a previous tracked point

        public PointTrackingData(Point point) {
            this.mTrackedPoint = point;
            this.mTrackedPointScore = INITIAL_SCORE;
        }

        // check if the new point matches the current point, meaning they are close enough
        public boolean matchPoint(Point newPoint) {
            if (pointDistance(mTrackedPoint, newPoint) < mMaxDist) {
                mMatched = true;
                mTrackedPoint = newPoint;
                Integer newScore = mTrackedPointScore + SINGLE_STEP_SCORE;
                mTrackedPointScore = (newScore > VALID_MAX_SCORE) ? VALID_MAX_SCORE : newScore;
                return true;
            }
            return false;
        }

        // if a point is not matched in current frame, decrease score
        public void handleIfUnmatched() {
            if (!mMatched) {
                mTrackedPointScore -= SINGLE_STEP_SCORE;
            }
        }

        // check if the point should be continued to be tracked, or it's not there anymore
        public boolean isTrackedPoint() {
            return (mTrackedPointScore >= MIN_TRACK_SCORE);
        }

        // check if the point has appeared "enough times" in lase previous frames, and should be reported
        public boolean isValidPoint() {
            return (mTrackedPointScore >= VALID_MIN_SCORE);
        }
    }
}