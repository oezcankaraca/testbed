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

    public YMLGenerator(String configFilePath) throws IOException {
        readAndProcessOutputFile();
    }

    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {
        Set<String> allPeers = new HashSet<>();
        Set<String> superpeerNames = superPeerToPeersMap.keySet();

        for (String superPeer : superPeerToPeersMap.keySet()) {
            allPeers.add(superPeer);
            allPeers.addAll(superPeerToPeersMap.get(superPeer));
        }

        for (String peerName : allPeers) {
            if (!peerName.equals("lectureStudioServer")) { // Überspringen des lecturestudioservers
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
            fw.write("      mgmt-ipv4: 172.100.100.10\n"); // Feste IP für den lecturestudioserver
            fw.write("      labels:\n");
            fw.write("        role: sender\n");
            fw.write("        group: server\n");
            fw.write("      binds:\n");
            fw.write("        - /home/ozcankaraca/Desktop/mydocument.pdf:/app/mydocument.pdf\n");
            fw.write("      env:\n");

            String targetPeers = String.join(",",
                    superPeerToPeersMap.getOrDefault("lectureStudioServer", new HashSet<>()));
            String targetPeersIps = getTargetPeersIps(superPeerToPeersMap.get("lectureStudioServer"));
            fw.write("        SOURCE_PEER: lectureStudioServer\n");
            fw.write("        TARGET_PEERS: " + targetPeers + "\n");
            fw.write("        TARGET_PEERS_IP: " + targetPeersIps + "\n");
            fw.write("        MAIN_CLASS: LectureStudioServer\n");
            fw.write("      ports:\n");
            fw.write("        - \"8080:8080\"\n\n");

            // Definieren der SuperPeers und Peers
            // Definieren der SuperPeers und Peers
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
                fw.write("      mgmt-ipv4: " + peerNameToIpMap.get(peerName) + "\n");
                fw.write("      env:\n");
                fw.write("        SUPER_PEER: " + superPeer + "\n");
                if (mainClass.equals("SuperPeer")) {
                    targetPeers = String.join(",", superPeerToPeersMap.getOrDefault(peerName, new HashSet<>()));
                    targetPeersIps = getTargetPeersIps(superPeerToPeersMap.get(peerName));
                    fw.write("        SOURCE_PEER: " + peerName + "\n");
                    fw.write("        TARGET_PEERS: " + targetPeers + "\n");
                    fw.write("        TARGET_PEERS_IP: " + targetPeersIps + "\n");
                }
                fw.write("        MAIN_CLASS: " + mainClass + "\n");
                fw.write("      labels:\n");
                fw.write("        role: " + role + "\n");
                fw.write("        group: " + (mainClass.equals("SuperPeer") ? "superpeer" : "peer") + "\n\n");
            }

            if (!includeExtraNodes) {
                appendExtraNodes(fw);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while generating the topology YAML file.");
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

    private String determineSuperPeerForPeer(String peerName) {
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            // Wenn der aktuelle Peer in der Liste der verbundenen Peers eines Superpeers
            // enthalten ist,
            // wird dieser Superpeer als SUPER_PEER des Peers zurückgegeben.
            if (entry.getValue().contains(peerName)) {
                return entry.getKey();
            }
        }
        return "lectureStudioServer";
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
