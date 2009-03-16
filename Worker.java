package team372;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;
import static battlecode.common.GameConstants.*;
import static team372.MessageConstants.*;
import static team372.StateConstants.*;

public class Worker extends AbstractRobot {

    HashSet<MapLocation> blockLocations = new HashSet<MapLocation>();
    MapLocation[] towerLocations = new MapLocation[6];
    int currTowerNr;
    MapLocation towerLocation;
    MapLocation fluxLocation;
    MapLocation moveLoc;
    double tower_r;
	int state;

    public Worker(RobotController _rc) {
        super(_rc);
        state = STATE_IDLE;
        for (int i = 0; i < towerLocations.length; i++)
            towerLocations[i] = null;
    }

    @Override
    protected void handleMessage(Message msg) throws GameActionException {
		switch (msg.ints[INDEX_CMD]) {
			case MSG_BUILD_TOWER:
				if (state != STATE_IDLE) {
					break;
				}
				if (msg.locations == null || msg.locations.length < 1) {
					break;
				}
				fluxLocation = towerLocation = msg.locations[0];
				towerLocations[0] = msg.locations[0];
				currTowerNr = 0;
				tower_r = 0;
				moveLoc = null;
				if (state == STATE_IDLE) {
					state = TASK_GET_BLOCK;
				}
				if (msg.locations.length < 2) {
					break;
				}
				for (int j = 1; j < msg.locations.length; j++) {
					blockLocations.add(msg.locations[j]);
				}
				break;
			default:
				break;
		}
    }

    public int run() throws GameActionException {
        state = STATE_IDLE;
        while (true) {
            endRound();
            senseNearbyRobots();
            receiveMessages();
            if (Clock.getRoundNum() % 32 == 0)
                senseNearbyBlocks();
            //dotad ok 1300 bytecodu zuzyte
            switch (state) {
                case TASK_GET_BLOCK:
                    doGetBlock();
                    break;
                case TASK_PUT_BLOCK:
                    doPutBlock();
                    break;
                case TASK_EXPLORE:
                    doExplore();
                    break;
                case STATE_IDLE:
                    rc.setIndicatorString(0, rc.getRobotType().toString() + ": Idle");
                    break;
                default:
                    break;
            }
        }
    }
  
    private MapLocation findNearestBlockLocation() {
        int minDist = Integer.MAX_VALUE;
        MapLocation nearest = null;
        for (MapLocation loc : blockLocations) {
            if (loc == null || loc.equals(myLoc))
                continue;
            int dist = myLoc.distanceSquaredTo(loc);
            if (dist < minDist) {
                nearest = loc;
                minDist = dist;
            }
        }
        return nearest;
    }

    private boolean senseNearbyBlocks() {
        MapLocation[] nearbyBlocks = rc.senseNearbyBlocks();
        if (nearbyBlocks.length > 0) {
            for (MapLocation loc : nearbyBlocks) {
                if (fluxLocation.distanceSquaredTo(loc) > tower_r)
                    blockLocations.add(loc);
            }
            blockLocations.remove(fluxLocation);
            return true;
        }
        else
            return false;
    }

    private void doGetBlock() throws GameActionException {
        rc.setIndicatorString(1, "FluxLoc: " + fluxLocation.toString());
        rc.setIndicatorString(0, rc.getRobotType().toString() + ": Getting Block");
        if (rc.isMovementActive())
            return;
        MapLocation blockLoc = findNearestBlockLocation();
        //nie zna zadnej lokalizacji bloku, wiec lazi i szuka
        if (blockLoc == null) {
            rc.setIndicatorString(2, "BlockLoc: NULL");
            state = TASK_EXPLORE;
            doExplore();
            return;
        }
        else if (blockLoc.isAdjacentTo(myLoc)) {
            if (!rc.canLoadBlockFromLocation(blockLoc)) {
                rc.setIndicatorString(2, "wrong BlockLoc: " + blockLoc.toString());
                blockLocations.remove(blockLoc);
            }
            else {
                rc.setIndicatorString(2, "BlockLoc: " + blockLoc.toString());
                rc.loadBlockFromLocation(blockLoc);
                blockLocations.remove(blockLoc);
                state = TASK_PUT_BLOCK;
            }
        }
        else {
            rc.setIndicatorString(2, "BlockLoc: " + blockLoc.toString());
            navigator.moveTo(blockLoc);
        }
    }

