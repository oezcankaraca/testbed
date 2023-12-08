import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GeneratorOfNetworkTopology extends CSVReaderUtils {
    private static final int numberOfPeers = 50; // The number of peers to include in the network

    public static void main(String[] args) {
        String pathToCSV = "/home/ozcankaraca/Desktop/testbed/src/resources/data/fixed-broadband-speeds-august-2019-data-25.csv";
        String pathToNetworkStatistics = "/home/ozcankaraca/Desktop/testbed/src/resources/results/network-statistics.txt";
        String pathToInputData = "/home/ozcankaraca/Desktop/testbed/src/resources/data/input-data.json";

        readCsvDataAndWriteToFile(pathToCSV, pathToNetworkStatistics, numberOfPeers);

        JsonObject inputDataObject = generateInputDataObject();

        generateInputDataJsonFile(inputDataObject, pathToInputData);
    }

    private static JsonObject generateInputDataObject() {
        JsonObject inputDataObject = new JsonObject();
        inputDataObject.addProperty("filename", "test.pdf");
        inputDataObject.addProperty("filesize", 5000);
        JsonArray peersArray = new JsonArray();

        for (int i = 0; i <= numberOfPeers; i++) {
            String peerId = i == 0 ? "lecturestudioserver" : String.valueOf(i);
            PeerStats stats = getPeerStats(peerId);

            JsonObject peerObject = new JsonObject();
            peerObject.addProperty("name", peerId);

            int maxUploadKbps = (int) (stats.maxUpload * 1000);
            int maxDownloadKbps = (int) (stats.maxDownload * 1000);

            peerObject.addProperty("maxDownload", maxDownloadKbps);
            peerObject.addProperty("maxUpload", maxUploadKbps);

            peersArray.add(peerObject);
        }

        inputDataObject.add("peers", peersArray);

        JsonArray connectionsArray = createConnectionsArray(peersArray);
        inputDataObject.add("connections", connectionsArray);

        return inputDataObject;
    }

    private static JsonArray createConnectionsArray(JsonArray peersArray) {
        JsonArray connectionsArray = new JsonArray();
    
        for (int sourceIndex = 0; sourceIndex < peersArray.size(); sourceIndex++) {
            for (int targetIndex = 0; targetIndex < peersArray.size(); targetIndex++) {
                if (sourceIndex != targetIndex) {
                    JsonObject connection = new JsonObject();
    
                    // Holen Sie sich die Namen direkt aus dem peersArray
                    String sourceName = peersArray.get(sourceIndex).getAsJsonObject().get("name").getAsString();
                    String targetName = peersArray.get(targetIndex).getAsJsonObject().get("name").getAsString();
    
                    connection.addProperty("sourceName", sourceName);
                    connection.addProperty("targetName", targetName);
    
                    // Retrieve the specific stats for source and target peers
                    PeerStats sourceStats = getPeerStats(sourceName);
                    PeerStats targetStats = getPeerStats(targetName);
    
                    // Convert the bandwidth from Mbps to Kbps and take the minimum of the two peers
                    int connectionBandwidthKbps = (int) (Math.min(sourceStats.maxUpload * 1000, targetStats.maxDownload * 1000));
    
                    // Sum the latencies and take the maximum of the packet losses of the two peers
                    double connectionLatency = sourceStats.latency + targetStats.latency;
                    double connectionLoss = Math.max(sourceStats.packetLoss, targetStats.packetLoss);
    
                    // Format the latency and loss
                    String formattedLatency = String.format("%.2f", connectionLatency);
                    String formattedLoss = String.format("%.4f", connectionLoss);
    
                    // Set the properties for the connection object
                    connection.addProperty("bandwidth", connectionBandwidthKbps); // Store as integer Kbps
                    connection.addProperty("latency", formattedLatency);
                    connection.addProperty("loss", formattedLoss);
    
                    // Add the connection object to the JSON array
                    connectionsArray.add(connection);
                }
            }
        }
    
        // Shuffle the list
        List<JsonElement> connectionList = new ArrayList<>();
        for (JsonElement elem : connectionsArray) {
            connectionList.add(elem);
        }
        Collections.shuffle(connectionList);
    
        // Clear the original JsonArray and add the shuffled elements back
        JsonArray shuffledArray = new JsonArray();
        for (JsonElement connectionElement : connectionList) {
            shuffledArray.add(connectionElement);
        }
    
        return shuffledArray;
    }    

    private static void generateInputDataJsonFile(JsonObject jsonObject, String filePath) {
        try (FileWriter file = new FileWriter(filePath)) {
            // Use Gson to write the JSON object to a file with pretty printing
            new GsonBuilder().setPrettyPrinting().create().toJson(jsonObject, file);
            // Notify the user that the file has been saved
            System.out.println("Input data JSON has been saved to the file: " + filePath);
        } catch (IOException e) {
            // Print the stack trace to the console if an IOException occurs
            e.printStackTrace();
        }
    }
}