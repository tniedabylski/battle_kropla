package team372;

import battlecode.common.*;

/**
 *
 * @author lukasz
 */
public abstract class AttackingRobot extends AbstractRobot {
	// otrzymane od archona lokacje wrogich robotow
	protected MapLocation[] enemyRobotLocations = null;
	// lokacja ostatnio widzianego wroga
	protected MapLocation lastEnemyLocation;

	public AttackingRobot(RobotController _rc) {
        super(_rc);
	}

	@Override
	protected void endRound() throws GameActionException {
		enemyRobotLocations = null;
		bestEnemy = null;
		super.endRound();
        if (lastEnemyLocation != null &&
                myLoc.distanceSquaredTo(lastEnemyLocation) < 8 &&
                rc.canSenseSquare(lastEnemyLocation) &&
                !isEnemyAtLocation(lastEnemyLocation)) {
            lastEnemyLocation = null;
        }
	}

	abstract protected void tryAttack() throws GameActionException;

	// Czy jestesmy wystarczajaco blisko zeby chronic Archona, ale nie tak
	// blisko, zeby mogl on nam dawac energon.
	abstract protected boolean isCloseToArchon();

    @Override
	protected void getBestEnemy() throws GameActionException {
		RobotInfo bestOther = null, bestArch = null, bestChan = null;
		int archDist = Integer.MAX_VALUE;
        int chanDist = Integer.MAX_VALUE;
        int dist = Integer.MAX_VALUE;
		for (RobotInfo enemy : nearbyEnemyRobots) {
			if (canAttack(enemy)) {
				int dist2 = enemy.location.distanceSquaredTo(myLoc);
				if (enemy.type.equals(RobotType.ARCHON) && dist2 < archDist) {
					archDist = dist2;
					bestArch = enemy;
				}
                else if (enemy.type.equals(RobotType.CANNON) && dist2 < chanDist) {
					chanDist = dist2;
					bestChan = enemy;
				}
                else if (dist2 < dist) {
					dist = dist2;
					bestOther = enemy;
				}
			}
		}
        if (bestChan != null)
            bestEnemy = bestChan;
        else if (bestArch != null)
            bestEnemy = bestArch;
        else bestEnemy = bestOther;
	}

	protected void protectNearestArchon() throws GameActionException {
        senseNearestArchon();
		boolean archonNearby = isCloseToArchon() && isEnemyNearby();
		if (archonNearby && !rc.hasActionSet() && !rc.isAttackActive() &&
				isEnergonLevelHigh()) {
			tryAttack();
		}
		if (!rc.hasActionSet() && !rc.isMovementActive()) {
			if (nearestArchon != null && !isArchonCloseEnough(nearestArchon)) {
				isNearArchon = false;
				gotoLocation(nearestArchon);
			} else {
				isNearArchon = true;
			}
		}
		if (!rc.hasActionSet() && !rc.isAttackActive() &&
				(isNearArchon || archonNearby)) {
			tryAttack();
		}
	}

	// Returns true if successful.
	protected boolean attack(RobotInfo ri) throws GameActionException {
        if (rc.isAttackActive())
            return false;
		if (ri.type == RobotType.ARCHON || ri.type == RobotType.SCOUT) {
			if (rc.canAttackAir() && rc.canAttackSquare(ri.location)) {
				rc.attackAir(ri.location);
				return true;
			}
		} else {
			if (rc.canAttackGround() && rc.canAttackSquare(ri.location)) {
				rc.attackGround(ri.location);
				return true;
			}
		}
		return false;
	}

	protected boolean canAttack(RobotInfo ri) {
		if (ri.type == RobotType.ARCHON || ri.type == RobotType.SCOUT) {
			return rc.canAttackAir() && rc.canAttackSquare(ri.location);
		} else {
			return rc.canAttackGround() && rc.canAttackSquare(ri.location);
		}
	}

   	protected void offensive() throws GameActionException {
		// precondition: getBestEnemy()
		if (bestEnemy != null) {
			if (!attack(bestEnemy)) {
				gotoLocation(bestEnemy.location);
			}
		} else if (lastEnemyLocation != null) {
			gotoLocation(lastEnemyLocation);
		} else if (!rc.isMovementActive()) {
			doExplore();
		}
	}

}
