package team372;

/**
 * Stale dla wiadomosci. Kazda wiadomosc zawiera
 * na ints[0] typ wiadomosci o poczatku MSG_
 * FIX: id odbiorcy zawsze na ints[1]
 * @author Kropla
 */
public class MessageConstants {

	/** stale opisujace wyglad pola 'ints' w wiadomosci **/
	public static final int INDEX_TAG = 0;
	public static final int INDEX_HASH = 1;
	public static final int INDEX_ROUND = 2;
	public static final int INDEX_WHO = 3;
	public static final int INDEX_CMD = 4;
	public static final int MESSAGE_TAG = 0xBABED011;
	public static final int MESSAGE_HASH_START = 0xDEADBEAF;
	public static final int MESSAGE_MIN_INTS = 5;

	/** stala uzywana dla pola 'who' zamiast
	 * identyfikatora
	 **/
	public static final int ID_BROADCAST = -1;

	/** wiadomosc bledna, nie powinna byc wysylana przez nikogo
	 * 
	 **/
	public static final int MSG_ZERO = 0;

	/** wiadomosc powiadamiajaca o wlasnej lokazlizacji
	 * zawiera:
	 * id nadawcy na ints[2]
	 * lokacje nadawcy na locations[0]
	 **/
	public static final int MSG_HERE_I_AM = 1;

	/** wiadomosc powiadamiajaca o zajeciu zloza
	 **/
	public static final int MSG_FLUX_FOUND = 2;
	public static final int MSG_PROTECT_ARCHON = 4;
	public static final int MSG_START_OFFENSIVE = 3;
	/* locations[0] - lokacja do której trzeba iść
	*/

	/** wiadomosc wysylajaca robota do danej lokacji
	 * zawiera:
	 * id odbiorcy na ints[1]
	 * lokacje docelowa na locations[0]
	 **/
	public static final int MSG_GOTO = 5;

	/** wiadomosc jak poprzednia, ale kazaca takze
	 * atakowac przeciwnika
	 **/
	public static final int MSG_GO_ATTACK = 6;

	/** wiadomosc o spostrzezeniu wroga **/
	public static final int MSG_ENEMY_SPOTTED = 7;

	/** archon natrafil na dzialo i wola o pomoc **/
	public static final int MSG_HELP = 8;
	
	/** oznacza, ze zloze zostalo zarezerwowane **/
	public static final int MSG_FLUX_TAKEN = 9;

    public static final int MSG_DRAIN = 10;

    /** wiadomosc proszaca scouta o przekazanie wiadomosci archonowi.
	 * zawiera:
	 * id scouta na ints[1]
	 * indeks archona docelowego w tablicy senseAlliedArchons na ints[2]
	 * pozostele informacje scout przekazuje archonowi docelowemu
	 **/
	public static final int MSG_LETTER = 11;

    /**
     * Wyslanie lokacji wrogich robotow do cannona.
     */
    public static final int MSG_ENEMY_LOCATIONS = 12;

    /**
     * Polecenie dla workera budowania wiezy.
     * Zawiera:
     * - lokalizacje zloza flux do budowania wiezy na niej na locations[0]
     * - lokalizacje blokow widzianych przez archona na dalszych polach
     */
    public static final int MSG_BUILD_TOWER = 13;

	public static final int MSG_EXPLORE = 14;

    public static final int MSG_JOIN_GROUP= 14;

}
