import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YMLGenerator {
    private static final String pathToYAMLFile = "/home/ozcankaraca/Desktop/testbed/src/main/java/containerlab-topology.yml";
    private static final String subnet = "172.100.100.";
    private int nextIp = 11;
    private HashMap<String, Set<String>> superPeerToPeersMap = new HashMap<>();
    private Map<String, String> peerNameToIpMap = new HashMap<>();
    private Map<String, Integer> interfaceCounter = new HashMap<>();

    public YMLGenerator(String configFilePath) throws IOException {
        readAndProcessOutputFile();
    }

    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {
        Set<String> allPeers = new HashSet<>();
        Set<String> superpeerNames = superPeerToPeersMap.keySet();

        // Hinzufügen aller Peers (einschließlich SuperPeers) zu allPeers
        for (String superPeer : superPeerToPeersMap.keySet()) {
            allPeers.add(superPeer);
            allPeers.addAll(superPeerToPeersMap.get(superPeer));
        }

        // Zuweisung von IP-Adressen für alle Peers außer lectureStudioServer
        for (String peerName : allPeers) {
            if (!peerName.equals("lectureStudioServer")) {
                peerNameToIpMap.put(peerName, generateNextIP());
            }
        }

        try (FileWriter fw = new FileWriter(pathToYAMLFile)) {
            fw.write("name: containerlab-topology\n");
            fw.write("prefix: p2p\n\n");
            fw.write("mgmt:\n");
            fw.write("  network: fixedips\n");
            fw.write("  ipv4-subnet: " + subnet + "0/24\n\n");
            fw.write("topology:\n");
            fw.write("  nodes:\n");

            // Definieren des lectureStudioServer-Knotens
            fw.write("    lectureStudioServer:\n");
            fw.write("      kind: linux\n");
            fw.write("      image: image-testbed\n");
            fw.write("      mgmt-ipv4: 172.100.100.10\n");
            fw.write("      labels:\n");
            fw.write("        role: sender\n");
            fw.write("        group: server\n");
            fw.write("      binds:\n");
            fw.write("        - /home/ozcankaraca/Desktop/mydocument.pdf:/app/mydocument.pdf\n");
            fw.write(
                    "        - /home/ozcankaraca/Desktop/testbed/src/resources/results/connection-details.json:/app/connection-details.json\n");
            fw.write(
                    "        - /home/ozcankaraca/Desktop/testbed/src/resources/skripts/connections-source.sh:/app/connections-source.sh\n");
            fw.write("      exec:\n");
            fw.write("        - echo \"Waiting for 5 seconds...\"\n");
            fw.write("        - sleep 5\n");
            fw.write("        - chmod +x /app/connections-source.sh\n");
            fw.write("        - ./connections-source.sh\n");
            fw.write("      env:\n");

            String targetPeers = String.join(",",
                    superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>()));
            String targetPeersIpsLecture = getTargetPeersIps(superPeerToPeersMap.get("lectureStudioServer"));
            fw.write("        SOURCE_PEER: lectureStudioServer\n");
            fw.write("        TARGET_PEERS: " + targetPeers + "\n");
            fw.write("        TARGET_PEERS_IP: " + targetPeersIpsLecture + "\n");
            fw.write("        MAIN_CLASS: LectureStudioServer\n");
            fw.write("      ports:\n");
            fw.write("        - \"8080:8080\"\n\n");

            // Erstellen von Knoten für die anderen Peers
            for (String peerName : allPeers) {
                if (peerName.equals("lectureStudioServer")) {
                    continue;
                }

                boolean isNormalPeer = !superpeerNames.contains(peerName);
                String mainClass = isNormalPeer ? "Peer" : "SuperPeer";
                String role = isNormalPeer ? "receiver" : "receiver/sender";

                fw.write("    " + peerName + ":\n");
                fw.write("      kind: linux\n");
                fw.write("      image: image-testbed\n");
                fw.write("      mgmt-ipv4: " + peerNameToIpMap.get(peerName) + "\n");
                fw.write("      labels:\n");
                fw.write("        role: " + role + "\n");
                fw.write("        group: " + mainClass.toLowerCase() + "\n");

                // Umgekehrte Logik für normale Peers
                if (isNormalPeer) {
                    Set<String> connectedTargets = findAllConnectedSuperPeersAndLectureStudioServer(peerName);
                    String targetPeersString = String.join(",", connectedTargets);
                    String targetPeersIps = getTargetPeersIps(connectedTargets, true); // Anpassung hier

                    fw.write("      env:\n");
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                    fw.write("        TARGET_PEERS: " + targetPeersString + "\n");
                    fw.write("        TARGET_PEERS_IP: " + targetPeersIps + "\n");
                } else {
                    // Umgebungsvariablen für SuperPeers
                    targetPeers = String.join(",", superPeerToPeersMap.getOrDefault(peerName, new HashSet<>()));
                    String targetPeersIpsSuperPeer = getTargetPeersIps(superPeerToPeersMap.get(peerName), false);

                    fw.write("      env:\n");
                    fw.write("        SUPER_PEER: " + "lectureStudioServer" + "\n");
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                    fw.write("        TARGET_PEERS: " + targetPeers + "\n");
                    fw.write("        TARGET_PEERS_IP: " + targetPeersIpsSuperPeer + "\n");
                }

                // Binds und Exec für alle Knoten
                appendBindsAndExec(fw, isNormalPeer);
            }

            if (!includeExtraNodes) {
                appendExtraNodes(fw);
            }

            // [Code zum Schreiben des Anfangs der YAML-Datei und der Peer-Knoten]

            fw.write("  links:\n");

            // Eindeutige Verbindung für jeden Peer in TARGET_PEERS des lectureStudioServer
            Set<String> uniqueConnections = new HashSet<>();

            // Erzeugen der Links für lectureStudioServer zu seinen Peers
            Set<String> lectureStudioPeers = superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>());
            for (String peer : lectureStudioPeers) {
                if (!uniqueConnections.contains(peer)) {
                    String lectureStudioInterface = assignInterface("lectureStudioServer");
                    String peerInterface = assignInterface(peer);
                    fw.write("    - endpoints: [\"lectureStudioServer:" + lectureStudioInterface + "\", \"" + peer + ":"
                            + peerInterface + "\"]\n");
                    uniqueConnections.add(peer);
                }
            }

            // Erzeugen der Links für SuperPeers zu ihren verbundenen Peers
            for (String superPeer : superpeerNames) {
                Set<String> connectedPeers = superPeerToPeersMap.getOrDefault(superPeer, new HashSet<>());
                for (String peer : connectedPeers) {
                    if (!uniqueConnections.contains(peer)) {
                        String superPeerInterface = assignInterface(superPeer);
                        String peerInterface = assignInterface(peer);
                        fw.write("    - endpoints: [\"" + superPeer + ":" + superPeerInterface + "\", \"" + peer + ":"
                                + peerInterface + "\"]\n");
                        uniqueConnections.add(peer);
                    }
                }
            }

            // [Weiterer Code zum Hinzufügen von zusätzlichen Knoten, falls erforderlich]

            // Schließen der FileWriter-Instanz
            fw.close();

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while generating the topology YAML file.");
        }
    }

    private String assignInterface(String nodeName) {
        int count = interfaceCounter.getOrDefault(nodeName, 1);
        interfaceCounter.put(nodeName, count + 1);
        return "eth" + count;
    }

    private String getTargetPeersIps(Set<String> targetPeers, boolean isNormalPeer) {
        return String.join(",",
                targetPeers.stream()
                        .map(peerName -> peerName.equals("lectureStudioServer") && isNormalPeer ? "172.100.100.10"
                                : peerNameToIpMap.getOrDefault(peerName, "unknown"))
                        .collect(Collectors.toList()));
    }

    private Set<String> findAllConnectedSuperPeersAndLectureStudioServer(String normalPeer) {
        Set<String> connectedPeers = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            if (entry.getValue().contains(normalPeer)) {
                connectedPeers.add(entry.getKey());
            }
        }
        if (superPeerToPeersMap.getOrDefault("lectureStudioServer", Collections.emptySet()).contains(normalPeer)) {
            connectedPeers.add("lectureStudioServer");
        }
        return connectedPeers;
    }

    private void appendBindsAndExec(FileWriter fw, boolean isSuperPeer) throws IOException {
        fw.write("      binds:\n");
        fw.write(
                "        - /home/ozcankaraca/Desktop/testbed/src/resources/results/connection-details.json:/app/connection-details.json\n");

        if (isSuperPeer) {
            fw.write(
                    "        - /home/ozcankaraca/Desktop/testbed/src/resources/skripts/connections-target.sh:/app/connections-target.sh\n");
            fw.write("      exec:\n");
            fw.write("        - echo \"Waiting for 5 seconds...\"\n");
            fw.write("        - sleep 5\n");
            fw.write("        - chmod +x /app/connections-target.sh\n");
            fw.write("        - ./connections-target.sh\n");
        } else {
            fw.write(
                    "        - /home/ozcankaraca/Desktop/testbed/src/resources/skripts/connections-source.sh:/app/connections-source.sh\n");
            fw.write("      exec:\n");
            fw.write("        - echo \"Waiting for 5 seconds...\"\n");
            fw.write("        - sleep 5\n");
            fw.write("        - chmod +x /app/connections-source.sh\n");
            fw.write("        - ./connections-source.sh\n");
        }

    }

    private String generateNextIP() {
        String ip = subnet + nextIp;
        nextIp++;
        return ip;
    }

    private String getTargetPeersIps(Set<String> targetPeers) {
        return String.join(",", targetPeers.stream().map(peerName -> peerNameToIpMap.getOrDefault(peerName, "unknown"))
                .collect(Collectors.toList()));
    }

    private void readAndProcessOutputFile() {
        ObjectMapper mapper = new ObjectMapper();
        String pathToOutputFile = "/home/ozcankaraca/Desktop/testbed/src/resources/data/output-data.json";

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

    private void appendExtraNodes(FileWriter fw) throws IOException {
        // Configuration for the prometheus tool
        fw.write("\n    prometheus:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: p2p-prometheus\n");
        fw.write("      binds:\n");
        fw.write(
                "       - /home/ozcankaraca/Desktop/p2pjava/src/resources/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml\n");
        fw.write("      ports:\n");
        fw.write("        - \"9090:9090\"\n");

        // Configuration for the cadvisor tool
        fw.write("\n    cadvisor:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: p2p-cadvisor\n");
        fw.write("      binds:\n");
        fw.write("        - /:/rootfs:ro\n");
        fw.write("        - /var/run:/var/run:ro\n");
        fw.write("        - /sys:/sys:ro\n");
        fw.write("        - /var/snap/docker/common/var-lib-docker/:/var/lib/docker:ro\n");
        fw.write("      ports:\n");
        fw.write("        - \"8080:8080\"\n");

        // Configuration for the grafana tool
        fw.write("\n    grafana:\n");
        fw.write("      kind: linux\n");
        fw.write("      image: p2p-grafana\n");
        fw.write("      ports:\n");
        fw.write("       - \"3000:3000\"\n");
    }

    public static void main(String[] args) {
        try {
            String pathToOutputData = "/home/ozcankaraca/Desktop/testbed/src/resources/data/output-data.json";
            YMLGenerator generator = new YMLGenerator(pathToOutputData);
            boolean includeExtraNodes = true; // Diese Variable sollte basierend auf Ihren Kriterien festgelegt werden.

            generator.generateTopologyFile(includeExtraNodes);
            System.out.println("YML topology file generated successfully.");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while initializing the YMLGenerator or NetworkConfigParser.");
        }
    }
}
