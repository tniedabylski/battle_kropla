package team372;

import battlecode.common.*;
import java.util.ArrayList;
import static battlecode.common.GameConstants.*;

import static team372.MessageConstants.*;
import static team372.StateConstants.*;
import static team372.ErrorConstants.*;

public class ScoutLeader extends AttackingRobot {

    Message message;
    int targetArchon;
    int goal;
    boolean gotProtectArchonMessage = false;
    ArrayList<Robot> group = new ArrayList<Robot>();

    public ScoutLeader(RobotController _rc) {
        super(_rc);
    }

    @Override
    public int run() throws GameActionException {
        while (true) {
            endRound();
            receiveMessages();
            senseNearbyRobots();
            getBestEnemy();
            selectGoals();
            if (Clock.getRoundNum() % 8 == 0) {
                Message m = new Message();
                m.ints = new int[MESSAGE_MIN_INTS + 1];
                m.ints[INDEX_CMD] = MSG_HERE_I_AM;
                m.ints[MESSAGE_MIN_INTS] = rc.getRobot().getID();
                m.locations = new MapLocation[1];
                m.locations[0] = myLoc;
                sendMessageTo(ID_BROADCAST, m);
            }

//            recruit();

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

//        if (group.size() < 2) {
//            goal = TASK_PROTECT_ARCHON;
//            return;
//        }

        if (bestEnemy != null) {
            if (canAttack(bestEnemy)) {
                goal = TASK_OFFENSIVE;
            }
        }
        else if (gotProtectArchonMessage) {
            goal = TASK_PROTECT_ARCHON;
        }
        else if (gotoDestination != null) {
            goal = TASK_GO_ATTACK;
        }
        else
            goal = TASK_OFFENSIVE;

        rc.setIndicatorString(2, "Goal " + goal);
    }

    @Override
    protected void handleMessage(Message msg) throws GameActionException {
        switch (msg.ints[INDEX_CMD]) {
            case MSG_PROTECT_ARCHON:
                isNearArchon = false;
                gotProtectArchonMessage = true;
                break;
            case MSG_GO_ATTACK:
                gotoDestination = msg.locations[0];
                break;
            case MSG_ENEMY_SPOTTED:
                lastEnemyLocation = msg.locations[0];
                break;
            case MSG_ENEMY_LOCATIONS:
                enemyRobotLocations = msg.locations;
                break;
            default:
                break;
        }
    }

    @Override
    protected void tryAttack() throws GameActionException {
    }

    @Override
    protected boolean isCloseToArchon() {
        return false;
    }

    protected void recruit() throws GameActionException {
        for (Robot r : rc.senseNearbyGroundRobots()) {
            RobotInfo info = rc.senseRobotInfo(r);
            Team team = rc.getTeam();
            if (!group.contains(r) && info.team == team && info.type == RobotType.SOLDIER) {
                Message m = new Message();
                m.ints = new int[MESSAGE_MIN_INTS + 1];
                m.ints[INDEX_CMD] = MSG_JOIN_GROUP;
                m.ints[MESSAGE_MIN_INTS] = rc.getRobot().getID();
                m.locations = new MapLocation[1];
                m.locations[0] = myLoc;
                sendMessageTo(ID_BROADCAST, m);
                group.add(r);
            }
        }
        return;
    }

    @Override
    protected void offensive() throws GameActionException {
        sendGoAttackMessage(bestEnemy.location);
        super.offensive();
    }



    private void sendGoAttackMessage(MapLocation loc) throws GameActionException {
        Message m = new Message();
        m.ints = new int[MESSAGE_MIN_INTS + 1];
        m.ints[INDEX_CMD] = MSG_GO_ATTACK;
        m.ints[MESSAGE_MIN_INTS] = rc.getRobot().getID();
        m.locations = new MapLocation[1];
        m.locations[0] = loc;
        sendMessageToAll(m);
    }
}
