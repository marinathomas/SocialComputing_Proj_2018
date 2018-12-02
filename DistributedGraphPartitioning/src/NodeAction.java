import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;

public class NodeAction implements Runnable {

	Node node = null;

	public NodeAction(Node currentNode) {
		this.node = currentNode;
	}

	@Override
	public void run() {
		long id = Thread.currentThread().getId();
		ServerSocket serverSocket;
		try {
			serverSocket = new ServerSocket(Node.myPort);

			Socket socket;
			while ((socket = serverSocket.accept()) != null) {
				PrintWriter output = new PrintWriter(socket.getOutputStream());
				Scanner received = new Scanner(socket.getInputStream());

				System.out.print("[ Node(TCP): " + String.valueOf(id) + "]\n");

				processActions(received);

				output.flush();
				output.close();
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param command
	 * @param scanner
	 * @return
	 * 
	 */
	synchronized void processActions(Scanner received) {

		// 1. Get the action
		String action = received.next();
		System.out.println(">>>>> Processing Command : " + action);
		switch (action) {
		case "initiate":
			// node.state = Node.ACTIVE;
			if (received.hasNext()) {
				String[] initiatorInfo = received.next().split(":");
				Node.initiatorIP = initiatorInfo[0];
				Node.initiatorPort = Integer.valueOf(initiatorInfo[1]);
			}
			node.shortestPathsDistance[Node.myID - 1] = 0;
			callNeighbors(Node.myID, 0);
			// node.state = Node.PASSIVE;
			break;
		case "path":
			// node.state = Node.ACTIVE;
			int beginningNode = -1;
			if (received.hasNext()) {
				beginningNode = Integer.parseInt(received.next());
			}
			System.out.println("beginningNode is >>>>>> " + beginningNode + " deficit>>>> "	+ node.deficientMap.get(beginningNode));
			if (received.hasNext()) {
				int dist = Integer.parseInt(received.next());
				int caller = Integer.valueOf(received.next());
				System.out.println("Distance received >>>>>> " + dist + " from " + caller + " current distance>>>> "
						+ node.shortestPathsDistance[beginningNode - 1]);

				if (dist <= (node.shortestPathsDistance[beginningNode - 1]-1)) {
					ArrayList<Integer> parents = node.parentMap.get(beginningNode);
					// strictly less - ignore previous parents
					if (dist != (node.shortestPathsDistance[beginningNode - 1] - 1)) {
						System.out.println("Distance received is less. Need to send marker to parents");
						for (int i = 0; i < parents.size(); i++) {
							sendMarkerToParent(beginningNode, parents.get(i), "marker", null);
						}
						System.out.println("Done sending marker. reset parents.");
						parents = new ArrayList<>();
						parents.add(caller);
						node.parentMap.put(beginningNode, parents);
						node.shortestPathsDistance[beginningNode - 1] = dist + 1;
						System.out.println("Call neighbors");
						callNeighbors(beginningNode, dist + 1);
					}else {
						System.out.println("Distance received is equal. Not calling neighbors again.");
						if(!node.parentMap.get(beginningNode).contains(caller)) {
							parents.add(caller);
							node.parentMap.put(beginningNode, parents);

						}else {
							System.out.println("Parent already present. Sending back marker.");
							sendMarkerToParent(beginningNode, caller, "marker", null);
						}
					}

				} else {
					sendMarkerToParent(beginningNode, caller, "marker", null);
				}
			}
			// node.state = Node.PASSIVE;
			break;
		case "marker":
			// node.state = Node.ACTIVE;
			beginningNode = -1;
			if (received.hasNext()) {
				beginningNode = Integer.parseInt(received.next());
			}

			int markerFrom = Integer.parseInt(received.next());

			Integer deficiencyCount = node.deficientMap.get(beginningNode);
			deficiencyCount--;
			node.deficientMap.put(beginningNode, deficiencyCount);
			System.out.println("Received marker from>>>>"+markerFrom+" beginningNode is >>>>>> " + beginningNode + " deficit>>>> "
					+ node.deficientMap.get(beginningNode));
			// }

			if (node.deficientMap.get(beginningNode) == 0) {
				System.out.println(
						"deficit>>>> " + node.deficientMap.get(beginningNode) + " beginningNode >>>>>>" + beginningNode);
				if (beginningNode != Node.myID) {
					sendMarkerToParents(beginningNode, "marker", null);
				} else {
					System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>DONE<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
					System.out.println("Before sending done initiate to neighbors: deficit>>>> "
							+ node.deficientMap.get(beginningNode) + " beginningNode(MyID)>>>" + beginningNode);
					node.nodesInitiateDone.add(Node.myID);
					sendMessageToNeighbors(beginningNode, getNodesDoneAsString(), "DoneInitiate");
					
				}

			}
			break;
		case "DoneInitiate":
			// node.state = Node.ACTIVE;

			beginningNode = received.nextInt();
			// System.out.println("Received DoneInitiate from>>"+beginningNode);
			String[] receivedNodes = null;
			int initialKnowldge = node.nodesInitiateDone.size();
			if (received.hasNext()) {
				receivedNodes = received.next().split(":");
				// System.out.println("Received nodes >>>"+receivedNodes);
				for (String nodeDone : receivedNodes) {
					Integer intNode = Integer.valueOf(nodeDone);
					if (!node.nodesInitiateDone.contains(intNode)) {
						node.nodesInitiateDone.add(intNode);
					}
				}
				System.out.println("Nodes that are done with calculating all shortest paths to them are:");
				for (Integer nodeDone : node.nodesInitiateDone) {
					System.out.println(nodeDone);
				}
			}

			if (initialKnowldge < node.nodesInitiateDone.size()) {
				// gained knowledge, propagate it
				sendMessageToNeighbors(beginningNode, getNodesDoneAsString(), "DoneInitiate");
			}

			//Every node is done asking other nodes to calculate the shortest paths to itself
			if((node.nodesInitiateDone.size() == Node.totalNumberOfServers) && !node.termination && !node.parentValueInitiated) {
				String parentsAsString = "";
				for (int i = 0; i < node.shortestPathsDistance.length; i++) {
					int j = i + 1;
					System.out.println("Shortest distance to " + j + " is " + node.shortestPathsDistance[i]);
					System.out.println("Parents for " + j + " are " + getParentsAsString(node.parentMap.get(j)));
					parentsAsString = parentsAsString + j + "=" + getParentsAsString(node.parentMap.get(j)) + ":";
					if(node.shortestPathsDistance[i] == Integer.MAX_VALUE) {
						System.out.println("Cannot reach all nodes. TERMINATION!");
						node.termination = true;
						break;
					}
				}
				
				if(!node.termination && !node.parentValueInitiated) {
					checkAndInitiateParentValueCalculation();
				}else {
					callBackInitiator(true);
				}
			}

			// node.state = Node.PASSIVE;
			break;
		case "parentValue":
			// node.state = Node.ACTIVE;
			beginningNode = Integer.parseInt(received.next());
			Integer parentValueReceived = Integer.valueOf(received.next());
			Integer parent = Integer.valueOf(received.next());

			ArrayList<Integer> parents = node.parentMap.get(beginningNode);
			boolean parentPresent = parents.contains(parent);
			System.out.println("parentValue received >>>>>> " + parentValueReceived + " from >>>> " + parent
					+ " for >>>" + beginningNode + " parentPresent>>>" + parentPresent);

			if (parentPresent) {
				ArrayList<Integer> parentsValues = node.parentValueMap.get(beginningNode);
				parentsValues.add(parentValueReceived);
				node.parentValueMap.put(beginningNode, parentsValues);
				// have I received parent values of all my parents?
				if (parentsValues.size() == parents.size()) {
					int myValue = calculateMyParentValue(beginningNode);
					boolean calledNeighbors = askParentValueToNeighbors(beginningNode, myValue);
					if (!calledNeighbors) {// I am a leaf, parent to none
						System.out.println(">>>>>>>>>>>>>>NO NEIGHBORS TO CALL !!!!<<<<<<<<<<<<<<<<<<");
						String parentString = "";
						for (int j = 0; j < parents.size(); j++) {
							parentString = parentString + parents.get(j) + ",";
						}
						parentString = parentString.substring(0, parentString.length() - 1);
						String myValueString = Node.myID + ":" + myValue + ":" + parentString;

						System.out.println(" deficit before sending parentValueMarker>>>> "
								+ node.deficientMap.get(beginningNode) + " beginningNode>>>" + beginningNode);
						sendMarkerToParents(beginningNode, "parentValueMarker", myValueString);
					}
				}
			} else {
				sendMarkerToParent(beginningNode, parent, "parentValueMarker", null);
			}

			// node.state = Node.PASSIVE;
			break;
		case "parentValueMarker":
			// node.state = Node.ACTIVE;
			beginningNode = -1;
			if (received.hasNext()) {
				beginningNode = Integer.parseInt(received.next());
			}
			markerFrom = Integer.parseInt(received.next());


			String parentValue = null;
			if (received.hasNext()) {
				parentValue = received.next();
				ArrayList<String> combinedParentValues = node.allParentValues.get(beginningNode);
				combinedParentValues.add(parentValue);
			}

			// synchronized (node.deficientMap) {
			System.out.println("Received parentmarker from>>>>"+markerFrom+" beginningNode is >>>>>> " + beginningNode + " deficit>>>> "
					+ node.deficientMap.get(beginningNode) + " parentValue>>>>>>>" + parentValue);
			deficiencyCount = node.deficientMap.get(beginningNode);
			deficiencyCount--;
			node.deficientMap.put(beginningNode, deficiencyCount);
			// }
			System.out.println("beginningNode is >>>>>> " + beginningNode + " deficit>>>> "
					+ node.deficientMap.get(beginningNode));

			// node.state = Node.PASSIVE;
			// if (node.state == Node.PASSIVE && node.deficientMap.get(beginningNode) == 0)
			// {
			if (node.deficientMap.get(beginningNode) == 0) {
				if (beginningNode != Node.myID) {
					String combinedParentValue = getCombinedParentValue(beginningNode);
					System.out.println("Got parentValue from all neighbors. Send info to parents.");
					sendMarkerToParents(beginningNode, "parentValueMarker", combinedParentValue);
				} else {
					System.out.println("===================DONE PARENT VALUE CALCULATION=====================");
					String combinedParentValue = getCombinedParentValue(beginningNode);
					System.out.println("PARENT VALUE" + combinedParentValue);
					node.parentValueString = combinedParentValue;

					node.nodesParentValueDone.add(Node.myID);
					sendMessageToNeighbors(beginningNode, getNodesPValueDoneAsString(), "DoneParentValue");

					checkAndProcessParentValueComplete();
				}

			}
			break;
		case "DoneParentValue":
			// node.state = Node.ACTIVE;

			beginningNode = received.nextInt();
			// System.out.println("Received DoneInitiate from>>"+beginningNode);
			receivedNodes = null;
			initialKnowldge = node.nodesParentValueDone.size();
			if (received.hasNext()) {
				receivedNodes = received.next().split(":");
				// System.out.println("Received nodes >>>"+receivedNodes);
				for (String nodeDone : receivedNodes) {
					Integer intNode = Integer.valueOf(nodeDone);
					if (!node.nodesParentValueDone.contains(intNode)) {
						node.nodesParentValueDone.add(intNode);
					}
				}
				System.out.println("Nodes that are done with calculating all parent values to them are:");
				for (Integer nodeDone : node.nodesParentValueDone) {
					System.out.println(nodeDone);
				}
			}

			if (initialKnowldge < node.nodesParentValueDone.size()) {
				// gained knowledge, propagate it
				sendMessageToNeighbors(beginningNode, getNodesPValueDoneAsString(), "DoneParentValue");
			}

			checkAndProcessParentValueComplete();

			// node.state = Node.PASSIVE;
			break;
		case "betweenness":

			beginningNode = Integer.parseInt(received.next());
			int initialKnowledge = node.betweennessMap.keySet().size();
			String incomingBetweennessValue = received.next();
			//System.out.println("INCOMNG VALUE:" + incomingBetweennessValue);

			// pattern of the incoming string :
			// nodeId=rowId:colId:betweenVal,rowId:colId:betweenVal;nodeId=rowId:colId:betweenVal,rowId:colId:betweenVal
			String[] incomingBetweennessValueArray = incomingBetweennessValue.split(";");
			for (String strValue : incomingBetweennessValueArray) {
				String incomingNode = strValue.split("=")[0];
				String nodeValue = strValue.split("=")[1];
				if (!node.betweennessMap.containsKey(incomingNode)) {
					node.betweennessMap.put(incomingNode, nodeValue);
				}

			}

			if (initialKnowledge < node.betweennessMap.keySet().size()) {
				String combinedBetweennessString = getCombineBetweennessString();
				System.out.println("COMBINED STRING:" + combinedBetweennessString);
				sendMessageToNeighbors(Node.myID, combinedBetweennessString, "betweenness");
			}

			checkAndProcessBetweennessValueComplete();

			break;
		default:
			System.out.println("WRONG ACTION");
		}
	}

	private synchronized void checkAndProcessBetweennessValueComplete() {
		if (!node.maxBetweennessInitiated && (node.betweennessMap.keySet().size() == Node.totalNumberOfServers)) {

			node.maxBetweennessInitiated = true;
			System.out.println("EVERYONE DONE CALCULATING BETWEENNESS");

			Iterator<String> betweennessValuesItr = node.betweennessMap.keySet().iterator();
			while (betweennessValuesItr.hasNext()) {
				String id = betweennessValuesItr.next();
				if (Integer.parseInt(id) != Node.myID) {
					//System.out.println("Adding>>"+id);
					String[] allValues = node.betweennessMap.get(id).split(",");
					for(String str: allValues) {
					//System.out.println("Value to add>>"+str);
					int xVal = Integer.parseInt(str.split(":")[0]);
					int yVal = Integer.parseInt(str.split(":")[1]);
					double betVal = Double.parseDouble(str.split(":")[2]);
					//System.out.println("Before>>"+node.betweennessMatrix[xVal][yVal]);
					node.betweennessMatrix[xVal][yVal] = node.betweennessMatrix[xVal][yVal]+betVal;
					//System.out.println("After>>"+node.betweennessMatrix[xVal][yVal]);
					}
				}
			}

			printBetweennessMatrix();

			// findMaxBetweenness
			double maxValue = 0;
			for (int i = 0; i < Node.totalNumberOfServers; i++) {
				for (int j = 0; j < Node.totalNumberOfServers; j++) {
					if (node.betweennessMatrix[i][j] > maxValue) {
						maxValue = node.betweennessMatrix[i][j];
					}
				}
			}

			// check if max betweenness is between me and any of my neighbors. If so remove
			ArrayList<Integer> neighborsToRemove = new ArrayList<>();
			for (int i = 0; i < Node.totalNumberOfServers; i++) {
				if (node.betweennessMatrix[Node.myID - 1][i] == maxValue) {
					System.out.println("Need to remove my neighbor" + i + 1);
					neighborsToRemove.add(i + 1);
				}
			}

			// update neighbor information locally
			ArrayList<String> newNeighbors = new ArrayList<>();
			for (String n : Node.myNeighbors) {
				String nId = n.split(" ")[0];
				if (!neighborsToRemove.contains(Integer.parseInt(nId))) {
					newNeighbors.add(n);
				}
			}
			Node.myNeighbors = newNeighbors.toArray(new String[newNeighbors.size()]);
			System.out.println("Finished modifying neighbors. New neighbors are below:");
			for (int nCount = 0; nCount < Node.myNeighbors.length; nCount++) {
				System.out.println(Node.myNeighbors[nCount]);
			}

			// send info to initiator to print matrix
			callBackInitiator(false);

			node.reset();
			node.shortestPathsDistance[Node.myID - 1] = 0;
			callNeighbors(Node.myID, 0);

		}
	}

	private synchronized void checkAndInitiateParentValueCalculation() {
			// initiate parent value calculation
			// node.nodesInitiateDone = new ArrayList<>();
			node.parentValueInitiated = true;
			//node.firstRound = false;
			initiateParentValueCalculation();
	}

	private synchronized void checkAndProcessParentValueComplete() {
		if (!node.betweennessInitiated && (node.nodesParentValueDone.size() == Node.totalNumberOfServers)) {
			node.betweennessInitiated = true;
			node.betweennessMap = new HashMap<>();
			// initiate betweenness calculation
			calculateFlowValue();
			// send flowValue ie my betweenness measures to neighbors
			// this should include - MyID(beginning node) columns separated by : and rows
			// separated by ;
			String betweennessString = getMyBetweennessString();
			node.betweennessMap.put(String.valueOf(Node.myID), betweennessString);
			String combinedBetweennessString = getCombineBetweennessString();
			
			//reset
			node.maxBetweennessInitiated = false;

			sendMessageToNeighbors(Node.myID, combinedBetweennessString, "betweenness");
		}
	}

	/*
	 * 
	 * Betweenness string pattern
	 * nodeId=rowId:colId:betweenVal,rowId:colId:betweenVal;nodeId=rowId:colId:
	 * betweenVal,rowId:colId:betweenVal
	 */
	private String getCombineBetweennessString() {
		Iterator<String> itr = node.betweennessMap.keySet().iterator();
		String strB = "";
		while (itr.hasNext()) {
			String key = itr.next();
			if (strB.equals("")) {
				strB = key + "=" + node.betweennessMap.get(key);
			} else {
				strB = strB + ";" + key + "=" + node.betweennessMap.get(key);
			}

		}
		return strB;
	}

	private synchronized String getMyBetweennessString() {
		String rowValue = "";
		for (int i = 0; i < Node.totalNumberOfServers; i++) {
			for (int j = 0; j < Node.totalNumberOfServers; j++) {
				//System.out.println("BETWEEN>>>>>>>" + node.betweennessMatrix[i][j]);
				if (node.betweennessMatrix[i][j] > 0.0) {
					if (rowValue.equals("")) {
						rowValue = i + ":" + j + ":" + node.betweennessMatrix[i][j];
					} else {
						rowValue = rowValue + "," + i + ":" + j + ":" + node.betweennessMatrix[i][j];
					}
				}

			}

		}
		//System.out.println("MATRIX BEFORE UPDATE");
		printBetweennessMatrix();
		
		//System.out.println("My Betweenness String"+rowValue);

		return rowValue;
	}

	private String getNodesDoneAsString() {
		String val = "";
		for (int i = 0; i < node.nodesInitiateDone.size(); i++) {
			String strVal = node.nodesInitiateDone.get(i).toString();

			if (val.equals("")) {
				val = strVal;
			} else {
				val = val + ":" + strVal;
			}

		}
		return val;
	}

	private String getNodesPValueDoneAsString() {
		String val = "";
		for (int i = 0; i < node.nodesParentValueDone.size(); i++) {
			String strVal = node.nodesParentValueDone.get(i).toString();

			if (val.equals("")) {
				val = strVal;
			} else {
				val = val + ":" + strVal;
			}

		}
		return val;
	}

	private void initiateParentValueCalculation() {
		System.out.println("About to call Neighbors for parentValue: deficit>>>> " + node.deficientMap.get(Node.myID));
		askParentValueToNeighbors(Node.myID, 1);
	}

	private String getCombinedParentValue(int beginningNode) {
		int myValue = calculateMyParentValue(beginningNode);
		ArrayList<Integer> parents = node.parentMap.get(beginningNode);
		String parentString = "";
		for (int j = 0; j < parents.size(); j++) {
			parentString = parentString + parents.get(j) + ",";
		}
		if (parentString.equals("")) {
			parentString = Node.myID.toString();
		} else {
			parentString = parentString.substring(0, parentString.length() - 1);
		}
		String myValueString = Node.myID + ":" + myValue + ":" + parentString;

		ArrayList<String> parentsValues = node.allParentValues.get(beginningNode);
		for (int i = 0; i < parentsValues.size(); i++) {
			myValueString = myValueString + ";" + parentsValues.get(i);

		}
		return myValueString;
	}

	private void callBackInitiator(boolean termination) {
		String neighborInfo = getMyNeighborIDs();
		if (!termination) {
			sendMessage(Node.myID + " " + neighborInfo, Node.initiatorIP, Node.initiatorPort);
		} else {
			sendMessage(Node.myID + " termination", Node.initiatorIP, Node.initiatorPort);
		}

	}

	private String getMyNeighborIDs() {
		String neighborInfo = "";
		for (String neighbor : Node.myNeighbors) {
			String[] neighborDetails = neighbor.split(" ");
			String neighbor_id = neighborDetails[0];
			if (neighborInfo.equals("")) {
				neighborInfo = neighbor_id;
			} else {
				neighborInfo = neighborInfo + ":" + neighbor_id;
			}
		}
		return neighborInfo;
	}

	private ArrayList<Integer> findLeafNodes() {
		int[] parents = new int[Node.totalNumberOfServers];
		String[] parentValues = node.parentValueString.split(";");
		for (String val : parentValues) {
			//System.out.println("ParentVal>>>"+val);
			String strParents = val.split(":")[2];
			String[] parentArray;
			if (strParents.contains(",")) {
				parentArray = strParents.split(",");
			} else {
				parentArray = new String[] { strParents };
			}
			for (String p : parentArray) {
				parents[Integer.parseInt(p) - 1] = 1;
			}
		}

		ArrayList<Integer> leaves = new ArrayList<>();
		for (int j = 0; j < Node.totalNumberOfServers; j++) {
			if (parents[j] == 0) {
				leaves.add(j + 1);
			}
		}
		return leaves;
	}

	private int findParentVal(int parentId, int beginningNode) {
		int parentVal = 0;

		String[] parentValues = node.parentValueString.split(";");
		for (String val : parentValues) {
			String[] strArray = val.split(":");
			int id = Integer.parseInt(strArray[0]);
			if (id == parentId) {
				parentVal = Integer.parseInt(strArray[1]);
				break;
			}

		}

		return parentVal;

	}

	private synchronized void calculateFlowValue() {
		// for(int i=0; i<numberOfServers; i++) {
		// array to track the servers already considered
		ArrayList<Integer> nodeList = new ArrayList<>();
		// find nodes that is/are not parent to any other node
		ArrayList<Integer> leafNodes = findLeafNodes();
		Queue<Integer> nodes = new LinkedList<>();
		// incoming value array
		double[] incomingVal = new double[Node.totalNumberOfServers];
		// add leaves to the queue
		for (Integer node : leafNodes) {
			nodes.add(node);
			nodeList.add(node);
		}
		// int[] parents = new int[numberOfServers];
		// while queue not empty - pop - add parents to the queue
		String[] parentValues = node.parentValueString.split(";");
		HashMap<String, String> tempMap = new HashMap<>();
		for (String val : parentValues) {
			String[] strArray = val.split(":");
			if(!tempMap.containsKey(strArray[0])) {
				tempMap.put(strArray[0], val);
			}
		}
		while (!nodes.isEmpty()) {
			Integer id = nodes.remove();
			String currentVal = tempMap.get(String.valueOf(id));
			String[] strArray = currentVal.split(":");
			String[] strParents = strArray[2].split(",");
			int parentVal = Integer.parseInt(strArray[1]);
			for(String parent:strParents) {
				if(!nodeList.contains(Integer.parseInt(parent))) {
					nodes.add(Integer.parseInt(parent));
					nodeList.add(Integer.parseInt(parent));
				}
			}
			for(String parent:strParents) {
				int pInt = Integer.parseInt(parent);
				int superParentVal = findParentVal(pInt, Node.myID);

				double edgeVal = 0.0;
				if (parentVal != 0) {
					edgeVal = (incomingVal[id - 1] + 1) * superParentVal / parentVal;
				}
				System.out.println("superParentVal>>" + superParentVal + " parentVal>>" + parentVal + " edgeVal"
						+ edgeVal + "child>>" + id + " parent>>" + pInt);
				//System.out.println("BEFORE1:" + node.betweennessMatrix[id - 1][pInt - 1]);
				//System.out.println("BEFORE2:" + node.betweennessMatrix[pInt - 1][id - 1]);
				node.betweennessMatrix[id - 1][pInt - 1] =  edgeVal;
				 node.betweennessMatrix[pInt - 1][id - 1] =  edgeVal;
				//System.out.println("AFTER1:" + node.betweennessMatrix[id - 1][pInt - 1]);
				//System.out.println("AFTER2:" + node.betweennessMatrix[pInt - 1][id - 1]);
				incomingVal[pInt - 1] = incomingVal[pInt - 1] + edgeVal;
			}
			
		}

		printBetweennessMatrix();

	}

	private int calculateMyParentValue(int beginningNode) {
		int myValue = 0;
		ArrayList<Integer> parentsValues = node.parentValueMap.get(beginningNode);
		for (int i = 0; i < parentsValues.size(); i++) {
			myValue = myValue + parentsValues.get(i);
		}
		return myValue;
	}

	private boolean askParentValueToNeighbors(int beginningNode, int parentValue) {
		boolean calledNeighbors = false;
		for (String neighbor : Node.myNeighbors) {
			String[] neighborDetails = neighbor.split(" ");
			Integer neighborID = Integer.parseInt(neighborDetails[0]);

			ArrayList<Integer> parents = node.parentMap.get(beginningNode);
			System.out.println("Parent present:" + parents.contains(neighborID));
			if (parents.contains(neighborID)) {// don't call parents for parentValue
				continue;
			} else {
				calledNeighbors = true;
			}

			String[] ipDetails = neighborDetails[1].split(":");
			String neighborIP = ipDetails[0];
			Integer neighborPort = Integer.parseInt(ipDetails[1]);

			synchronized (node.deficientMap) {
				int deficient = node.deficientMap.get(beginningNode);
				deficient++;
				node.deficientMap.put(beginningNode, deficient);
				System.out.println("Call Neighbors for parentValue: deficit>>>> " + node.deficientMap.get(beginningNode)
						+ " beginningNode>>>" + beginningNode);
			}

			// pattern = command TheBeginningNode myParentValue myID
			sendMessage("parentValue " + beginningNode + " " + parentValue + " " + Node.myID, neighborIP, neighborPort);

		}

		return calledNeighbors;
	}

	private String getParentsAsString(ArrayList<Integer> parentList) {
		String parents = "";
		for (int i = 0; i < parentList.size(); i++) {
			if ("".equals(parents)) {
				parents = parents + parentList.get(i);
			} else {
				parents = parents + "," + parentList.get(i);
			}

		}
		return parents;
	}

	private void sendMessageToNeighbors(Integer beginningNode, String message, String command) {
		for (String neighbor : Node.myNeighbors) {
			String[] neighborDetails = neighbor.split(" ");
			String[] ipDetails = neighborDetails[1].split(":");
			String neighborIP = ipDetails[0];
			Integer neighborPort = Integer.parseInt(ipDetails[1]);

			if (message == null) {
				sendMessage(command + " " + beginningNode, neighborIP, neighborPort);
			} else {
				sendMessage(command + " " + beginningNode + " " + message, neighborIP, neighborPort);
			}
		}
	}


	private void sendMarkerToParents(Integer beginningNode, String message, String message2) {
		ArrayList<Integer> parents = node.parentMap.get(beginningNode);
		for (Integer parent : parents) {
			sendMarkerToParent(beginningNode, parent, message, message2);
		}
	}

	private void sendMarkerToParent(Integer beginningNode, Integer parent, String message1, String message2) {
		System.out.println("Send marker to parent " + parent + " for beginningNode " + beginningNode);
		String[] ipDetails = getNeighborDetails(parent).split(":");
		String neighborIP = ipDetails[0];
		Integer neighborPort = Integer.parseInt(ipDetails[1]);

		System.out.println("Sending marker. Ideally deficit should be 0. Current deficit is: "
				+ node.deficientMap.get(beginningNode));
		String message = message1 + " " + beginningNode+" "+Node.myID;
		if (message2 != null) {
			message = message + " " + message2;
		}
		sendMessage(message, neighborIP, neighborPort);
	}

	private String getNeighborDetails(Integer id) {
		String neighborInfo = null;
		for (String neighbor : Node.myNeighbors) {
			String[] neighborDetails = neighbor.split(" ");
			String neighbor_id = neighborDetails[0];
			if (Integer.parseInt(neighbor_id) == id) {// to send to a specific parent
				neighborInfo = neighborDetails[1];
				break;
			}
		}
		return neighborInfo;

	}

	private synchronized void callNeighbors(int beginningNode, int distanceToMe) {
		for (String neighbor : Node.myNeighbors) {
			String[] neighborDetails = neighbor.split(" ");
			String[] ipDetails = neighborDetails[1].split(":");
			String neighborIP = ipDetails[0];
			Integer neighborPort = Integer.parseInt(ipDetails[1]);

			synchronized (node.deficientMap) {
				int deficient = node.deficientMap.get(beginningNode);
				deficient++;
				node.deficientMap.put(beginningNode, deficient);
				System.out.println("Call Neighbors to get shortest distance: deficit>>>> "
						+ node.deficientMap.get(beginningNode) + " beginningNode>>>" + beginningNode);
			}

			// pattern = command TheBeginningNode theDistanceToMe myID
			sendMessage("path " + beginningNode + " " + distanceToMe + " " + Node.myID, neighborIP, neighborPort);

		}
	}

	private void sendMessage(String cmd, String neighborIP, Integer neighborPort) {

		System.out.println("Sending Msg >> " + cmd + " neighborIP " + neighborIP + " neighborPort " + neighborPort);

		try {
			InetAddress address = InetAddress.getByName(neighborIP);
			Socket socket = new Socket(address, neighborPort);
			// socket.setSoTimeout(TIMEOUT);
			PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
			// Scanner inFromServer = new Scanner(new
			// InputStreamReader(socket.getInputStream()));
			outToServer.println(cmd);
			outToServer.flush();
			socket.close();
		} catch (SocketTimeoutException t) {
			System.out.println("Node at " + neighborIP + ":" + neighborPort.toString() + " TIMEOUT!");
		} catch (Exception e) {
			System.out.println("Node at " + neighborIP + ":" + neighborPort.toString() + " ERROR:\t" + e.getMessage());
			e.printStackTrace();
		}

	}

	private void printBetweennessMatrix() {
		for (int i = 0; i < Node.totalNumberOfServers; i++) {
			String rowValue = "";
			for (int j = 0; j < Node.totalNumberOfServers; j++) {
				if (rowValue.equals("")) {
					rowValue = node.betweennessMatrix[i][j] + "";
				} else {
					rowValue = rowValue + ", " + node.betweennessMatrix[i][j];
				}

			}
			//System.out.println(rowValue);
		}
	}

}
