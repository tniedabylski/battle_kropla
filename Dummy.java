package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

public class Dummy extends AbstractRobot {

	public Dummy(RobotController _rc) {
		super(_rc);
	}

	public int run() throws GameActionException{
		while(true) {
			/*** beginning of main loop ***/

			while(rc.isMovementActive()) {
				rc.setIndicatorString(0,"Moving");
				rc.yield();
			}
			rc.setIndicatorString(0,"Not moving");

			if(rc.canMove(rc.getDirection())) {
				rc.moveForward();
			}
			else {
				rc.setDirection(rc.getDirection().rotateRight());
				rc.setIndicatorString(1,rc.getDirection().toString());
			}
			rc.yield();

			/*** end of main loop ***/
		}
	}

	protected void handleMessage(Message msg) throws GameActionException {
		// empty
	}
}
