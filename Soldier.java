package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static team372.MessageConstants.*;
import static team372.StateConstants.*;

/**
 *
 * @author lukasz
 */
public class Soldier extends AttackingRobot {
	private int goal;
    private int groupId = -1;
    private MapLocation leaderPos;
	boolean gotOffensiveMessage;
	boolean gotProtectArchonMessage;
	boolean gotGoAttackMessage;

	public Soldier(RobotController _rc)
	{
		super(_rc);
        goal = TASK_PROTECT_ARCHON;
	}

	public int run() throws GameActionException
	{
        isNearArchon = false;
		while (true) {
			endRound();
			receiveMessages();
			senseNearbyRobots();
			getBestEnemy();
            doFeed();
            getLeaderPos();
			selectGoals();
			switch (goal) {
				case TASK_PROTECT_ARCHON:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Protect Archon");
					protectNearestArchon();
					break;
				case TASK_GO_ATTACK:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Moving (GO_ATTACK)");
					rc.setIndicatorString(1, myLoc.toString() + " -> " + gotoDestination.toString());
					gotoLocation(gotoDestination);
					break;
				case TASK_OFFENSIVE:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Offensive");
					offensive();
					break;
                case TASK_FOLLOW_LEADER:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Follow");
					doFollowLeader();
					break;
				default:
					doExplore();
					break;
			}
		}
	}

	void selectGoals() throws GameActionException {
		if (gotoDestination != null &&
				(myLoc.equals(gotoDestination) ||
					myLoc.isAdjacentTo(gotoDestination))) {
			gotoDestination = null;
		}
		if (bestEnemy != null) {
			if (canAttack(bestEnemy)) {
				goal = TASK_OFFENSIVE;
			} else if (gotProtectArchonMessage) {
				goal = TASK_PROTECT_ARCHON;
			} else {
				goal = TASK_GO_ATTACK;
				gotoDestination = bestEnemy.location;
			}
		} else if (gotProtectArchonMessage) {
			goal = TASK_PROTECT_ARCHON;
        } else if (groupId != -1) {
            goal = TASK_FOLLOW_LEADER;
		} else if (gotoDestination != null) {
			goal = TASK_GO_ATTACK;
		} else
			goal = TASK_OFFENSIVE;

        rc.setIndicatorString(2, "Goal " + goal);
	}

	@Override
	protected void endRound() throws GameActionException {
		gotGoAttackMessage = false;
		gotOffensiveMessage = false;
		gotProtectArchonMessage = false;
		super.endRound();
	}

    @Override
    protected void tryAttack() throws GameActionException
	{
        RobotInfo enemy = bestEnemy;
        if (enemy != null)
            attack(enemy);
	}

    @Override
    protected boolean isCloseToArchon() {
        return nearestArchon != null &&
                myLoc.distanceSquaredTo(nearestArchon) < 8;
    }

	@Override
	protected void handleMessage(Message msg) throws GameActionException {
		switch (msg.ints[INDEX_CMD]) {
			case MSG_START_OFFENSIVE:
				gotOffensiveMessage = true;
				break;
			case MSG_PROTECT_ARCHON:
				isNearArchon = false;
				gotProtectArchonMessage = true;
				break;
			case MSG_GO_ATTACK:
//                 if (msg.ints.length <= MESSAGE_MIN_INTS ||
//                         groupId != msg.ints[MESSAGE_MIN_INTS])
//                     break;
				gotGoAttackMessage = true;
				gotoDestination = msg.locations[0];
				break;
			case MSG_ENEMY_SPOTTED:
				lastEnemyLocation = msg.locations[0];
				break;
			case MSG_ENEMY_LOCATIONS:
				enemyRobotLocations = msg.locations;
				break;
            case MSG_JOIN_GROUP:
                if (groupId != - 1)
                    break;
                groupId = msg.ints[MESSAGE_MIN_INTS];
                rc.setIndicatorString(1, "Group " + groupId);
                leaderPos = msg.locations[0];
                break;
            case MSG_HERE_I_AM:
                if (groupId == msg.ints[MESSAGE_MIN_INTS]) {
                    leaderPos = msg.locations[0];
                }
                break;
			default:
				break;
		}
	}

    protected void getLeaderPos() throws GameActionException {
        if (groupId == -1)
            return;

        if (Clock.getRoundNum() % 16 == 0)
            leaderPos = null;

        for (Robot r: rc.senseNearbyAirRobots()) {
            if (r.getID() == groupId) {
                leaderPos = rc.senseRobotInfo(r).location;
                return;
            }
        }
    }

    protected void doFollowLeader() throws GameActionException{
        if (leaderPos == null)
            offensive();

        gotoLocation(leaderPos);
    }
}
