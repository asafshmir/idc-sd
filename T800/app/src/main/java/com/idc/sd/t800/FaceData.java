package com.idc.sd.t800;

import org.opencv.core.Point;
import org.opencv.core.Rect;

/*
    Holds relevant data for tracking and handling a single face.
    When a face is detected, we start tracking it by matching faces in the following frames.
    On each frame, we try to match each already detected face to the new discovered face, by
    measuring the distance between the centers.
    A face is considered to be 'valid' if it has appeared in enough frames (we use a scoring system
    to determine what is enough).
    A face is considered to be 'marked' if one of the detected markers is close enough to it.
    A face is considered to be 'alive' if it is marked (i.e bad guy) and the player killed it.
*/
class FaceData {

    // tracking constants
    public static final int INITIAL_SCORE = 0; // Initial score for a new point
    public static final int SINGLE_STEP_SCORE = 1; // Addition to the score if a point is matched
    public static final int VALID_MAX_SCORE = 15; // Max score
    public static final int VALID_MIN_SCORE = 5; // Minimum score to report the point
    public static final int MIN_TRACK_SCORE = 0; // Minimum score to continue tracking the point

    // marker matching constants
    public static final int PROCESS_FRAMES = 100; // Amount of initial frames to look for markers
    public static final int INITIAL_MARKER_SCORE = 0; // Initial matching score
    public static final int MIN_MARKER_MATCH_SCORE = 5; // Minimum score for marker matching

    // minimal overlap percentage between two consecutive frames
    public static final double      MIN_OVERLAP_FACTOR = 0.15;

    private Rect            mFaceRect;
    private Point           mFaceCenter;
    private Integer         mTrackingScore;
    private boolean         mMatched;
    private int             mMarkerScore;
    private boolean         mMarked;
    private boolean         mIsAlive;
    private int             mFrames;

    public FaceData(Rect face) {
        this.mFaceRect = face;
        this.mFaceCenter = ProcessUtils.findCenter(mFaceRect);
        this.mTrackingScore = INITIAL_SCORE;
        this.mMatched = true;
        this.mMarkerScore = INITIAL_MARKER_SCORE;
        this.mMarked = false;
        this.mIsAlive = true;
        this.mFrames = 0;
    }

    public void setUnmatched() { this.mMatched = false; }
    // determines if the face detected in current frame matches a face in previous frames
    public boolean isMatched() { return mMatched; }
    // determines if the person represented by the FaceData is dead or alive
    public boolean isAlive() { return mIsAlive; }
    public void kill() { mIsAlive = false; }
    // determines if the face is close enough to a marker
    public boolean isMarked() { return mMarked; }
    public Rect getFaceRect() { return mFaceRect; }

    // check if the new face matches the current face, meaning their centers are close enough
    public boolean matchFace(Rect newFace) {

        // calculate the overlapping percentage of the current rectangle with the given rectangle
        Rect interRect = ProcessUtils.intersection(mFaceRect, newFace);
        if ((interRect != null) && (interRect.area() > mFaceRect.area() * MIN_OVERLAP_FACTOR)) {
            mMatched = true;
            mFaceRect = newFace;
            mFaceCenter = ProcessUtils.findCenter(newFace);
            Integer newScore = mTrackingScore + SINGLE_STEP_SCORE;
            mTrackingScore = (newScore > VALID_MAX_SCORE) ? VALID_MAX_SCORE : newScore;
            return true;
        }
        return false;
    }

    // check if a marker matches the face (i.e. in the proximity of the face center)
    public void matchMarker(Point markerCenter, double maxDist) {

        // don't check for already marked (forever) faces or for "old" faces
        if(mMarked || (mFrames > PROCESS_FRAMES)) { return; }

        if (ProcessUtils.pointDistance(mFaceCenter, markerCenter) < maxDist) {
            // matched
            mMarkerScore++;
            if(mMarkerScore > MIN_MARKER_MATCH_SCORE) {
                mMarked = true;
            }
        }
    }

    // check values at end of frame
    public void handleEndOfFrame() {
        // new frame
        mFrames++;

        // reduce score of unmatched face
        if (!mMatched) {
            mTrackingScore -= SINGLE_STEP_SCORE;
        }
    }

    // check if the face should be continued to be tracked, or it's not there anymore
    public boolean hasDisappeared() {
        return (mTrackingScore < MIN_TRACK_SCORE);
    }

    // check if the face has appeared "enough times" in lase previous frames, and should be reported
    public boolean isValidFace() {
        return (mTrackingScore >= VALID_MIN_SCORE);
    }
}
