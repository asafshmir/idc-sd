package com.idc.sd.t800;

import android.content.Context;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
    The face tracker is used for tracking all the faces and handling different actions regarding
    the faces. It uses a FaceDetector to detect faces in a given frame, and track each on of them
    using the FaceData class. On each frame, the FaceTracker tries to match the detected faces to
    the previous detected faces, and tries to match detected markers received from outside.
 */
public class FaceTracker {

    // maximal distance (relative to face size) for marker matching
    public static final double MAX_MARKER_DIST_FACTOR = 1.5;

    private FaceDetector mFaceDetector;
    private List<FaceData> mTrackedFaces;

    public FaceTracker(Context context) {
        mFaceDetector = new FaceDetector(context);
        mTrackedFaces = new ArrayList<>();
    }

    public void init() {
        mFaceDetector.init();
    }

    public void process(Mat gray, List<Point> markers) {

        // detect faces in the given Mat
        Rect[] faces = mFaceDetector.detectFaces(gray);
        Rect[] finalFaces = removeDuplicates(faces);

        // track the faces
        updateTrackedFaces(finalFaces);

        // try to match markers to faces
        matchMarkers(markers);
    }

    // remove rectangles that are too close, meaning that they are the same face
    private Rect[] removeDuplicates(Rect[] faces) {
        List<Rect> noDups = new ArrayList<>();
        for (Rect face : faces) {
            boolean foundDup = false;
            for (Rect finalFace : noDups) {
                double dist = ProcessUtils.pointDistance
                        (ProcessUtils.findCenter(face), ProcessUtils.findCenter(finalFace));
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

        // sort both new faces and already tracked faces (according to x axis), in order to improve matching
        Arrays.sort(faces, new Comparator<Rect>() {
            @Override
            public int compare(Rect lhs, Rect rhs) {
                return ((Integer) lhs.x).compareTo(rhs.x);
            }
        });

        Collections.sort(mTrackedFaces, new Comparator<FaceData>() {
            @Override
            public int compare(FaceData lhs, FaceData rhs) {
                return ((Integer) (lhs.getFaceRect().x)).compareTo(rhs.getFaceRect().x);
            }
        });

        // try to match each given face to a previous tracked face
        for (Rect faceRect : faces) {
            boolean isRectMatched = false;

            for (FaceData trackedFace : mTrackedFaces) {
                if (!trackedFace.isMatched()) {
                    isRectMatched = trackedFace.matchFace(faceRect);
                    break; // stop matching a tracked face to the given face
                }
            }

            // no matched face found - create a new tracked face
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

    // try to match each given marker to a face
    private void matchMarkers(List<Point> markers) {
        for (Point marker : markers) {
            for (FaceData trackedFace : mTrackedFaces) {
                trackedFace.matchMarker(marker, trackedFace.getFaceRect().height * MAX_MARKER_DIST_FACTOR);
            }
        }
    }

    // return the bounding rectangle of all valid faces which are also 'alive'
    private Rect[] getRectanglesOfInnocentFaces() {
        List<Rect> rects = new ArrayList<>();
        for (FaceData faceTrackingData : mTrackedFaces) {
            if (faceTrackingData.isValidFace() && faceTrackingData.isAlive()
                    && !faceTrackingData.isMarked()) {
                rects.add(faceTrackingData.getFaceRect());
            }
        }

        return rects.toArray(new Rect[rects.size()]);
    }

    // return the bounding rectangle of all valid faces which are also 'dead'
    private Rect[] getRectanglesOfDeadFaces() {
        List<Rect> rects = new ArrayList<>();
        for (FaceData faceTrackingData : mTrackedFaces) {
            if (faceTrackingData.isValidFace() && !faceTrackingData.isAlive()) {
                rects.add(faceTrackingData.getFaceRect());
            }
        }

        return rects.toArray(new Rect[rects.size()]);
    }

    // return the bounding rectangle of all valid faces which are also 'dead'
    private Rect[] getRectanglesOfTargetFaces() {
        List<Rect> rects = new ArrayList<>();
        for (FaceData faceTrackingData : mTrackedFaces) {
            if (faceTrackingData.isValidFace() && faceTrackingData.isAlive()
                    && faceTrackingData.isMarked()) {
                rects.add(faceTrackingData.getFaceRect());
            }
        }

        return rects.toArray(new Rect[rects.size()]);
    }

    // return a list with 3 items -
    // first is the rectangles representing the faces of the 'alive' faces,
    // second is the rectangles representing the faces of the 'dead' faces
    // third is the rectangles representing the faces of the target faces which are alive
    public ArrayList<Rect[]> getFaceRectangles() {
        ArrayList<Rect[]> allFaces = new ArrayList<>();
        allFaces.add(getRectanglesOfInnocentFaces());
        allFaces.add(getRectanglesOfDeadFaces());
        allFaces.add(getRectanglesOfTargetFaces());
        return allFaces;
    }

    // check if the touched zone is inside a face, and if so - kill it
    public void handleScreenTouch(Point touchedPoint) {
        for (FaceData faceData : mTrackedFaces) {
            if (faceData.getFaceRect().contains(touchedPoint) && faceData.isValidFace()
                 && faceData.isMarked()) {
                faceData.kill();
                return;
            }
        }
    }
}