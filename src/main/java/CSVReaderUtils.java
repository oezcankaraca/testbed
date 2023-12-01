import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides utilities to read network statistics from a CSV file
 * and write certain metrics like download speed, upload speed, latency, and
 * packet loss to a text file.
 */
public class CSVReaderUtils {

    // List to store network statistics
    public static List<Double[]> networkStatsList = new ArrayList<>();

    /**
     * Reads network statistics from a CSV file and writes calculated metrics to a
     * text file.
     * 
     * @param pathToCSV  The file path to the CSV input.
     * @param pathToOutput The file path for the text output.
     */
    public static void readCsvDataAndWriteToFile(String pathToCSV, String pathToOutput) {
        try (CSVReader reader = new CSVReader(new FileReader(pathToCSV));
                BufferedWriter writer = new BufferedWriter(new FileWriter(pathToOutput))) {

            String[] nextLine;
            String[] headers = reader.readNext(); // Reads the header line

            // Indices for the columns we are interested in
            int indexMaximumDownloadSpeed = -1;
            int indexMaximumUploadSpeed = -1;
            int indexDownloadSpeed = -1;
            int indexUploadSpeed = -1;
            int indexLatency = -1;
            int indexPacketLoss = -1;

            
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim(); 
                switch (header) {
                    case "Peak average maximum upload speed":
                        indexMaximumUploadSpeed = i;
                        break;
                    case "Peak average maximum download speed":
                        indexMaximumDownloadSpeed = i;
                        break;
                    case "Peak average minimum speed download":
                        indexDownloadSpeed = i;
                        break;
                    case "Peak average minimum speed upload":
                        indexUploadSpeed = i;
                        break;
                    case "24 hour Latency":
                        indexLatency = i;
                        break;
                    case "24 hour packet loss":
                        indexPacketLoss = i;
                        break;
                }
            }

            // Check if all indices were found
            if (indexMaximumUploadSpeed == -1 || indexMaximumDownloadSpeed == -1 ||
                    indexDownloadSpeed == -1 || indexUploadSpeed == -1 ||
                    indexLatency == -1 || indexPacketLoss == -1) {
                throw new IllegalArgumentException(
                        "One or more required columns were not found in the header.");
            }

            // Write the headings in the file
            writer.write("***Network Statistics: Download and Upload Speeds, Latency, Packet Loss***\n\n");
            while ((nextLine = reader.readNext()) != null) {
                try {
                    // Convert the read strings into Double values
                    Double maximumUploadSpeed = Double.parseDouble(nextLine[indexMaximumUploadSpeed]);
                    Double maximumDownloadSpeed = Double.parseDouble(nextLine[indexMaximumDownloadSpeed]);
                    Double downloadSpeed = Double.parseDouble(nextLine[indexDownloadSpeed]);
                    Double uploadSpeed = Double.parseDouble(nextLine[indexUploadSpeed]);
                    Double latency = Double.parseDouble(nextLine[indexLatency]);
                    Double packetLoss = Double.parseDouble(nextLine[indexPacketLoss]);

                    // Add the values to the list
                    networkStatsList.add(new Double[] { maximumUploadSpeed, maximumDownloadSpeed,
                            downloadSpeed, uploadSpeed,
                            latency, packetLoss });

                    // Write the speeds to the file
                    writer.write(String.format("Maximum Upload Speed: %.2f Mbps, Maximum Download Speed: %.2f Mbps%n",
                            maximumUploadSpeed, maximumDownloadSpeed));
                    writer.write(String.format("Download Speed: %.2f Mbps, Upload Speed: %.2f Mbps%n",
                            downloadSpeed, uploadSpeed));
                    writer.write(String.format("Latency: %.2f ms, Packet Loss: %.4f%%%n", latency, packetLoss));
                    writer.write("\n");

                } catch (NumberFormatException e) {
                    System.out.println("Error parsing numeric data: " + e.getMessage());
                }
            }

            writer.flush(); // Ensure all data is written

        } catch (IOException | CsvValidationException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        String pathToCSV = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/data/fixed-broadband-speeds-august-2019-data-25.csv";
        String pathToNetworkStatistics = "/home/ozcankaraca/Desktop/master-thesis-okaraca/testbed/src/resources/results/network-statistics.txt";

        // Execute the read and write process
        readCsvDataAndWriteToFile(pathToCSV, pathToNetworkStatistics);
        System.out.println("Network statistics have been written to the file: " + pathToNetworkStatistics);
    }
}
