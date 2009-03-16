package team372;

import battlecode.common.*;
import static team372.MessageConstants.*;
import static team372.StateConstants.*;
import java.util.*;

public class Archon extends AbstractRobot {
	
	int warGoal; // rozroznia czy atakujemy, bronimi sie, etc.
	MapLocation helpWanted; // jezeli null to nikt nie potrzebuje pomocy
	ArrayList<MapLocation> fluxSourceTaken; 
		// zrodla zarezerwowane przez jakiegos Archona
	ArrayList<MapLocation> knownFluxSource; // znane zrodla fluxu
	RobotInfo channelerRobotInfo;
    int unhandledMessagesRounds;
    boolean isMining;
    int searchGoal; // czy szukamy fluxa, czy tez sie rozpraszamy i eksplorujemy
    int exploreRounds; // ile rund juz explorowalismy
    boolean gotFluxTakenMessage;
    boolean existsCannon;

    //pola do obslugi workerow
    boolean createdWorker;
    boolean createdWorkerLastRound = false;

    boolean createdSoldier = false;

    boolean createdScout = false;

	private static final int RANDOM_WALK_TIME = 10;
	
	/////////////////////////

	private static final double MIN_ENERGON_RESERVE = 25.0;
    private static final double ENERGON_ESCAPE_LEVEL = 15.0;

	public Archon(RobotController _rc) {
		super(_rc);
		channelerRobotInfo = null;
		fluxSourceTaken = new ArrayList<MapLocation>();
		knownFluxSource = new ArrayList<MapLocation>();
		
		warGoal = TASK_ATTACK;
        searchGoal = TASK_SEARCH_FOR_FLUX;

        unhandledMessagesRounds = 0;
        isMining = false;
	}
	
	public int run() throws GameActionException {
		do {
            sendMessages();
            endRound();

            myLoc = rc.getLocation();
			myDir = rc.getDirection();
            if (unhandledMessagesRounds > 8) {
                receiveMessages();
                continue;
            }
			senseNearbyRobots();
            getBestEnemy();
			doFeed();

            if (Clock.getBytecodeNum() > 3000) {
                unhandledMessagesRounds++;
            } else {
				unhandledMessagesRounds = 0;
				receiveMessages();
			}

            selectGoals();

            if (Clock.getRoundNum() % 256 == 0)
                createdScout = false;

//            if (Clock.getRoundNum() % 512 == 0 || !createdScout) {
//                createdScout = spawnScout();
////                if (createdScout)
//                    continue;
//            }

//            if (Clock.getRoundNum() % 128 == 0 || !createdSoldier) {
//                createdSoldier = spawnSoldier();
////                if (createdSoldier)
//                    continue;
//            }

            if (warGoal == TASK_DEFENCE || warGoal == TASK_ATTACK) {
                doCannonCreation();
                if (actionQueued || Clock.getBytecodeNum() > 5200)
                    continue;
            }
			
			switch(warGoal) {
				case TASK_DEFENCE:
					doDefense();		
					break;
				case TASK_ATTACK:
					doAttack();
					break;
				case TASK_RESCUE:
					doRescue();
					break;
				case TASK_ESCAPE:
					doEscape();
					break;
                case TASK_NONE:
                    break;
				default:
					assert false;
			}
            if (actionQueued || Clock.getBytecodeNum() > 5500)
                continue;
			
			doCaptureAndMine();
            if (actionQueued || Clock.getBytecodeNum() > 5500)
                continue;

            isMining = false;
            if (searchGoal == TASK_SEARCH_FOR_FLUX) {
                exploreRounds = 0;
    			doSearchForFlux();
            } else {
                exploreRounds++;
                doExplore();
            }
		} while(true);
	}
	
    // message handling

	@Override
	protected void handleMessage(Message m) throws GameActionException {
		switch (m.ints[INDEX_CMD]) {
			case MSG_HELP:
				helpWanted = m.locations[0];
				break;
			case MSG_FLUX_TAKEN:
				fluxSourceTaken.add(m.locations[0]);
				gotFluxTakenMessage = true;
				break;
			case MSG_FLUX_FOUND:
				knownFluxSource.add(m.locations[0]);
				break;
			default:
				break;
		} // end switch
	}
    
    private void sendGoAttackMessage(MapLocation loc) throws GameActionException {
		Message m = new Message();
		m.ints = new int[MESSAGE_MIN_INTS];
		m.ints[INDEX_CMD] = MSG_GO_ATTACK;
		m.locations = new MapLocation[1];
		m.locations[0] = loc;
		sendMessageToAll(m);
	}

    private void sendDrainMessage() {
		Message m = new Message();
		m.ints = new int[MESSAGE_MIN_INTS];
		m.ints[INDEX_CMD] = MSG_DRAIN;
		sendMessageToAll(m);
    }

