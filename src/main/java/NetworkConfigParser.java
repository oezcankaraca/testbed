import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

public class NetworkConfigParser {

    static class PeerConnection {
        private String targetName;
        private String sourceName;

        // Getter und Setter (wenn Setter benötigt werden)
        public String getTargetName() {
            return targetName;
        }

        public String getSourceName() {
            return sourceName;
        }
        
        // Jackson benötigt einen leeren Konstruktor
        public PeerConnection() {
        }

        // JSON deserialisierender Konstruktor
        public PeerConnection(String sourceName, String targetName) {
            this.sourceName = sourceName;
            this.targetName = targetName;
        }
    }

    static class Superpeer {
        private String name;

        // Getter und Setter (wenn Setter benötigt werden)
        public String getName() {
            return name;
        }
        
        // Jackson benötigt einen leeren Konstruktor
        public Superpeer() {
        }

        // JSON deserialisierender Konstruktor
        public Superpeer(String name) {
            this.name = name;
        }
    }

    static class NetworkConfig {
        private List<PeerConnection> peer2peer;
        private List<Superpeer> superpeers;

        // Getter und Setter (wenn Setter benötigt werden)
        public List<PeerConnection> getPeer2peer() {
            return peer2peer;
        }

        public List<Superpeer> getSuperpeers() {
            return superpeers;
        }
    }

    private NetworkConfig config;

    public NetworkConfigParser(String configFilePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        config = mapper.readValue(new File(configFilePath), new TypeReference<NetworkConfig>() {});
    }

    public List<String> getSuperpeerNames() {
        return config.getSuperpeers().stream()
                .map(Superpeer::getName)
                .collect(Collectors.toList());
    }

    public List<PeerConnection> getPeerConnections() {
        return config.getPeer2peer();
    }

    public Map<String, List<String>> getSuperpeerConnections() {
        Map<String, List<String>> superpeerConnections = new HashMap<>();
        for (Superpeer superpeer : config.getSuperpeers()) {
            List<String> connectedPeers = config.getPeer2peer().stream()
                    .filter(connection -> superpeer.getName().equals(connection.getSourceName()))
                    .map(PeerConnection::getTargetName)
                    .collect(Collectors.toList());
            superpeerConnections.put(superpeer.getName(), connectedPeers);
        }
        return superpeerConnections;
    }

    // Utility-Funktion, um alle Peers (außer Superpeers und lectureStudioServer) zu erhalten
    public List<String> getPeers() {
        List<String> superpeerNames = getSuperpeerNames();
        return config.getPeer2peer().stream()
                .map(PeerConnection::getTargetName)
                .filter(peerName -> !superpeerNames.contains(peerName) && !"lectureStudioServer".equals(peerName))
                .distinct()
                .collect(Collectors.toList());
    }

    // Hauptmethode für Konsolenausgaben
    public static void main(String[] args) throws IOException {
        String pathToOutputData = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/data/output-data.json";
        NetworkConfigParser parser = new NetworkConfigParser(pathToOutputData);
        
        // Informationen über Superpeers sammeln und ausgeben
        List<String> superpeerNames = parser.getSuperpeerNames();
        System.out.println("Superpeers:");
        superpeerNames.forEach(System.out::println);

        // Verbindungen vom lectureStudioServer zu den Superpeers finden und ausgeben
        List<PeerConnection> connectionsFromServer = parser.getPeerConnections().stream()
                .filter(connection -> "lectureStudioServer".equals(connection.getSourceName()))
                .collect(Collectors.toList());
        System.out.println("\nVerbindungen vom lectureStudioServer zu Superpeers:");
        connectionsFromServer.forEach(connection -> System.out.println("lectureStudioServer -> " + connection.getTargetName()));

        // Verbindungen zwischen Superpeers und Peers finden und ausgeben
        Map<String, List<String>> superpeerConnections = parser.getSuperpeerConnections();
        System.out.println("\nVerbindungen von Superpeers zu Peers:");
        superpeerConnections.forEach((superpeer, peers) -> System.out.println(superpeer + " -> " + peers));
    }
}
