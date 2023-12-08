import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class NetworkStatisticsYML extends CSVReaderUtils {

    public static void main(String[] args) {
        String pathToJsonOutput = "/home/ozcankaraca/Desktop/testbed/src/resources/results/output-data.json";
        String pathToPeerInfoFile = "/home/ozcankaraca/Desktop/testbed/src/resources/results/output-info.json";
        String pathToCSV = "/home/ozcankaraca/Desktop/testbed/src/resources/data/fixed-broadband-speeds-august-2019-data-25.csv";
        String pathToNetworkStatistics = "/home/ozcankaraca/Desktop/testbed/src/resources/results/network-statistics.txt";
        int numberOfPeers = 50; 

        // Lesen Sie zuerst die CSV-Daten und schreiben Sie sie in eine Datei
        readCsvDataAndWriteToFile(pathToCSV, pathToNetworkStatistics, numberOfPeers);

        Set<String> peerIds = new HashSet<>();

        try {
            // Schritt 1: JSON-Daten lesen und Peer-IDs extrahieren
            JsonObject outputData = JsonParser.parseReader(new FileReader(pathToJsonOutput)).getAsJsonObject();
            extractPeerIdsFromJson(outputData, peerIds);

            // Schritt 2: Peer-Infos in eine Datei schreiben
            writePeerInfosToFile(peerIds, pathToPeerInfoFile);

            // Schritt 3: Schreiben der Verbindungseigenschaften in die Datei
            writeConnectionPropertiesToFile(outputData, pathToPeerInfoFile);

            System.out.println("Peer information has been saved to the file: " + pathToPeerInfoFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void extractPeerIdsFromJson(JsonObject outputData, Set<String> peerIds) {
        JsonArray superPeers = outputData.getAsJsonArray("superpeers");
        JsonArray peer2peer = outputData.getAsJsonArray("peer2peer");

        for (JsonElement peerElement : superPeers) {
            String peerId = peerElement.getAsJsonObject().get("name").getAsString();
            peerIds.add(peerId);
        }

        for (JsonElement connection : peer2peer) {
            JsonObject connectionObj = connection.getAsJsonObject();
            peerIds.add(connectionObj.get("sourceName").getAsString());
            peerIds.add(connectionObj.get("targetName").getAsString());
        }
    }

    private static void writePeerInfosToFile(Set<String> peerIds, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String peerId : peerIds) {
                PeerStats stats = getPeerStats(peerId);
                if (stats != null) {
                    writer.write(peerId + ": " + stats.toString() + "\n");
                } else {
                    writer.write("Keine Daten gefunden für Peer " + peerId + "\n");
                }
            }
        }
    }

    private static void writeConnectionPropertiesToFile(JsonObject outputData, String filePath) throws IOException {
    JsonArray connectionsArray = new JsonArray();

    JsonArray peer2peer = outputData.getAsJsonArray("peer2peer");
    for (JsonElement connectionElement : peer2peer) {
        JsonObject connectionObj = connectionElement.getAsJsonObject();
        String sourceName = connectionObj.get("sourceName").getAsString();
        String targetName = connectionObj.get("targetName").getAsString();

        PeerStats sourceStats = getPeerStats(sourceName);
        PeerStats targetStats = getPeerStats(targetName);

        int bandwidth = Math.min((int) (sourceStats.maxUpload * 1000), (int) (targetStats.maxDownload * 1000)); // Bandbreite in Kbps
        double latency = sourceStats.latency + targetStats.latency;
        double packetLoss = sourceStats.packetLoss + targetStats.packetLoss;

        JsonObject connectionJson = new JsonObject();
        connectionJson.addProperty("sourceName", sourceName);
        connectionJson.addProperty("targetName", targetName);
        connectionJson.addProperty("bandwidth", bandwidth);
        connectionJson.addProperty("latency", latency);
        connectionJson.addProperty("loss", packetLoss);

        connectionsArray.add(connectionJson);
    }

    // Schreiben des JSON-Arrays in eine Datei
    try (FileWriter writer = new FileWriter(filePath)) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(connectionsArray, writer);
    }
    }
}