    private void sendEnemyLocations() {
        Message m = new Message();
        m.ints = new int[MESSAGE_MIN_INTS];
        m.ints[INDEX_CMD] = MSG_ENEMY_LOCATIONS;
//        m.locations = new MapLocation[nearbyEnemyRobots.size()];
//        int i = 0;
//        for (RobotInfo ri : nearbyEnemyRobots) {
//            m.locations[i++] = ri.location;
//        }
        m.locations = new MapLocation[1];
        m.locations[0] = bestEnemy.location;

        sendMessageToAll(m);
    }

    // overriden

    @Override
    protected void endRound() throws GameActionException {
        gotFluxTakenMessage = false;
        if (Clock.getRoundNum() % 32 == 0) {
            channelerRobotInfo = null;
        }
        super.endRound();
    }

    // helper methods

    private void selectGoals() throws GameActionException {
        if (isEnemyNearby()) {
            if (rc.getEnergonLevel() < ENERGON_ESCAPE_LEVEL)
                warGoal = TASK_ESCAPE;
            else
                warGoal = TASK_ATTACK;
        } else if (isMining)
            warGoal = TASK_DEFENCE;
        else
            warGoal = TASK_NONE;
        if (exploreRounds > RANDOM_WALK_TIME)
            searchGoal = TASK_SEARCH_FOR_FLUX;
        if (gotFluxTakenMessage) {
            exploreRounds = 0;
            searchGoal = TASK_EXPLORE;
        }
    }

	// TODO: ta metoda nie jest OK
//    private Robot spawnScout() throws GameActionException {
//		while (rc.senseAirRobotAtLocation(
//				myLoc.add(myDir)) != null ||
//				!rc.senseTerrainTile(myLoc.add(myDir)).
//				isTraversableAtHeight(RobotLevel.IN_AIR)) {
//			while (rc.isMovementActive()) {
//				rc.yield();
//			}
//			rc.setDirection(myDir.rotateRight());
//			rc.yield();
//		}
//
//		//poczekaj az bedzie wystarczajaco duzo energii
//		while (rc.getEnergonLevel() < RobotType.SCOUT.spawnCost() + 5) {
//			rc.yield();
//		}
//
//		rc.spawn(RobotType.SCOUT);
//		rc.yield();
//		return rc.senseAirRobotAtLocation(myLoc.add(myDir));
//	}

//	private void cryForHelp() throws GameActionException {
//		rc.setIndicatorString(2, "HELP!");
//		Robot messenger = spawnScout();
//		Message m = new Message();
//		m.ints = new int[MESSAGE_MIN_INTS];
//		m.locations = new MapLocation[1];
//		m.locations[0] = myLoc;
//		m.ints[INDEX_CMD] = MSG_HELP;
//		sendMessageTo(messenger.getID(), m);
//	}

    private boolean canSpawnRobot(RobotLevel level, double energonNeeded)
            throws GameActionException {
        boolean notOccupied;
        if (level == RobotLevel.IN_AIR)
            notOccupied = rc.senseAirRobotAtLocation(myLoc.add(myDir)) != null;
        else
            notOccupied = rc.senseGroundRobotAtLocation(myLoc.add(myDir)) != null;
        
		if (notOccupied ||
			!rc.senseTerrainTile(myLoc.add(myDir)).
            isTraversableAtHeight(level)) {
            if (!rc.isMovementActive()) {
                rc.setDirection(myDir.rotateRight());
                actionQueued = true;
            }
			return false;
		}
		if (rc.getEnergonLevel() < energonNeeded) {
			return false;
		}
        return true;
    }

    // Zwraca true jesli udalo sie spawn'owac. Jesli sie nie udalo trzeba
    // wywolac ponownie w kolejnej rundzie.
	private boolean spawnSoldier() throws GameActionException {
        if (!canSpawnRobot(RobotLevel.ON_GROUND, 15 + RobotType.SOLDIER.spawnCost()))
            return false;

		rc.spawn(RobotType.SOLDIER);
        actionQueued = true;
        return true;
	}

    private boolean spawnChanneler() throws GameActionException{
        if (channelerRobotInfo != null) return false;
        if (!canSpawnRobot(RobotLevel.ON_GROUND, 30 + RobotType.CHANNELER.spawnCost()))
            return false;

		rc.spawn(RobotType.CHANNELER);
        actionQueued = true;
		Robot r = rc.senseGroundRobotAtLocation(myLoc.add(myDir));
        if (r != null) {
            channelerRobotInfo = rc.senseRobotInfo(r);
        }

        return true;
    }

    private boolean spawnCannon() throws GameActionException {
        if (!canSpawnRobot(RobotLevel.ON_GROUND, 10 + RobotType.CANNON.spawnCost()))
            return false;

        rc.spawn(RobotType.CANNON);
        actionQueued = true;
        existsCannon = true;
        return true;
    }

    private boolean spawnWorker() throws GameActionException {
        if (!canSpawnRobot(RobotLevel.ON_GROUND, 5 + RobotType.WORKER.spawnCost()))
            return false;

        rc.spawn(RobotType.WORKER);
        actionQueued = true;
        return true;
    }
    