    private class FindResult {
        public MapLocation loc;
        public boolean foundNew;

        public FindResult(MapLocation loc, boolean foundNew) {
            this.loc = loc;
            this.foundNew = foundNew;
        }
        
    }

    /**
     * Znajduje najlepsze miejsce do polozenia klocka. Jesli miejsce jest nowe
     * to zwraca true. Jesli na planszy jest miejsce z ktorego mozna postawic
     * klocek na dotychczasowe miejsce to zwraca false
     */
    FindResult findNewTowerPlace () throws GameActionException {
        Direction dir = Direction.EAST;
        int minDiff = Integer.MAX_VALUE;
        MapLocation best = null;
        if (currTowerNr < towerLocations.length - 1 &&
                towerLocations[currTowerNr + 1] != null)
            best = towerLocations[currTowerNr + 1];
        else do {
            MapLocation tmp = towerLocation.add(dir);
            dir = dir.rotateRight();
            if (rc.senseGroundRobotAtLocation(tmp) != null)
                continue;
            int diff = rc.senseHeightOfLocation(towerLocation) - rc.senseHeightOfLocation(tmp);
            if (diff < 0)
                continue;
            else if (diff < minDiff) {
                minDiff = diff;
                best = tmp;
            }
        } while (!dir.equals(Direction.EAST));
        if (minDiff < WORKER_MAX_HEIGHT_DELTA)
            return new FindResult(best, false);
        else {
            if (currTowerNr < towerLocations.length)
                towerLocations[currTowerNr + 1] = best;
            return new FindResult(best, true);
        }
    }

    private void doPutBlock() throws GameActionException {
        //TODO: poprawic na sensowniejsze
        rc.setIndicatorString(1, "FluxLoc: " + fluxLocation.toString());
        rc.setIndicatorString(2, "TowLoc: " + towerLocation.toString());
        rc.setIndicatorString(0, rc.getRobotType().toString() + ": Putting Block");
        if (rc.isMovementActive())
            return;
//        if (towerLocation.equals(myLoc) && rc.canMove(myDir.opposite()))
//        {
//            rc.moveBackward();
//            return;
//        }

        if (!rc.canUnloadBlockToLocation(towerLocation)) {
            if (moveLoc != null && !myLoc.equals(moveLoc)) {
                if (myLoc.isAdjacentTo(moveLoc) &&
                        (rc.senseHeightOfLocation(myLoc)
                        - rc.senseHeightOfLocation(moveLoc)
                        < WORKER_MAX_HEIGHT_DELTA)) {
                    towerLocation = moveLoc;
                    moveLoc = null;
                }
                else {
                    rc.setIndicatorString(0, rc.getRobotType().toString() + ": Moving to "+moveLoc.toString());
                    navigator.moveTo(moveLoc);
                }
            }
            else if (!myLoc.isAdjacentTo(towerLocation) &&
                    !myLoc.equals(towerLocation))
                navigator.moveTo(towerLocation);
            else {
                FindResult res = findNewTowerPlace();
                if (res.foundNew) {
                    towerLocation = res.loc;
                    currTowerNr++;
                    tower_r = Math.max (currTowerNr * currTowerNr * 2,
                            tower_r);
                    moveLoc = null;
                    navigator.moveTo(towerLocation);
                }
                else {
                    moveLoc = res.loc;
                    navigator.moveTo(moveLoc);
                }
            }
        }
        else {
            rc.unloadBlockToLocation(towerLocation);
            moveLoc = null;
            towerLocation = fluxLocation;
            currTowerNr = 0;
            state = TASK_GET_BLOCK;
        }
    }

    @Override
    void doExplore() throws GameActionException {
        //sprawdza czy nie ma w poblizu blokow, jak sa to kontynuuje
        //pobieranie bloku
        rc.setIndicatorString(0, rc.getRobotType().toString() + ": Exploring");
        senseNearbyBlocks();
        if (!blockLocations.isEmpty())
            state = TASK_GET_BLOCK;
        else
            super.doExplore();
    }
}
