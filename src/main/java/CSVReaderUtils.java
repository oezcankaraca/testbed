import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class PeerStats {
    double maxUpload;
    double maxDownload;
    double latency;
    double packetLoss;

    public PeerStats(double maxUpload, double maxDownload, double latency, double packetLoss) {
        this.maxUpload = maxUpload;
        this.maxDownload = maxDownload;
        this.latency = latency;
        this.packetLoss = packetLoss;
    }

    @Override
    public String toString() {
        return String.format("Max Upload: %.2f Mbps, Max Download: %.2f Mbps, Latency: %.2f ms, Packet Loss: %.4f%%",
                maxUpload, maxDownload, latency, packetLoss);
    }
}

public class CSVReaderUtils {

    private static final Map<String, PeerStats> peerStatsMap = new HashMap<>();

    public static void readCsvDataAndWriteToFile(String pathToCSV, String pathToOutput, int numberOfPeers) {
        try (CSVReader reader = new CSVReader(new FileReader(pathToCSV));
             BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutput))) {

            String[] nextLine;
            String[] headers = reader.readNext(); // Reads the header line

            // Indices for the columns we are interested in
            int indexMaximumDownloadSpeed = -1;
            int indexMaximumUploadSpeed = -1;
            int indexLatency = -1;
            int indexPacketLoss = -1;

            // Identifying the indices of the columns
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim();
                switch (header) {
                    case "Peak average maximum upload speed":
                        indexMaximumUploadSpeed = i;
                        break;
                    case "Peak average maximum download speed":
                        indexMaximumDownloadSpeed = i;
                        break;
                    case "24 hour Latency":
                        indexLatency = i;
                        break;
                    case "24 hour packet loss":
                        indexPacketLoss = i;
                        break;
                }
            }

            if (indexMaximumUploadSpeed == -1 || indexMaximumDownloadSpeed == -1 ||
                    indexLatency == -1 || indexPacketLoss == -1) {
                throw new IllegalArgumentException(
                        "One or more required columns were not found in the header.");
            }

            for (int peerIndex = 1; peerIndex <= numberOfPeers + 1; peerIndex++) {
                if ((nextLine = reader.readNext()) == null) break;

                try {
                    Double maximumUploadSpeed = Double.parseDouble(nextLine[indexMaximumUploadSpeed]);
                    Double maximumDownloadSpeed = Double.parseDouble(nextLine[indexMaximumDownloadSpeed]);
                    Double latency = Double.parseDouble(nextLine[indexLatency]);
                    Double packetLoss = Double.parseDouble(nextLine[indexPacketLoss]);

                    PeerStats stats = new PeerStats(maximumUploadSpeed, maximumDownloadSpeed, latency, packetLoss);
                    String peerId = peerIndex <= numberOfPeers ? String.valueOf(peerIndex) : "lectureStudioServer";

                    peerStatsMap.put(peerId, stats);
                    writer.write(peerId + ": ");
                    writer.write(stats.toString() + "\n");

                } catch (NumberFormatException e) {
                    System.out.println("Error parsing numeric data: " + e.getMessage());
                }
            }

            writer.flush();

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }

    public static PeerStats getPeerStats(String peerId) {
        return peerStatsMap.get(peerId);
    }

    public static void main(String[] args) {
        String pathToCSV = "/home/ozcankaraca/Desktop/testbed/src/resources/data/fixed-broadband-speeds-august-2019-data-25.csv";
        String pathToNetworkStatistics = "/home/ozcankaraca/Desktop/testbed/src/resources/results/network-statistics.txt";
        int numberOfPeers = 10; // Set the number of peers you want to read

        readCsvDataAndWriteToFile(pathToCSV, pathToNetworkStatistics, numberOfPeers);
        System.out.println("Network statistics have been written to the file: " + pathToNetworkStatistics);

        // Beispiel, um die Daten einer bestimmten Peer zu erhalten
        String peerToRetrieve = "lectureStudioServer"; // Peer ID als String
        PeerStats stats = getPeerStats(peerToRetrieve);
        if (stats != null) {
            System.out.println("Data for Peer " + peerToRetrieve + ": " + stats);
        } else {
            System.out.println("No data found for Peer " + peerToRetrieve);
        }
    }
}