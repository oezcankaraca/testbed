import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class YMLGenerator {
    private static final String pathToYAMLFile = "/home/ozcankaraca/Desktop/testbed/src/resources/data/containerlab-topology.yml";
    private static final String subnet = "172.100.100.";
    private int nextIp = 11; // Startet nach der festgelegten IP des lecturestudioservers
    private HashMap<String, Set<String>> superPeerToPeersMap = new HashMap<>();

    public YMLGenerator(String configFilePath) throws IOException {
        readAndProcessOutputFile(); // Diese Methode wird beim Instanziieren der Klasse aufgerufen
    }

    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {
        Set<String> targetPeersForLectureStudioServer = new HashSet<>();
        Set<String> allPeers = new HashSet<>();
        Set<String> superpeerNames = superPeerToPeersMap.keySet();
    
        // Sammeln Sie alle Peers und die Zielpeers für den lectureStudioServer
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            if ("lectureStudioServer".equals(entry.getKey())) {
                targetPeersForLectureStudioServer.addAll(entry.getValue());
            }
            allPeers.addAll(entry.getValue());
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
            fw.write("    lecturestudioserver:\n");
            fw.write("      kind: linux\n");
            fw.write("      image: image-testbed\n");
            fw.write("      mgmt-ipv4: 172.100.100.10\n");
            fw.write("      labels:\n");
            fw.write("        role: sender\n");
            fw.write("        group: server\n");
            fw.write("      binds:\n");
            fw.write("        - /home/ozcankaraca/Desktop/mydocument.pdf:/app/mydocument.pdf\n");
            fw.write("      env:\n");
            fw.write("        TARGET_PEERS: " + String.join(",", targetPeersForLectureStudioServer) + "\n");
            fw.write("        MAIN_CLASS: LectureStudioServer\n");
            fw.write("      ports:\n");
            fw.write("        - \"8080:8080\"\n\n");
    
            // Definieren der restlichen Peers
            for (String peerName : allPeers) {
                if (!"lectureStudioServer".equals(peerName)) {
                    String superPeer = determineSuperPeerForPeer(peerName);
                    String mainClass = superpeerNames.contains(peerName) ? "SuperPeer" : "Peer";
                    String role = superpeerNames.contains(peerName) ? "receiver/sender" : "receiver";
    
                    fw.write("    " + peerName + ":\n");
                    fw.write("      kind: linux\n");
                    fw.write("      image: image-testbed\n");
                    fw.write("      mgmt-ipv4: " + generateRandomIP() + "\n");
                    fw.write("      env:\n");
                    fw.write("        SUPER_PEER: " + superPeer + "\n");
                    fw.write("        MAIN_CLASS: " + mainClass + "\n");
                    fw.write("      labels:\n");
                    fw.write("        role: " + role + "\n");
                    fw.write("        group: " + (mainClass.equals("SuperPeer") ? "superpeer" : "peer") + "\n");
                }
            }
    
            if (!includeExtraNodes) {
                appendExtraNodes(fw);
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while generating the topology YAML file.");
        }
    }    

    private String determineSuperPeerForPeer(String peerName) {
        for (Map.Entry<String, Set<String>> entry : superPeerToPeersMap.entrySet()) {
            // Wenn der aktuelle Peer in der Liste der verbundenen Peers eines Superpeers enthalten ist,
            // wird dieser Superpeer als SUPER_PEER des Peers zurückgegeben.
            if (entry.getValue().contains(peerName)) {
                return entry.getKey();
            }
        }
        // Wenn der Peer in keiner der Listen enthalten ist, ist der Standard-SuperPeer der lecturestudioserver.
        return "lecturestudioserver";
    }
    

    private String generateRandomIP() {
        String ip = subnet + nextIp;
        nextIp++;
        return ip;
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
