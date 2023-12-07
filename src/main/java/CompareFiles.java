import java.io.*;
import java.security.*;

public class CompareFiles {

    public static void main(String[] args) {
        final int numberOfContainers = 10; // Oder die Anzahl der Container, die Sie überprüfen möchten
        final String baseContainerName = "p2p-containerlab-topology-";
        final String specialContainerName = "p2p-containerlab-topology-lecturestudioserver";
        final String[] containerPaths = {
            "/app/mydocument.pdf",
            "/app/receivedMydocumentFromLectureStudioServer.pdf",
            "/app/receivedMydocumentFromSuperPeer.pdf"
        };
        final String SOURCE_FILE_DIR = "/home/ozcankaraca/Desktop/mydocument.pdf"; // Lokalen Dateipfad anpassen

        try {
            String originalHash = calculateFileHash(SOURCE_FILE_DIR);
            System.out.println("Original File Hash: " + originalHash);

            // Prüfe speziellen Container
            checkAndCompareHashes(specialContainerName, containerPaths, originalHash);

            // Prüfe die restlichen Container
            for (int i = 1; i <= numberOfContainers; i++) {
                String containerName = baseContainerName + i;
                checkAndCompareHashes(containerName, containerPaths, originalHash);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void checkAndCompareHashes(String containerName, String[] containerPaths, String originalHash) throws IOException, InterruptedException {
        boolean fileExists = false;
        for (String containerPath : containerPaths) {
            if (doesFileExistInContainer(containerName, containerPath)) {
                fileExists = true;
                String containerHash = getContainerFileHash(containerName, containerPath);
                System.out.println("Hash for " + containerPath + " in container " + containerName + ": " + containerHash);

                if (originalHash.equals(containerHash)) {
                    System.out.println("The hash values match for: " + containerPath);
                } else {
                    System.out.println("Hash values do not match for: " + containerPath);
                }
                break; // Beendet die Schleife nach dem Finden der ersten vorhandenen Datei
            }
        }

        if (!fileExists) {
            System.out.println("Keine der Dateien wurde im Container " + containerName + " gefunden.");
        }
    }

    private static boolean doesFileExistInContainer(String containerName, String containerFilePath) throws IOException, InterruptedException {
        String[] checkCommand = {
            "docker", "exec", containerName, "sh", "-c",
            "[ -f " + containerFilePath + " ] && echo found || echo not found"
        };
        Process checkProcess = Runtime.getRuntime().exec(checkCommand);
        BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
        String checkResult = checkReader.readLine();
        checkProcess.waitFor();
        return "found".equals(checkResult);
    }
      

    private static String getContainerFileHash(String containerName, String containerFilePath) throws IOException, InterruptedException {
        String[] command = {
            "docker", "exec", containerName, "sh", "-c",
            "sha256sum " + containerFilePath + " | awk '{print $1}'"
        };
        
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        
        // Lesen der Ausgabe des Befehls
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = stdInput.readLine()) != null) {
            output.append(line).append("\n");
        }
    
        // Lesen von Fehlermeldungen
        while ((line = stdError.readLine()) != null) {
            System.out.println("ERROR: " + line);
        }
        
        process.waitFor();
        return output.length() > 0 ? output.toString().trim() : "Fehler bei der Hashberechnung";
    }
    

    public static String calculateFileHash(String filePath) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath);
             DigestInputStream dis = new DigestInputStream(fis, sha256)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // Reading file for hashing
            }
            byte[] digest = sha256.digest();
            return bytesToHex(digest);
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
