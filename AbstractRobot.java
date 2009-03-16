package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static team372.MessageConstants.*;
import static team372.StateConstants.*;
import static team372.ErrorConstants.*;
import java.util.*;

public abstract class AbstractRobot {

	protected final RobotController rc;
	protected final Rand rand;
	protected LinkedList<Message> messageQueue;
	protected boolean actionQueued;
	// actionQueued == true jesli zostala zakolejkowana akcja (lub jej "brak" -
	// z tym doNotMove() to i tak nie dziala)
	protected ArrayList<RobotInfo> nearbyPlayerRobots = null;
	protected ArrayList<RobotInfo> nearbyEnemyRobots = null;
	protected MapLocation myLoc = null;
	protected Direction myDir = null;
	protected MapLocation nearestArchon = null;
	protected MapLocation gotoDestination = null;
	protected boolean isNearArchon = false;
	protected BugNavigator navigator;

	public AbstractRobot(RobotController _rc) {
		rc = _rc;
		messageQueue = new LinkedList<Message>();
		rand = new Rand();
		navigator = new BugNavigator(rc);
	}

	abstract public int run() throws GameActionException;

	// helper methods

	protected void endRound() throws GameActionException {
		sendMessages();
		// clear caches first
		nearbyEnemyRobots = null;
		nearbyPlayerRobots = null;
		nearestArchon = null;
		actionQueued = false;
		rc.yield();
        myLoc = rc.getLocation();
		myDir = rc.getDirection();
	}

	protected void gotoLocation(MapLocation loc) throws GameActionException {
		if (rc.isMovementActive() || rc.hasActionSet()) {
			return;
		}

        if (myLoc.isAdjacentTo(loc) && isEnemyAtLocation(loc)) {
            rc.setDirection(myLoc.directionTo(loc));
        } else {
            navigator.moveTo(loc);
        }
	}

	protected boolean isEnergonLevelHigh() {
		return rc.getEnergonLevel() / rc.getMaxEnergonLevel() >= 0.7;
	}
	
	// message handling

	abstract protected void handleMessage(Message m) throws GameActionException;

	protected void receiveMessages() throws GameActionException {
		Message msg = null;
		for (int ii = 0; ii < 6 && (msg = getMessage()) != null; ++ii) {
			if (msg.ints[INDEX_CMD] == MSG_ZERO) {
				System.out.print("Got unknown message: [");
				for (int jj = 0; jj < msg.ints.length; jj++) {
					System.out.print(" " + new Integer(msg.ints[jj]).toString());
				}
				System.out.println(" ] " + msg.toString());
			} else
				handleMessage(msg);
		} // end for
	}

	final int messageHash(Message m) {
		// w sumie trudno nam zaszkodzic przez zmienianie intow, wiec
		// olejmy to, bo to duzo bytecodeow zzera
		/*for (int ii = INDEX_HASH + 1; ii < m.ints.length; ++ii) {
			hash += m.ints[ii];
			hash ^= (hash << 16) ^ (m.ints[ii] << 11);
			hash += hash >> 11;
		}*/
		// ale chcemy aby hash byl zalezny od rundy
		int hash = MESSAGE_HASH_START + m.ints[INDEX_ROUND] ^ (MESSAGE_HASH_START << 3);
		// chcemy sprawdzic lokacje
		if (m.locations != null) {
			for (int ii = 0; ii < m.locations.length; ++ii) {
				hash += m.locations[ii].getX() ^ (hash << 3);
				hash += m.locations[ii].getY() ^ (hash << 3);
			}
		}
		hash += MESSAGE_HASH_START ^ (hash << 3);
		return hash;
	}
	
	final Message getMessage() {
		Message m = null;
		while ((m = rc.getNextMessage()) != null) {
			if (m.ints != null && 
				m.ints.length >= MESSAGE_MIN_INTS &&
				m.ints[INDEX_TAG] == MESSAGE_TAG &&
				(m.ints[INDEX_WHO] == rc.getRobot().getID() ||
				m.ints[INDEX_WHO] == ID_BROADCAST) &&
				(m.ints[INDEX_ROUND] >> 3) + 30 >= Clock.getRoundNum() &&
				messageHash(m) == m.ints[INDEX_HASH]) {
				return m;
			} 
		}
		return null;
	}

	final void enqueueMessage(Message m) {
		messageQueue.addLast(m);
	}

