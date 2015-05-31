package com.idc.sd.t800;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class FaceTracker {

    // maximal distance (relative to screen width) for tracking
    public static final double      MAX_TRACKING_DIST_FACTOR = 0.25;
    // maximal distance (relative to face size) for marker matching
    public static final double      MAX_MARKER_DIST_FACTOR = 2;

    private FaceDetector    mFaceDetector;
    private List<FaceData>  mTrackedFaces;
    private double          mMaxDist;

    public FaceTracker(Context context) {
        mFaceDetector = new FaceDetector(context);
        mTrackedFaces = new ArrayList<>();
        mMaxDist = -1;
    }

    public void init() {
        mFaceDetector.init();
    }

    public void process(Mat gray, List<MatOfPoint> markers) {

        // calc the maximum distance for two consecutive points to be considered the same one.
        // the distance is calculated relatively to the image size
        if (mMaxDist == -1) {
            mMaxDist = MAX_TRACKING_DIST_FACTOR * (gray.cols());
        }

        // detect faces in the given Mat
        Rect[] faces = mFaceDetector.detectFaces(gray);
        Rect[] finalFaces = removeDuplicates(faces);

        // track the faces
        updateTrackedFaces(finalFaces);

        // try to match markers to faces
        matchMarkers(markers);
    }

    // remove rectangles that overlap
    private Rect[] removeDuplicates(Rect[] faces) {
        List<Rect> noDups = new ArrayList<>();
        for (Rect face : faces) {
            boolean foundDup = false;
            for (Rect finalFace : noDups) {
                double dist = pointDistance(findCenter(face), findCenter(finalFace));
                if (dist < (finalFace.width / 2)) {
                    foundDup = true;
                }
            }
            if (!foundDup) {
                noDups.add(face);
            }
        }
        return noDups.toArray(new Rect[noDups.size()]);
    }

    // get an array list of rectangles found by the FaceDetector, and update the tracking data.
    private void updateTrackedFaces(Rect[] faces) {

        // init all TrackingData
        for (FaceData trackedFace : mTrackedFaces) {
            trackedFace.setUnmatched();
        }

        // try to match each given face to a previous tracked face
        for (Rect faceRect : faces) {
            boolean isRectMatched = false;

            for (FaceData trackedFace : mTrackedFaces) {
                if (!trackedFace.isMatched()) {
                    isRectMatched = trackedFace.matchFace(faceRect, mMaxDist);
                }
            }

            // no matched polygon found - create a new tracked polygon
            if (!isRectMatched) {
                mTrackedFaces.add(new FaceData(faceRect));
            }
        }

        // remove faces that doesn't have high enough score to be tracked
        List<FaceData> toBeRemoved = new ArrayList<>();
        for (FaceData trackedFace : mTrackedFaces) {
            trackedFace.handleEndOfFrame();
            if (trackedFace.hasDisappeared()) {
                toBeRemoved.add(trackedFace);
            }
        }
        mTrackedFaces.removeAll(toBeRemoved);
    }

    private void matchMarkers(List<MatOfPoint> markers) {

        // try to match each given marker to a face
        for (MatOfPoint marker : markers) {

            for (FaceData trackedFace : mTrackedFaces) {
                trackedFace.matchMarker(findCentroid(marker),
                        trackedFace.getFaceRect().height * MAX_MARKER_DIST_FACTOR);
            }
        }
    }

    // return the bounding rectangle of all valid polygons
    public Rect[] getBoundingRectangles() {
        // return only the polygons with high enough score
        List<Rect> rects = new ArrayList<>();
        for (FaceData faceTrackingData : mTrackedFaces) {
            if (faceTrackingData.isValidFace()) {
                rects.add(faceTrackingData.getFaceRect());
            }
        }

        return rects.toArray(new Rect[rects.size()]);
    }

    // return the center of all valid polygons
    public List<Point> getValidTrackedCenters() {
        List<Point> centers = new ArrayList<>();
        for (FaceData trackingData : mTrackedFaces) {
            if (trackingData.isValidFace()) {
                centers.add(trackingData.getFaceCenter());
            }
        }
        return centers;
    }

    // TODO move all functions to utils
    // calc euclidean distance between two given points
    public static double pointDistance(Point p, Point q) {
        double xDiff = p.x - q.x;
        double yDiff = p.y - q.y;
        return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
    }

    // calc the center of a given rectangle
    public static Point findCenter(Rect rect) {
        Point center = new Point();
        center.x = rect.x + (rect.width / 2);
        center.y = rect.y + (rect.height / 2);
        return center;
    }

    // calc the centroid of a given polygon
    private Point findCentroid(MatOfPoint polygon) {
        Moments moments = Imgproc.moments(polygon);
        Point centroid = new Point();
        centroid.x = moments.get_m10() / moments.get_m00();
        centroid.y = moments.get_m01() / moments.get_m00();
        return centroid;
    }
}