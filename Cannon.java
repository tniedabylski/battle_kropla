package team372;

import battlecode.common.*;
import static team372.StateConstants.*;
import static team372.MessageConstants.*;

/**
 * Armata.
 * @author lukasz
 */
public class Cannon extends AttackingRobot{

	public Cannon(RobotController _rc)
	{
		super(_rc);
	}

    public int run() throws GameActionException{
        isNearArchon = false;
        while (true) {
            endRound();
            receiveMessages();
            senseNearbyRobots();
            protectNearestArchon();
        }
    }

    @Override
    protected boolean isEnemyNearby() {
        return enemyRobotLocations != null;
    }

    @Override
    protected void tryAttack() throws GameActionException
	{
        if (enemyRobotLocations != null) {
            boolean attacked = false;
            MapLocation best = null;
            int dist = Integer.MAX_VALUE;
            for (MapLocation loc : enemyRobotLocations) {
                if (rc.canAttackSquare(loc)) {
                    rc.attackGround(loc);
                    attacked = true;
                    break;
                } else {
                    int dist1 = myLoc.distanceSquaredTo(loc);
                    if (dist1 > 1 && dist1 < dist) {
                        best = loc;
                        dist = dist1;
                    }
                }
            }
            if (!attacked && !rc.isMovementActive()) {
                if (best != null) {
                    gotoLocation(best);
                } else {
                    rc.setDirection(myDir.rotateRight());
                }
            }
        }
	}

	@Override
    protected boolean isCloseToArchon() {
        return nearestArchon != null &&
                myLoc.distanceSquaredTo(nearestArchon) < 8;
    }

	@Override
	protected void handleMessage(Message msg) throws GameActionException {
		switch (msg.ints[INDEX_CMD]) {
			case MSG_ENEMY_LOCATIONS:
				enemyRobotLocations = msg.locations;
				break;
			default:
				break;
		}
	}
}
