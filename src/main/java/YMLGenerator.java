import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class YMLGenerator {

    private NetworkConfigParser networkConfigParser;
    private static final String pathToYAMLFile = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/data/containerlab-topology.yml";

    public YMLGenerator(String configFilePath) throws IOException {
        this.networkConfigParser = new NetworkConfigParser(configFilePath);
    }

    public void generateTopologyFile(boolean includeExtraNodes) throws IOException {
            List<String> superpeerNames = networkConfigParser.getSuperpeerNames();
            List<NetworkConfigParser.PeerConnection> peerConnections = networkConfigParser.getPeerConnections();
            Map<String, List<String>> superpeerConnections = networkConfigParser.getSuperpeerConnections();
        
            try (FileWriter fw = new FileWriter(pathToYAMLFile)) {
                fw.write("name: containerlab-topology\n");
                fw.write("prefix: p2p\n\n");
                fw.write("topology:\n");
                fw.write("  nodes:\n");
        
                // Definieren Sie den lectureStudioServer-Knoten mit Umgebungsvariablen für Ziel-Peers
                fw.write("    lecturestudioserver:\n");
                fw.write("      kind: linux\n");
                fw.write("      image: image-lecturestudioserver\n");
                fw.write("      labels:\n");
                fw.write("        role: sender\n");
                fw.write("        group: server\n");
                fw.write("      binds:\n");
                fw.write("        - /home/ozcankaraca/Desktop/mydocument.pdf:/app/mydocument.pdf\n");
                fw.write("      env:\n");
                // Hier fügen wir die Ziel-Peers als Umgebungsvariable hinzu
                fw.write("        TARGET_PEERS: " + getTargetsForLectureStudioServer(peerConnections) + "\n");
                fw.write("      exec:\n");
                fw.write("        - sleep 15\n");
                fw.write("      cmd: java -cp /app LectureStudioServer\n\n");

            // Definieren Sie die SuperPeer-Knoten
            for (String superpeerName : superpeerNames) {
                List<String> connectedPeers = superpeerConnections.get(superpeerName);
                String peersAsEnvVar = String.join(",", connectedPeers);

                fw.write("    " + superpeerName + ":\n");
                fw.write("      kind: linux\n");
                fw.write("      image: image-superpeer\n");
                fw.write("      env:\n");
                fw.write("        TARGET_PEERS: " + peersAsEnvVar + "\n");
                fw.write("      labels:\n");
                fw.write("        role: relaysender\n");
                fw.write("        group: superpeer\n");
                fw.write("      cmd: java -cp /app SuperPeer\n\n");
            }

            // Definieren Sie die Peer-Knoten und weisen Sie jedem Peer seinen SuperPeer zu
            peerConnections.stream()
                    .forEach(peerConnection -> {
                        String peerName = peerConnection.getTargetName();
                        String superpeerName = peerConnection.getSourceName(); // Dies setzt voraus, dass die Quelle
                                                                               // immer der SuperPeer ist

                        if (!superpeerNames.contains(peerName) && !peerName.equals("lectureStudioServer")) {
                            try {
                                fw.write("    " + peerName + ":\n");
                                fw.write("      kind: linux\n");
                                fw.write("      image: image-peer\n");
                                fw.write("      env:\n");
                                fw.write("        SUPER_PEER: " + superpeerName + "\n"); // Setzen der Umgebungsvariable
                                                                                         // SUPER_PEER
                                fw.write("      labels:\n");
                                fw.write("        role: receiver\n");
                                fw.write("        group: peer\n");
                                fw.write("      cmd: java -cp /app Peer\n\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

            if (!includeExtraNodes) {
                appendExtraNodes(fw);
            }
            /**
             * // Verbindungen zwischen lectureStudioServer und SuperPeers
             * fw.write("\n links:\n");
             * for (NetworkConfigParser.PeerConnection connection : peerConnections) {
             * if (connection.getSourceName().equals("lectureStudioServer")
             * && superpeerNames.contains(connection.getTargetName())) {
             * fw.write(" - endpoints: [\"" + connection.getSourceName() + ":eth1\", \""
             * + connection.getTargetName() + ":eth1\"]\n");
             * }
             * }
             */
            /**
             * // Verbindungen zwischen SuperPeers und Peers
             * for (Map.Entry<String, List<String>> entry : superpeerConnections.entrySet())
             * {
             * for (String peer : entry.getValue()) {
             * fw.write(" - endpoints: [\"" + entry.getKey() + ":eth1\", \"" + peer +
             * ":eth1\"]\n");
             * }
             * }
             */
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("An error occurred while generating the topology YAML file.");
        }
    }

    private String getTargetsForLectureStudioServer(List<NetworkConfigParser.PeerConnection> peerConnections) {
    return peerConnections.stream()
            .filter(connection -> connection.getSourceName().equals("lectureStudioServer"))
            .map(NetworkConfigParser.PeerConnection::getTargetName)
            .collect(Collectors.joining(","));
}

    /**
     * Appends the configurations for additional tools (Prometheus, Cadvisor, and
     * Grafana) to the topology YML.
     *
     * @param fw FileWriter instance used to write configurations to the YML file.
     * @throws IOException If any IO operation fails.
     */
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
            String pathToOutputData = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/data/output-data.json";
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
