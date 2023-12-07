import java.io.*;
import java.security.*;

public class CompareFiles {

    public static void main(String[] args) {
        final int numberOfContainers = 10; // Number of containers to be checked
        final String baseContainerName = "p2p-containerlab-topology-";
        final String specialContainerName = "p2p-containerlab-topology-lecturestudioserver";
        final String[] containerPaths = {
            "/app/mydocument.pdf",
            "/app/receivedMydocumentFromLectureStudioServer.pdf",
            "/app/receivedMydocumentFromSuperPeer.pdf"
        };
        final String SOURCE_FILE_DIR = "/home/ozcankaraca/Desktop/mydocument.pdf"; // Adjust local file path

        boolean allHashesMatch = true;

        try {
            String originalHash = calculateFileHash(SOURCE_FILE_DIR);
            System.out.println("Original File Hash: " + originalHash);

            // Check special container
            allHashesMatch &= checkAndCompareHashes(specialContainerName, containerPaths, originalHash);

            // Check the remaining containers
            for (int i = 1; i <= numberOfContainers; i++) {
                String containerName = baseContainerName + i;
                allHashesMatch &= checkAndCompareHashes(containerName, containerPaths, originalHash);
            }

            if (allHashesMatch) {
                System.out.println("All containers have the same file based on the hash values.");
            } else {
                System.out.println("Not all containers have the same file based on the hash values.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkAndCompareHashes(String containerName, String[] containerPaths, String originalHash) throws IOException, InterruptedException {
        boolean fileExists = false;
        boolean containerHashMatches = true;
        for (String containerPath : containerPaths) {
            if (doesFileExistInContainer(containerName, containerPath)) {
                fileExists = true;
                String containerHash = getContainerFileHash(containerName, containerPath);
                System.out.println("Hash for " + containerPath + " in container " + containerName + ": " + containerHash);

                if (originalHash.equals(containerHash)) {
                    System.out.println("The hash values match for: " + containerPath);
                } else {
                    System.out.println("Hash values do not match for: " + containerPath);
                    containerHashMatches = false;
                }
                break; // Stop the loop after finding the first existing file
            }
        }

        if (!fileExists) {
            System.out.println("None of the files were found in the container " + containerName);
            return false;
        }
        return containerHashMatches;
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
