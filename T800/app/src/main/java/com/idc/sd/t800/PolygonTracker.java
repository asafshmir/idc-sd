package com.idc.sd.t800;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;

public class PolygonTracker {

    private static final String     TAG                 = "T800::PolygonTracker";

    public static final double DIST_THRESHOLD = 150;
    public static final int MAX_FRAME_HISTORY = 80;


    private PolygonDetector         mPolyDetector;
    private List<List<Point>>      mPolyCenters;



    public PolygonTracker(Scalar color, int numVertices) {
        mPolyDetector = new PolygonDetector(color, numVertices);
        mPolyCenters = new ArrayList<>();
    }

    public void init() {
        mPolyDetector.init();
    }


    // save the centers of the polygons in the last 10 frames history.
    // If none is found, take the last found center and add NULL to the frames history

    // to handle multiple polygons - save multiple history data,
    // match a given polygon center to the closest last saved centers

    public List<Point> process(Mat rgbaImage) {

        List<MatOfPoint> polygons = mPolyDetector.detectPolygons(rgbaImage);
        List<Point> centers = new ArrayList<>();

        // calculate center for each polygon
        for (int i = 0; i < polygons.size(); i++) {
            centers.add(findCentroid(polygons.get(i)));
        }

        // update the cache of centers
        updateCenters(centers);

        return centers;
    }

    private void updateCenters(List<Point> centers) {

        boolean[] taken = new boolean[mPolyCenters.size()];
        System.out.println("zzz A:" + taken.length);

        for (Point center : centers) {

            boolean found = false;

            for (int i = 0; i < mPolyCenters.size(); i++) {
                System.out.println("zzz B:" + taken.length);
                if (!taken[i]) {

                    List<Point> curPolyHistory = mPolyCenters.get(i);
                    Point curCenter;
                    if (curPolyHistory != null) {

                        curCenter = null;
                        int j = 0;
                        while (curCenter == null && curPolyHistory.size() > j) {
                            curCenter = curPolyHistory.get(j);
                            j++;
                        }

                        if (curCenter != null) {
                            double curDist = pointDistance(curCenter, center);
                            System.out.println("zzz DIST:" + curDist);
                            if ((curDist < DIST_THRESHOLD)) {
                                curPolyHistory.add(0,center);
                                // remove oldest if size too long
                                if (curPolyHistory.size() > MAX_FRAME_HISTORY) {
                                    curPolyHistory.remove(curPolyHistory.size() - 1);
                                }
                                taken[i] = true;
                                found = true;
                                break;

                            }
                        } else {
                            // shouldn't get here
                            Log.w(TAG, "Shouldn't get here");
                        }
                    }
                }

            }

            // no existing queue is matching the center
            if (!found) {
                // create new queue for center
                List<Point> queue = new ArrayList<>();
                queue.add(center);
                mPolyCenters.add(queue);
            }

        }

        // go over non-taken queues and add null (meaning no matching polygon was found on current frame)
        List<List<Point>> toBeRemoved = new ArrayList<>();
        for (int i = 0; i < taken.length; i++) {
            if (!taken[i]) {
                List<Point> queue = mPolyCenters.get(i);
                queue.add(null);
                // remove oldest if size too long
                if (queue.size() > MAX_FRAME_HISTORY) {
                    queue.remove(0);
                }
                // remove queue with all null values
                boolean allNull = true;
                for (Point point : queue) {
                    if (point != null) {
                        allNull = false;
                        break;
                    }
                }
                if (allNull) {
                    toBeRemoved.add(queue);
                }
            }
        }
        mPolyCenters.removeAll(toBeRemoved);
    }

    private Point findCentroid(MatOfPoint polygon) {
        Moments moments = Imgproc.moments(polygon);
        Point centroid = new Point();
        centroid.x = moments.get_m10() / moments.get_m00();
        centroid.y = moments.get_m01() / moments.get_m00();
        return centroid;
    }

    static double pointDistance(Point p, Point q) {
        double xDiff = p.x - q.x;
        double yDiff = p.y - q.y;
        return Math.sqrt(xDiff*xDiff + yDiff*yDiff);
    }

}
