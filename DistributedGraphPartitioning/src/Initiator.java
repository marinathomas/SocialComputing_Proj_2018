import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Initiator {

	static int myPort;
	static int numberOfServers;
	static String myIP;
	static HashMap<Integer, String> serverMap = new HashMap<>();
	static int round=0;

	static int[][] serverMatrixArray;
	static ArrayList<Integer> terminationCount;
	static ArrayList<String> edges = new ArrayList<>();

	static HashMap<Integer, String> neighborMap = new HashMap<>();

	static String InputPath = ".";
	static String OutputPath = "bin/output";
	public static void main(String args[]) {
		if (args.length == 2) {
			InputPath = args[0];
			OutputPath = args[1];
		}
		// 1. Read from file names of all servers
		setupEnvironment();

		// 2. Get ready for socket communication
		startUp();

		// 3. Initiate the calculation
		sendMessageToAll("initiate" + " " + myIP + ":" + myPort);
	}

	private static void sendMessageToAll(String message) {
		for (String serverInfo : serverMap.values()) {

			sendMessage(message, serverInfo);
		}
	}

	private static void sendMessage(String message, String serverInfo) {
		System.out.println("Sending Msg >> " + message + " serverInfo " + serverInfo);
		String hostIP = serverInfo.split(":")[0];
		String port = serverInfo.split(":")[1];
		try {
			InetAddress address = InetAddress.getByName(hostIP);
			Socket socket = new Socket(address, Integer.parseInt(port));
			PrintWriter outToServer = new PrintWriter(socket.getOutputStream(), true);
			outToServer.println(message);
			outToServer.flush();

			socket.close();
		} catch (Exception e) {
			System.out.println("Server at " + hostIP + ":" + port.toString() + " ERROR:\t" + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void setupEnvironment() {
		String fileName = InputPath+"/E.txt";
		System.out.println(fileName);
		Input input = new Input(fileName);
		String[] rows = input.read();
		for (int i = 0; i < rows.length; i++) {
			if (i == 0) {
				String[] myDetails = rows[i].split(":");
				myIP = myDetails[0];
				myPort = Integer.parseInt(myDetails[1]);
			} else if (i == 1) {
				numberOfServers = Integer.parseInt(rows[i]);
			} else {
				serverMap.put((i - 1), rows[i]);
			}
		}

		serverMatrixArray = new int[numberOfServers][numberOfServers];
		terminationCount = new ArrayList<>();

		createServerConnectionMatrix();

	}

	private static void createServerConnectionMatrix() {
		Integer[] serverIDs = serverMap.keySet().toArray(new Integer[numberOfServers]);
		String fileName = "";
		for (Integer id : serverIDs) {
			fileName = InputPath + "/" + id + ".txt";
			System.out.println(fileName);
			Input input = new Input(fileName);
			String[] rows = input.read();
			for (int i = 2; i < rows.length; i++) {
				Integer neighbor = Integer.valueOf(rows[i].split(" ")[0]);
				serverMatrixArray[neighbor - 1][id - 1] = 1;
				serverMatrixArray[id - 1][neighbor - 1] = 1;
				
				int id1 = neighbor - 1;
				int id2 = id - 1;
				String edge = id1 +","+id2;
				edges.add(edge);

			}
		}
		printServerMatrix();
		writeToFile();

	}

	private static void printServerMatrix() {
		String[] rows = new String[numberOfServers];
		for (int i = 0; i < numberOfServers; i++) {
			String rowValue = "";
			for (int j = 0; j < numberOfServers; j++) {
				if (rowValue.equals("")) {
					rowValue = serverMatrixArray[i][j] + "";
				} else {
					rowValue = rowValue + ", " + serverMatrixArray[i][j];
				}

			}
			System.out.println(rowValue);
			rows[i] = rowValue;
		}

		System.out.println("=================================================");
		//writeToFile(rows);

	}

	public static void startUp() {

		System.out.println("Startup begin");

		new Thread() {
			public void run() {
				try {
					ServerSocket serverSocket = new ServerSocket(myPort);

					Socket socket;
					while ((socket = serverSocket.accept()) != null) {

						PrintWriter output = new PrintWriter(socket.getOutputStream());
						Scanner received = new Scanner(socket.getInputStream());
						processResponse(received);

						output.flush();
						output.close();
						socket.close();
						received.close();
					}
					serverSocket.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();

		System.out.println("Startup ends");
	}

	protected synchronized static void processResponse(Scanner received) {
		Integer node = Integer.parseInt(received.next());
		String values = received.next();
		if (values.equals("termination")) {
			System.out.println("Termination received from node >>" + node);
			terminationCount.add(node);
			if (terminationCount.size() == numberOfServers) {
				System.out.println("Termination detected.");
			}
		} else {
			System.out.println("Node:" + node + " Values:" + values);
			neighborMap.put(node, values);
			if (neighborMap.keySet().size() == serverMap.keySet().size()) {
				serverMatrixArray = new int[numberOfServers][numberOfServers];
				round++;
				edges = new ArrayList<>();
				for (int i = 0; i < numberOfServers; i++) {
					String[] neighbors = neighborMap.get(i + 1).split(":");
					for (int k = 0; k < neighbors.length; k++) {
						int neighborVal = Integer.parseInt(neighbors[k]);
						serverMatrixArray[i][neighborVal - 1] = 1;
						
						
						int id1 = i;
						int id2 = neighborVal-1;
						String edge = id1 +","+id2;
						edges.add(edge);

					}

				}

				neighborMap = new HashMap<>();
				printServerMatrix();
				writeToFile();
			}
		}
	}

	public static void writeToFile() {
		try {
			String path = OutputPath;
			String fileName = path + "/output"+round+".txt";
			System.out.println(fileName);
			BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, false));
			for (int i=0; i<edges.size(); i++) {
				writer.append(edges.get(i));
				if(i < edges.size()-1 ) {
					writer.append("\n");
				}				
			}
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
