import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Node {

	public final static int ACTIVE = 1;
	public final static int PASSIVE = 0;

	static int myPort;
	static String myIP;
	static Integer myID;
	static String[] myNeighbors;
	static int totalNumberOfServers;
	static String initiatorIP;
	static Integer initiatorPort;

	//int state = PASSIVE;
	int[] shortestPathsDistance;
	//int[] previousShortestPathsDistance;
	Map<Integer, Integer> deficientMap = Collections.synchronizedMap(new HashMap<>());
	//my parents to reach a specific node
	Map<Integer, ArrayList<Integer>> parentMap = new HashMap<>();
	//For a specific beginning node, who all are my parents
	Map<Integer, ArrayList<Integer>> parentValueMap = new HashMap<>();
	Map<Integer, ArrayList<String>> allParentValues = new HashMap<>();
	String parentValueString="";
	ArrayList<Integer> nodesInitiateDone;
	ArrayList<Integer> nodesParentValueDone;
	//ArrayList<Integer> nodesBetweennessDone;
	ArrayList<Integer> roundComplete;
	double[][] betweennessMatrix;
	Map<String, String> betweennessMap = new HashMap<>();
	boolean parentValueInitiated = false;
	boolean betweennessInitiated = false;
	boolean maxBetweennessInitiated = false;
	//boolean firstRound = true;
	boolean termination = false;
	
	String action;
	static String InputFilePath = ".";

	public static void main(String args[]) {

		// 1. Create new server with server id and port as inputs
		myID = Integer.parseInt(args[0]);
		if (args.length == 2 ) {
			InputFilePath = args[1];
		}

		System.out.println("MY ID >>"+myID);
		System.out.println("InputFilePath >>"+InputFilePath);
		// 2. Read from file the my IP and details of my neighbors
		setup();

		// 3 set initial distance to max value
		Node node = new Node();
		node.reset();

		// 4. Open socket and wait for call. Call neighbors to find shortest paths
		node.startUp();

	}

	void reset() {
		shortestPathsDistance = new int[totalNumberOfServers];
		for (int i = 0; i < totalNumberOfServers; i++) {
			shortestPathsDistance[i] = Integer.MAX_VALUE;
		}
		for (int i = 0; i < totalNumberOfServers; i++) {
			deficientMap.put(i + 1, 0);
			parentMap.put(i + 1, new ArrayList<>());
			parentValueMap.put(i + 1, new ArrayList<>());
			allParentValues.put(i + 1, new ArrayList<>());
		}
		parentValueString = "";
		nodesInitiateDone = new ArrayList<>();
		nodesParentValueDone = new ArrayList<>();
		roundComplete = new ArrayList<>();
		betweennessMatrix = new double[totalNumberOfServers][totalNumberOfServers];
		parentValueInitiated = false;
		betweennessInitiated = false;
	}

	private static void setup() {
		String fileName = InputFilePath + "/" + myID + ".txt";
		Input input = new Input(fileName);
		String[] rows = input.read();
		int i = 0;
		myNeighbors = new String[rows.length - 2];
		for (String row : rows) {
			if (i == 0) {
				System.out.println("Number of servers>> " + row);
				totalNumberOfServers = Integer.parseInt(row);
			} else if (i == 1) {
				System.out.println("My Info>> " + row);
				String[] myDetails = row.split(":");
				myIP = myDetails[0];
				myPort = Integer.parseInt(myDetails[1]);
			} else {
				myNeighbors[i - 2] = row;
			}

			i++;
		}
	}

	public void startUp() {
		NodeAction nodeAction = new NodeAction(this);
		new Thread(nodeAction).start();
	}

}