    private boolean spawnScout() throws GameActionException {
        if (!canSpawnRobot(RobotLevel.IN_AIR, 5 + RobotType.SCOUT.spawnCost()))
            return false;

        rc.spawn(RobotType.SCOUT);
        actionQueued = true;
        return true;
    }


    // doXXX() methods

	void doFeed() throws GameActionException {
//        int a = Clock.getBytecodeNum();
		double transferPerFriend = 
				(rc.getEnergonLevel() - MIN_ENERGON_RESERVE) 
				/ (nearbyPlayerRobots.size() + 1);
		transferPerFriend = Math.max(transferPerFriend, 0);
		for (RobotInfo ri : nearbyPlayerRobots) {
			if ((ri.location.isAdjacentTo(myLoc) ||
					ri.location.equals(myLoc)) &&
					ri.type != RobotType.ARCHON &&
					ri.energonReserve < GameConstants.ENERGON_RESERVE_SIZE) {
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
    void doCannonCreation() throws GameActionException {
        if (nearbyPlayerRobots.size() < 5 || Clock.getRoundNum() % 32 == 0) {
            existsCannon = false;
            for (RobotInfo ri : nearbyPlayerRobots) {
                if (ri.type == RobotType.CANNON) {
                    existsCannon = true;
                    break;
                }
            }
        }
        if (!existsCannon) {
            spawnCannon();
        }
    }
    void doWorkerCreation() throws GameActionException {
        if (createdWorkerLastRound) {
            MapLocation[] blocks = rc.senseNearbyBlocks();
            Message m = new Message();
            m.ints = new int[MESSAGE_MIN_INTS];
            m.locations = new MapLocation[/*blocks.length + */1];
            m.locations[0] = myLoc;
//            for (int i = 0; i < blocks.length; i++)
//                m.locations[i + 1] = blocks[i];
            m.ints[INDEX_CMD] = MSG_BUILD_TOWER;
            sendMessageToAll(m);
            createdWorkerLastRound = false;
        }
        if (Clock.getRoundNum() % 256 == 0 || !createdWorker) {
            createdWorkerLastRound = createdWorker = spawnWorker();
        }
    }
	void doDefense() throws GameActionException {
        if (nearbyPlayerRobots.size() < 5 || Clock.getRoundNum() % 32 == 0) {
            channelerRobotInfo = null;
            for (RobotInfo ri : nearbyPlayerRobots) {
                if (ri.type == RobotType.CHANNELER) {
                    channelerRobotInfo = ri;
                    break;
                }
            }
        }
        if (!actionQueued && channelerRobotInfo == null) {
            spawnChanneler();
        }
        if (isEnemyNearby()) {
            if (channelerRobotInfo != null && !existsCannon) {
                sendDrainMessage();
            } else if (existsCannon) {
                sendEnemyLocations();
            }
        }
    }
	void doAttack() throws GameActionException {
		if (isEnemyNearby())
		{
//            if (!createdScout) {
//                createdScout = spawnScout();
//            }
            if (Clock.getRoundNum() % 128 == 0 && spawnScout())
                sendGoAttackMessage(bestEnemy.location);
            else if (spawnSoldier())
                sendGoAttackMessage(bestEnemy.location);
            else if (existsCannon)
                sendEnemyLocations();
            else
                sendDrainMessage();
            actionQueued = true;
		}
	}
	void doRescue() throws GameActionException {}
	
	void doCaptureAndMine() throws GameActionException {
		FluxDeposit fluxDeposit = 
				rc.senseFluxDepositAtLocation(myLoc);
		if (fluxDeposit == null) return;
		FluxDepositInfo info = rc.senseFluxDepositInfo(fluxDeposit);
		if (info.team != rc.getTeam()) {
    		rc.setIndicatorString(0, "Capturing");
            doWorkerCreation();
            actionQueued = true;
			Message m = new Message();
			m.ints = new int[MESSAGE_MIN_INTS];
			m.ints[INDEX_CMD] = MSG_FLUX_TAKEN;
			m.locations = new MapLocation[1];
			m.locations[0] = info.location;
			sendMessageToAll(m);
            isMining = true;
        } else if (info.roundsAvailableAtCurrentHeight > 0) {
    		rc.setIndicatorString(0, "Mining");
            doWorkerCreation();
            actionQueued = true;
            isMining = true;
        } else {
            isMining = false;
            searchGoal = TASK_EXPLORE;
            exploreRounds = 0;
        }
	}
	void doSearchForFlux() throws GameActionException {
		rc.setIndicatorString(0, "Searching flux");
		Direction fluxDir = rc.senseDirectionToUnownedFluxDeposit();
		switch (fluxDir) {
			case OMNI:
				break;
			case NONE:
				fluxDir = rc.senseDirectionToOwnedFluxDeposit();
			default: // fall through
				if (!rc.isMovementActive() && !rc.hasActionSet())
				{
					if (rc.canMove(fluxDir) && myDir != fluxDir)
						rc.setDirection(fluxDir);
					else if(rc.canMove(myDir)) rc.moveForward();
					else rc.setDirection(myDir.rotateRight());
                    actionQueued = true;
				}
				break;
		}
	}
}
