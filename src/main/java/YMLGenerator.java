import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YMLGenerator {
    private static final String pathToYAMLFile = "/home/ozcankaraca/Desktop/testbed/src/main/java/containerlab-topology.yml";
    private HashMap<String, Set<String>> superPeerToPeersMap = new HashMap<>();
    private Map<String, Integer> interfaceCounter = new HashMap<>();
    private static List<String> linkInformation = new ArrayList<>();

    private static int subnetCounter = 21;
    private static Map<String, String> lectureStudioServerEnvVariables = new HashMap<>();
    private static Map<String, List<String>> superPeerEnvVariables = new HashMap<>();
    private static Map<String, String> peerEnvVariables = new HashMap<>();

    public YMLGenerator(String configFilePath) throws IOException {
        readAndProcessOutputFile(); // Diese Methode wird beim Instanziieren der Klasse aufgerufen
    }

    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {

        Set<String> allPeers = new HashSet<>();
        Set<String> superpeerNames = superPeerToPeersMap.keySet();

        for (String superPeer : superPeerToPeersMap.keySet()) {
            allPeers.add(superPeer);
            allPeers.addAll(superPeerToPeersMap.get(superPeer));
        }

        try (FileWriter fw = new FileWriter(pathToYAMLFile)) {
            fw.write("name: containerlab-topology\n");
            fw.write("prefix: p2p\n\n");
            fw.write("mgmt:\n");
            fw.write("  network: fixedips\n");
            fw.write("topology:\n");
            fw.write("  nodes:\n");

            // Definieren des lectureStudioServer-Knotens
            fw.write("    lectureStudioServer:\n");
            fw.write("      kind: linux\n");
            fw.write("      image: image-testbed\n");
            fw.write("      labels:\n");
            fw.write("        role: sender\n");
            fw.write("        group: server\n");
            fw.write("      binds:\n");
            fw.write("        - /home/ozcankaraca/Desktop/mydocument.pdf:/app/mydocument.pdf\n");
            fw.write("      env:\n");

            Set<String> lectureStudioPeers = superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>());
            fw.write("        SOURCE_PEER: lectureStudioServer\n");
            fw.write("        TARGET_PEERS: " + String.join(",", lectureStudioPeers) + "\n");
            for (Map.Entry<String, String> entry : lectureStudioServerEnvVariables.entrySet()) {
                fw.write("        " + entry.getKey() + ": " + entry.getValue() + "\n");
            }
            fw.write("        MAIN_CLASS: LectureStudioServer\n");
            fw.write("      ports:\n");
            fw.write("        - \"8080:8080\"\n\n");

            for (String peerName : allPeers) {
                if (peerName.equals("lectureStudioServer")) {
                    continue; // Überspringen der Konfiguration für den lecturestudioserver
                }
                String superPeer = determineSuperPeerForPeer(peerName);
                String mainClass = superpeerNames.contains(peerName) ? "SuperPeer" : "Peer";
                String role = superpeerNames.contains(peerName) ? "receiver/sender" : "receiver";

                fw.write("    " + peerName + ":\n");
                fw.write("      kind: linux\n");
                fw.write("      image: image-testbed\n");
                fw.write("      env:\n");
                
                    for (Map.Entry<String, String> entry : peerEnvVariables.entrySet()) {
                        if (entry.getKey().equals(peerName)) {
                        fw.write("        IP_ADDRES: " + entry.getValue() + "\n");
                    }
                }

                fw.write("        SUPER_PEER: " + superPeer + "\n");
                if (mainClass.equals("SuperPeer")) {
                    Set<String> targetPeers = superPeerToPeersMap.getOrDefault(peerName, new HashSet<>());
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                    fw.write("        TARGET_PEERS: " + String.join(",", targetPeers) + "\n");
                }
                if (superPeerEnvVariables.containsKey(peerName)) {
                    List<String> envVars = superPeerEnvVariables.get(peerName);
                    for (String envVar : envVars) {
                        fw.write("        " + envVar + "\n");
                    }
                }

                fw.write("        MAIN_CLASS: " + mainClass + "\n");
                fw.write("      labels:\n");
                fw.write("        role: " + role + "\n");
                fw.write("        group: " + (mainClass.equals("SuperPeer") ? "superpeer" : "peer") + "\n\n");
            }

            fw.write("  links:\n");
            for (String link : linkInformation) {
                fw.write("    - endpoints: [" + link + "]\n");
            }

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while generating the topology YAML file.");
        }
    }
    // Methode zur IP-Generierung

    private static String generateIpAddress(int connectionCounter, boolean isFirstNode) {
        String baseIp = "172.20." + (subnetCounter + connectionCounter - 1) + ".";
        int lastOctet = isFirstNode ? 2 : 3; // .2 für den ersten Knoten, .3 für den zweiten Knoten
        return baseIp + lastOctet;
    }

    public static void processLinkInformation() {
        int connectionCounter = 1;
        for (String link : linkInformation) {
            String[] endpoints = link.split(", ");
            String[] node1Details = endpoints[0].split(":");
            String[] node2Details = endpoints[1].split(":");

            System.out.println(node2Details[0]);
            String node1Ip = generateIpAddress(connectionCounter, true);
            String node2Ip = generateIpAddress(connectionCounter, false);

            String envVariableValue = node1Details[1] + ";" + node1Ip + ", " + node2Ip;
            peerEnvVariables.put(node2Details[0], node2Ip);

            if (node1Details[0].equals("lectureStudioServer")) {
                lectureStudioServerEnvVariables.put("CONNECTION_" + connectionCounter, envVariableValue);
            } else {
                superPeerEnvVariables.computeIfAbsent(node1Details[0], k -> new ArrayList<>())
                        .add("CONNECTION_" + connectionCounter + ":" + " " + envVariableValue);
            }
            connectionCounter++;
        }

        // printEnvironmentVariables();
    }

    private static void printEnvironmentVariables() {
        System.out.println("lectureStudioServer Environment Variables:");
        for (Map.Entry<String, String> entry : lectureStudioServerEnvVariables.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        System.out.println("\nSuperPeer Environment Variables:");
        for (Map.Entry<String, List<String>> entry : superPeerEnvVariables.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    private String assignInterface(String nodeName) {
        int count = interfaceCounter.getOrDefault(nodeName, 1);
        interfaceCounter.put(nodeName, count + 1);
        return "eth" + count;
    }

    public static void printLinkInformation() {
        System.out.println("Link Information:");
        for (String link : linkInformation) {
            System.out.println(link);
        }
    }

    private String determineSuperPeerForPeer(String peerName) {
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            if (entry.getValue().contains(peerName)) {
                return entry.getKey();
            }
        }

        return "lecturestudioserver";
    }

    private void readAndProcessOutputFile() {
        ObjectMapper mapper = new ObjectMapper();
        String pathToOutputFile = "/home/ozcankaraca/Desktop/testbed/output.json";

        try {
            JsonNode rootNode = mapper.readTree(new File(pathToOutputFile));
            JsonNode peer2peerNode = rootNode.path("peer2peer");

            for (JsonNode connection : peer2peerNode) {
                String sourceName = connection.path("sourceName").asText();
                String targetName = connection.path("targetName").asText();

                superPeerToPeersMap.putIfAbsent(sourceName, new HashSet<>());
                superPeerToPeersMap.get(sourceName).add(targetName);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateLinkInformation() {
        Set<String> uniqueConnections = new HashSet<>();

        // Generieren der Links für lectureStudioServer zu seinen Peers
        Set<String> lectureStudioPeers = superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>());
        for (String peer : lectureStudioPeers) {
            if (!uniqueConnections.contains(peer)) {
                String lectureStudioInterface = assignInterface("lectureStudioServer");
                String peerInterface = assignInterface(peer);
                String linkInfo = "lectureStudioServer:" + lectureStudioInterface + ", " + peer + ":" + peerInterface;
                linkInformation.add(linkInfo);
                uniqueConnections.add(peer);
            }
        }

        // Generieren der Links für SuperPeers zu ihren verbundenen Peers
        for (String superPeer : superPeerToPeersMap.keySet()) {
            Set<String> connectedPeers = superPeerToPeersMap.getOrDefault(superPeer, new HashSet<>());
            for (String peer : connectedPeers) {
                if (!uniqueConnections.contains(peer)) {
                    String superPeerInterface = assignInterface(superPeer);
                    String peerInterface = assignInterface(peer);
                    String linkInfo = superPeer + ":" + superPeerInterface + ", " + peer + ":" + peerInterface;
                    linkInformation.add(linkInfo);
                    uniqueConnections.add(peer);
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            String pathToOutputData = "/home/ozcankaraca/Desktop/testbed/src/resources/data/output-data.json";
            YMLGenerator generator = new YMLGenerator(pathToOutputData);
            boolean includeExtraNodes = true; // Diese Variable sollte basierend auf Ihren Kriterien festgelegt werden.

            // Generiert die Link-Informationen
            generator.generateLinkInformation();

            // Verarbeitet die Link-Informationen und füllt die Umgebungsvariablen-Maps
            processLinkInformation();

            // Generiert die Topologie-Datei basierend auf den gesammelten Informationen
            generator.generateTopologyFile(includeExtraNodes);

            System.out.println("YML topology file generated successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred.");
        }
    }

}