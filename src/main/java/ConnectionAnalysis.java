import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConnectionAnalysis {

    private static Set<String> checkFullMesh(Set<String> allConnections, Set<String> peerNames) {
        Set<String> missingConnections = new HashSet<>();
        for (String source : peerNames) {
            for (String target : peerNames) {
                if (!source.equals(target)) {
                    String connectionKey = source + "-" + target;
                    if (!allConnections.contains(connectionKey)) {
                        missingConnections.add(connectionKey);
                    }
                }
            }
        }
        return missingConnections;
    }

    public static void main(String[] args) throws IOException {
        String pathToInputData = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/data/input-data.json"; // Replace with the actual path to your JSON file
        PrintStream pathToInputInfo = new PrintStream("/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/results/input-info.txt");
        System.setOut(pathToInputInfo);

        String content = new String(Files.readAllBytes(Paths.get(pathToInputData)));
        JSONObject json = new JSONObject(content);

        JSONArray peers = json.getJSONArray("peers");
        JSONArray connections = json.getJSONArray("connections");

        Map<String, Long> uploadBandwidthMap = new HashMap<>();
        Map<String, Long> downloadBandwidthMap = new HashMap<>();

        // Populate the bandwidth maps
        for (int i = 0; i < peers.length(); i++) {
            JSONObject peer = peers.getJSONObject(i);
            String name = peer.getString("name");
            long maxDownload = peer.getLong("maxDownload");
            long maxUpload = peer.getLong("maxUpload");
            downloadBandwidthMap.put(name, maxDownload);
            uploadBandwidthMap.put(name, maxUpload);
        }

        StringBuilder connectionDetails = new StringBuilder();
        Set<String> allConnections = new HashSet<>();
        Set<String> peerNames = new HashSet<>();

        for (int i = 0; i < connections.length(); i++) {
            JSONObject connection = connections.getJSONObject(i);
            String source = connection.getString("sourceName");
            String target = connection.getString("targetName");
            double latency = connection.getDouble("latency");
            double loss = connection.getDouble("loss");

            peerNames.add(source);
            peerNames.add(target);

            // Get the upload bandwidth of the source and download bandwidth of the target
            long sourceUploadBandwidth = uploadBandwidthMap.getOrDefault(source, 0L);
            long targetDownloadBandwidth = downloadBandwidthMap.getOrDefault(target, 0L);

            // The actual bandwidth used for the connection is the minimum of source upload and target download
            long usedBandwidth = Math.min(sourceUploadBandwidth, targetDownloadBandwidth);

            connectionDetails.append(String.format(
                    "Source: %s (maxUpload: %d Kbps) - Target: %s (maxDownload: %d Kbps) - Bandwidth: %d Kbps - Latency: %.2f ms - Loss: %.2f%%\n",
                    source, sourceUploadBandwidth, target, targetDownloadBandwidth, usedBandwidth, latency, loss));

            allConnections.add(source + "-" + target);
        }

        System.out.println("-------Connection details-------");
        System.out.println(connectionDetails.toString());

        Set<String> missingConnections = checkFullMesh(allConnections, peerNames);

        if (missingConnections.isEmpty()) {
            System.out.println("There is a full mesh among all peers including the lectureStudioServer.");
        } else {
            System.out.println("There is NOT a full mesh among all peers including the lectureStudioServer.");
            System.out.println("Missing connections:");
            missingConnections.forEach(System.out::println);
        }

        pathToInputInfo.close();
    }
}