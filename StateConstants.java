package team372;

/**
 *
 * @author kropla
 */
public class StateConstants {
	public static final int ATTACK = 0;
	public static final int DEFENSE = 1;
	public static final int MINING = 2;
	public static final int CAPTURING = 7;
	public static final int TRAVEL = 3;
	public static final int TRAVEL_RANDOM = 4;
	public static final int SEARCH_FOR_FLUX = 5;
	public static final int RUN_AWAY = 6;
	public static final int JOINING_ARCHON = 8;
	public static final int SENDING_MESSENGER = 9;
	// TODO: to trzeba uzgodnic
	public static final int STATE_IDLE = 10;
	public static final int STATE_PROTECT_ARCHON = 11;
	public static final int STATE_OFFENSIVE = 12;
	public static final int STATE_GO_ATTACK = 13;
	public static final int RESCUE = 14;
	
	public static final int TASK_DEFENCE = 100;
	public static final int TASK_ATTACK = 101;
	public static final int TASK_RESCUE = 102;
	public static final int TASK_ESCAPE = 103;
    public static final int TASK_NONE = 104;

    public static final int TASK_SEARCH_FOR_FLUX = 200;
    public static final int TASK_EXPLORE = 201;

    public static final int TASK_GET_BLOCK = 300;
    public static final int TASK_PUT_BLOCK = 301;

	public static final int TASK_PROTECT_ARCHON = 400;
	public static final int TASK_GO_ATTACK = 401;
	public static final int TASK_OFFENSIVE = 402;
    public static final int TASK_FOLLOW_LEADER = 403;

	public static final int TASK_DELIVER_MESSAGE = 500;
}
