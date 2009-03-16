package team372;

import battlecode.common.*;
import java.util.ArrayList;

/**
 * Nawigacja robakowa. Jak spotka przeszkode, to skreca w lewo lub prawo,
 * az uda sie ja ominac lub za dlugo juz omijanie trwa.
 * @author Kropla
 */
public class BugNavigator {

    RobotController rc;
    boolean tracing;
    int movesTraced;
    boolean tracingRight;
    ArrayList<BugPair> traceDirs;
    MapLocation traceStart;
    static int MAX_TRACE_LENGTH = 15;
    static int MAX_SAME_LOC_DIST_SQUARED = 16;

    protected class BugPair {

        public MapLocation loc;
        public boolean tracedRight;

        public BugPair(MapLocation l, boolean trace) {
            loc = l;
            tracedRight = trace;
        }
    }

    public BugNavigator(RobotController r) {
        rc = r;
        resetBug();
    }

    public void resetBug() {
        tracing = false;
        movesTraced = 0;
        tracingRight = true;
        traceDirs = new ArrayList<BugPair>();
    }

    /**
     * moveTo(target) wykonuje ruch (lub potrzebny obrot) w kierunku celu,
     * omijajac ew.przeszkody. 
     * Nie sprawdza czy moze wykonac ruch (isMovementActive()). Przed wywolaniem
     * trzeba zadbac, aby mozna bylo wykonac akcje.
     * Funkcja w testach zajmuje 100-300 bytocode'u.
     */
    public void moveTo(MapLocation target) throws GameActionException {
//        System.out.println("BUGSTART:" + Clock.getBytecodeNum());
        Direction dirToTarget = rc.getLocation().directionTo(target);
        if (movesTraced > MAX_TRACE_LENGTH) {
            tracing = false;
        }
        MapLocation myLoc = rc.getLocation();
        if (tracing) {
            movesTraced++;
            Direction dir = rc.getDirection();
            while (rc.canMove(dir)) {
                if (dir == dirToTarget /*&& myLoc.distanceSquaredTo(target) <
                        traceStart.distanceSquaredTo(target)*/) {
                    tracing = false;
                    move(dir);
//                    System.out.println("BUGEND:" + Clock.getBytecodeNum());
                    return;
                }
                dir = turnBack(dir);
            }

            while (!rc.canMove(dir)) {
                dir = turnAway(dir);
            }
            move(dir);
        } else if (rc.canMove(dirToTarget)) {
            move(dirToTarget);
        } else {
            if (myLoc.isAdjacentTo(target)) {
                if (!rc.getDirection().equals(dirToTarget)) {
                    rc.setDirection(dirToTarget);
                }
//                System.out.println("BUGEND:" + Clock.getBytecodeNum());
                return;
            }
            tracing = true;
            movesTraced = 0;
            traceStart = myLoc;
            boolean found = false;
            for (BugPair p : traceDirs) {
                if ((p.loc.distanceSquaredTo(myLoc) < MAX_SAME_LOC_DIST_SQUARED) &&
                        (p.loc.distanceSquaredTo(target)) < myLoc.distanceSquaredTo(target)) {
                    p.loc = myLoc;
                    p.tracedRight = !p.tracedRight;
                    tracingRight = p.tracedRight;
                    found = true;
                    break;
                }
            }
            if (!found) {
                traceDirs.add(new BugPair(myLoc, tracingRight));
            }

            Direction dir = rc.getDirection();
            while (!rc.canMove(dir)) {
                dir = turnAway(dir);
            }
            move(dir);
        }
//        System.out.println("BUGEND:" + Clock.getBytecodeNum());
    }

    private void move(Direction d) throws GameActionException {
        if (!rc.getDirection().equals(d)) {
            rc.setDirection(d);
            return;
        } else {
            rc.moveForward();
        }

    }

    private Direction turnAway(Direction d) {
        return (tracingRight ? d.rotateRight() : d.rotateLeft());
    }

    private Direction turnBack(Direction d) {
        return (tracingRight ? d.rotateLeft() : d.rotateRight());
    }
}