	final void forwardMessage(Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessageTo(int who, Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_WHO] = who;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessageToAll(Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_WHO] = ID_BROADCAST;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessages() throws GameActionException {
		if (!rc.hasBroadcastMessage() && !messageQueue.isEmpty()) {
			rc.broadcast(messageQueue.poll());
		}

	}

	// Obsluga otoczenia

	final void senseNearbyRobots() throws GameActionException {
//        int a = Clock.getBytecodeNum();
		nearbyPlayerRobots = new ArrayList<RobotInfo>();
		nearbyEnemyRobots =	new ArrayList<RobotInfo>();
		RobotInfo ri;

		Robot[] robots = rc.senseNearbyAirRobots();
		for (Robot robot : robots) {
			ri = rc.senseRobotInfo(robot);
			if (ri.team == rc.getTeam()) {
				nearbyPlayerRobots.add(ri);
			} else {
				nearbyEnemyRobots.add(ri);
			}

		}
		robots = rc.senseNearbyGroundRobots();
		for (Robot robot : robots) {
			ri = rc.senseRobotInfo(robot);
			if (ri.team == rc.getTeam()) {
				nearbyPlayerRobots.add(ri);
			} else {
				nearbyEnemyRobots.add(ri);
			}

		}
//        System.out.print("senseNearbyRobots bytecodes: ");
//        System.out.println(Clock.getBytecodeNum() - a);
	}

 	protected MapLocation senseNearestArchonLocationFurtherThan(int d)
			throws GameActionException {
		MapLocation[] locs = rc.senseAlliedArchons();
		int minDist = Integer.MAX_VALUE;
        MapLocation nearest = null;
		for (MapLocation loc : locs) {
			int dist = loc.distanceSquaredTo(myLoc);
			if (dist < minDist && dist > d) {
				nearest = loc;
				minDist = dist;
			}
		}
        return nearest;
	}

    protected void senseNearestArchon() throws GameActionException {
		if (nearestArchon == null)
            nearestArchon = senseNearestArchonLocationFurtherThan(-1);
    }

    // Czy jestesmy tak blisko Archona, ze moze on nam przekazywac energon.
	protected boolean isArchonCloseEnough(MapLocation archonPos) {
		MapLocation pos2 = myLoc;
		return pos2.equals(archonPos) || pos2.isAdjacentTo(archonPos);
	}

	protected boolean isEnemyNearby() throws GameActionException {
		return !nearbyEnemyRobots.isEmpty();
	}

	protected boolean isEnemyAtLocation(MapLocation loc) throws GameActionException {
        RobotInfo ri;
        if (!rc.canSenseSquare(loc))
            return false;
        Robot r = rc.senseGroundRobotAtLocation(loc);
        if (r != null) {
            ri = rc.senseRobotInfo(r);
            if (ri.team != rc.getTeam()) {
                return true;
            }
        }
        r = rc.senseAirRobotAtLocation(loc);
        if (r != null) {
            ri = rc.senseRobotInfo(r);
            if (ri.team != rc.getTeam()) {
                return true;
            }
        }
        return false;
    }

	/**
	 * Oblicza kierunek ucieczki jako kierunek od srodka ciezkosci miedzy
	 * robotami przeciwnika do wlasnej pozycji.
	 * Cannony maja 2 razy wieksza wage niz pozostale roboty.
	 * Workery nie sa liczone. 
	 * Zwraca NONE jesli nie ma przeciwnikow w poblizu.
	 */
	Direction calculateEscapeDirection() {
		int x_sum = 0, y_sum = 0, num = 0;
		for (RobotInfo r : nearbyEnemyRobots) {
			if (r.type == RobotType.CANNON) {
				x_sum += 2 * r.location.getX();
				y_sum +=
						2 * r.location.getY();
				num +=
						2;
			} else if (r.type != RobotType.WORKER) {
				x_sum += r.location.getX();
				y_sum +=
						r.location.getY();
				num++;

			}


		}
		if (num == 0) {
			return Direction.NONE;
		}

		MapLocation massCentre = new MapLocation(x_sum / num, y_sum / num);
		return massCentre.directionTo(myLoc);
	}

	// obsluga celow

	void doExplore() throws GameActionException {
		//	if (Clock.getBytecodeNum() > bytecodeExplore) return;
		//	int a = Clock.getBytecodeNum();
		if (rc.isMovementActive()) {
			return;
		}
		rc.setIndicatorString(0, "Exploring");
		if (!rc.canMove(myDir) || Clock.getRoundNum() % 8 == 0) {
			if (rand.getBoolean()) {
				rc.setDirection(myDir.rotateLeft());
			} else {
				rc.setDirection(myDir.rotateRight());
			}
		} else {
			rc.moveForward();
		}
		actionQueued = true;
	//	if (Clock.getBytecodeNum() > a &&
	//			bytecodeExplore < a) bytecodeExplore = a;
	}

    	void doFeed() throws GameActionException {
//        int a = Clock.getBytecodeNum();
		double transferPerFriend =
				(rc.getEnergonLevel() - 10)
				/ (nearbyPlayerRobots.size() + 1);
		transferPerFriend = Math.max(transferPerFriend, 0);
        if (transferPerFriend == 0)
            return;
		for (RobotInfo ri : nearbyPlayerRobots) {
			if ((ri.location.isAdjacentTo(myLoc) ||
					ri.location.equals(myLoc)) &&
					ri.type != RobotType.ARCHON &&
					ri.energonReserve < 3) {
				rc.transferEnergon(Math.min(transferPerFriend,
					GameConstants.ENERGON_RESERVE_SIZE - ri.energonReserve),
					ri.location,
					ri.type.isAirborne()?
						RobotLevel.IN_AIR : RobotLevel.ON_GROUND);
			}
		}
//        System.out.print("doFeed bytecodes: ");
//        System.out.println(Clock.getBytecodeNum() - a);
	}

    void doEscape() throws GameActionException {
        if (rc.isMovementActive() || actionQueued)
            return;

        Direction escapeDir = calculateEscapeDirection();
        if (escapeDir == Direction.NONE)
            return;
        if (!rc.canMove(escapeDir))
            escapeDir = escapeDir.rotateLeft();
        if (!rc.canMove(escapeDir))
            escapeDir = escapeDir.rotateRight().rotateRight();
        if (!rc.canMove(escapeDir))
            return;

        rc.setIndicatorString(0, "Escaping " + escapeDir.toString());

        if (!actionQueued) {
            if (myDir != escapeDir)
                rc.setDirection(escapeDir);
            else
                rc.moveForward();
            actionQueued = true;
        }
    }
}
