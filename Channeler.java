package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static team372.MessageConstants.*;
import static team372.StateConstants.*;
import static team372.ErrorConstants.*;

public class Channeler extends AttackingRobot {

	private int drainRounds; // ile rund jeszcz nalezy wykonywac drain()

	public Channeler(RobotController _rc)
	{
		super(_rc);
	}

	public int run() throws GameActionException
	{
		isNearArchon = false;
        while (true) {
            endRound();
            receiveMessages();
            senseNearbyRobots();
            protectNearestArchon();
        }
	}

	@Override
	protected void endRound() throws GameActionException{
		if (drainRounds > 0) {
			--drainRounds;
		}
		super.endRound();
	}

    @Override
	protected void tryAttack() throws GameActionException
	{
        if (drainRounds > 0)
            rc.drain();
        else if (enemyRobotLocations != null && enemyRobotLocations.length > 0) {
            for (MapLocation loc : enemyRobotLocations) {
                if (myLoc.distanceSquaredTo(loc) <= 6) {
                    rc.drain();
                }
            }
        }
	}

	@Override
	protected boolean isCloseToArchon() {
		return false;
	}
	
	@Override
	protected void handleMessage(Message msg) throws GameActionException{
		switch (msg.ints[INDEX_CMD]) {
			case MSG_DRAIN:
				drainRounds = 5;
				break;
			case MSG_ENEMY_LOCATIONS:
				enemyRobotLocations = msg.locations;
				break;
			default:
				break;
		}
	}
}
