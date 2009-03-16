package team372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import java.lang.*;

public class RobotPlayer implements Runnable {

	private final RobotController rc;
	private AbstractRobot player;

	public RobotPlayer(RobotController _rc)
	{
		rc = _rc;
		switch (rc.getRobotType())
		{
			case ARCHON:
				player = new Archon(rc);
				break;
			case CHANNELER:
				player = new Channeler(rc);
				break;
			case SOLDIER:
				player = new Soldier(rc);
				break;
			case SCOUT:
				player = new ScoutLeader(rc);
				break;
            case CANNON:
                player = new Cannon(rc);
                break;
            case WORKER:
                player = new Worker(rc);
                break;
			default:
				player = new Dummy(rc);
				break;
		}
	}

	public void run()
	{
		while (true)
		{
			try
			{
                player.run();
                // A to po co???
			/*	switch (player.run())
				{
					case 0:
						player = new Archon(rc);
						break;
					default:
						player = new Dummy(rc);
						break;
				}*/
			} catch (Exception e)
			{
				System.out.println("caught exception:");
				e.printStackTrace();
        //chwilowo, zeby uniknac kaskady wyjatkow
        rc.yield();
			}
		}
	}
}
