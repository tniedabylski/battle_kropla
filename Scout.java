package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import static team372.MessageConstants.*;
import static team372.StateConstants.*;
import static team372.ErrorConstants.*;

public class Scout extends AbstractRobot {
	boolean explorationActive;
	int goal;
	Message message;

	public Scout(RobotController _rc) {
		super(_rc);
	}

	public int run() throws GameActionException {
		explorationActive = false;
		while (true) {
			endRound();
			receiveMessages();
			selectGoals();
			if (goal == TASK_DELIVER_MESSAGE) {
				Messenger messenger = new Messenger(rc);
				messenger.setMessage(message);
				messenger.setTargetArchon(message.ints[3]);
				if (messenger.run() == ERROR_SUCCESS) {
					rc.setIndicatorString(2, "Messenger mission succeded");
				} else {
					rc.setIndicatorString(2, "Messenger mission failed");
				}
				explorationActive = false;
			} else if (goal == TASK_EXPLORE) {
				rc.setIndicatorString(0, "SCOUT: Explore");
				doExplore();
			} else {
				rc.setIndicatorString(0, "SCOUT: Idle");
			}
		}
	}

	void selectGoals() throws GameActionException {
		if (message != null) {
			goal = TASK_DELIVER_MESSAGE;
		} else if (explorationActive) {
			goal = TASK_EXPLORE;
		} else
			goal = TASK_NONE;
	}
	
	@Override
	protected void endRound() throws GameActionException {
		message = null;
		super.endRound();
	}

	@Override
	protected void handleMessage(Message msg) throws GameActionException {
		switch (msg.ints[INDEX_CMD]) {
			case MSG_EXPLORE:
				explorationActive = true;
				break;
			case MSG_HELP: // fall through
			case MSG_LETTER:
				if (!rc.getRobotType().equals(RobotType.SCOUT)) {
					break;
				}
				rc.setIndicatorString(0, "SCOUT: " + (msg.ints[INDEX_CMD] == MSG_HELP ? "HELP" : "Messenger"));
				message = msg;
				break;
			default:
				break;
		}
	}

}
