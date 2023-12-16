import java.io.*;
import java.security.*;

/**
 * The CompareFiles class is designed to verify the integrity of files across
 * multiple Docker containers.
 * It achieves this by comparing the SHA-256 hash values of files located in
 * different containers against
 * a reference hash value of an original file. This class is useful in scenarios
 * where consistency and
 * data integrity of files distributed across multiple containers need to be
 * ensured.
 * 
 * The main method initializes the process by defining container names, file
 * paths, and calculating the hash
 * of an original file. It then iterates through each specified container,
 * checking if the files exist and
 * comparing their hash values against the original file's hash. The class
 * provides detailed output about
 * the hash comparison results, including whether the hashes match or not.
 * 
 * Key methods include:
 * - checkAndCompareHashes: Checks if files in a container match the original
 * file's hash.
 * - doesFileExistInContainer: Checks for the existence of a file in a Docker
 * container.
 * - getContainerFileHash: Calculates the SHA-256 hash of a file within a Docker
 * container.
 * - calculateFileHash: Calculates the SHA-256 hash of a local file.
 * - bytesToHex: Converts byte array into a hexadecimal string.
 */
public class CompareFiles {

    public static void main(String[] args) {
        // Define the number of containers to be checked
        final int numberOfContainers = 50;

        // Base name for containers, used for individually naming the containers
        final String baseContainerName = "p2p-containerlab-topology-";

        // Special container name for the "lectureStudioServer"
        final String specialContainerName = "p2p-containerlab-topology-lectureStudioServer";

        // Array of file paths to be checked in the containers
        final String[] containerPaths = {
                "/app/mydocument.pdf",
                "/app/receivedMydocumentFromLectureStudioServer.pdf",
                "/app/receivedMydocumentFromSuperPeer.pdf"
        };

        // Local path of the source file, adjust as needed
        final String SOURCE_FILE_DIR = "/home/ozcankaraca/Desktop/mydocument.pdf";

        // Flag to track if all file hashes match
        boolean allHashesMatch = true;

        try {
            // Calculate the hash of the original file
            String originalHash = calculateFileHash(SOURCE_FILE_DIR);
            System.out.println("Original File Hash: " + originalHash);

            // Check and compare hashes for the special container
            allHashesMatch &= checkAndCompareHashes(specialContainerName, containerPaths, originalHash);

            // Check and compare hashes for the remaining containers
            for (int i = 1; i <= numberOfContainers; i++) {
                String containerName = baseContainerName + i;
                allHashesMatch &= checkAndCompareHashes(containerName, containerPaths, originalHash);
            }

            // Output the result of hash comparison
            if (allHashesMatch) {
                System.out.println("All containers have the same file based on the hash values.");
            } else {
                System.out.println("Not all containers have the same file based on the hash values.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks and compares the hash of files within a container against an original
     * hash.
     * Iterates through file paths within the container, checks if each file exists,
     * then compares its hash with the original hash, stopping after finding the
     * first existing file.
     *
     * @param containerName  The name of the container where the files are located.
     * @param containerPaths An array of paths to the files within the container.
     * @param originalHash   The hash value of the original file for comparison.
     * @return True if all found files have matching hash values; False if any file
     *         is not found or hashes do not match.
     * @throws InterruptedException
     * @throws IOException
     */
    private static boolean checkAndCompareHashes(String containerName, String[] containerPaths, String originalHash)
            throws IOException, InterruptedException {
        boolean fileExists = false;
        boolean containerHashMatches = true;

        // Loop through each file path in the container
        for (String containerPath : containerPaths) {
            // Check if the current file exists in the container
            if (doesFileExistInContainer(containerName, containerPath)) {
                fileExists = true;
                // Get the hash of the file from the container
                String containerHash = getContainerFileHash(containerName, containerPath);
                System.out
                        .println("Hash for " + containerPath + " in container " + containerName + ": " + containerHash);

                // Compare the container file hash with the original file hash
                if (originalHash.equals(containerHash)) {
                    System.out.println("The hash values match for: " + containerPath);
                } else {
                    System.out.println("Hash values do not match for: " + containerPath);
                    containerHashMatches = false;
                }
                // Stop after finding the first file that exists
                break;
            }
        }

        // If no file was found in the container, return false
        if (!fileExists) {
            System.out.println("None of the files were found in the container " + containerName);
            return false;
        }
        // Return true if all found files have matching hash values
        return containerHashMatches;
    }

    /**
     * Checks if a file exists within a Docker container.
     * Executes a command in the Docker container to check if a specified file path
     * exists.
     *
     * @param containerName     The name of the Docker container.
     * @param containerFilePath The file path within the container to be checked.
     * @return True if the file is found; False otherwise.
     * @throws IOException
     * @throws InterruptedException
     */
    private static boolean doesFileExistInContainer(String containerName, String containerFilePath)
            throws IOException, InterruptedException {
        String[] checkCommand = {
                "docker", "exec", containerName, "sh", "-c",
                "[ -f " + containerFilePath + " ] && echo found || echo not found"
        };
        // Execute the command to check if the file exists in the container
        Process checkProcess = Runtime.getRuntime().exec(checkCommand);
        BufferedReader checkReader = new BufferedReader(new InputStreamReader(checkProcess.getInputStream()));
        // Read the output of the command
        String checkResult = checkReader.readLine();
        checkProcess.waitFor();
        // Return true if 'found' is in the command output, indicating the file exists
        return "found".equals(checkResult);
    }

    /**
     * Calculates the SHA-256 hash of a file within a Docker container.
     * Executes a command in the container to generate the hash of the specified
     * file.
     *
     * @param containerName     The name of the Docker container.
     * @param containerFilePath The file path within the container to be hashed.
     * @return The SHA-256 hash of the file as a String, or an error message if the
     *         process fails.
     */
    private static String getContainerFileHash(String containerName, String containerFilePath)
            throws IOException, InterruptedException {
        String[] command = {
                "docker", "exec", containerName, "sh", "-c",
                "sha256sum " + containerFilePath + " | awk '{print $1}'"
        };

        // Execute the command to calculate the file's hash
        Process process = Runtime.getRuntime().exec(command);
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

        // Read the command output to get the hash
        String line;
        StringBuilder output = new StringBuilder();
        while ((line = stdInput.readLine()) != null) {
            output.append(line).append("\n");
        }

        // Read any error messages from the command execution
        while ((line = stdError.readLine()) != null) {
            System.out.println("ERROR: " + line);
        }

        process.waitFor();
        return output.length() > 0 ? output.toString().trim() : "Error in hash calculation";
    }

    /**
     * Calculates the SHA-256 hash of a local file.
     * Reads the file and computes its SHA-256 hash.
     *
     * @param filePath The path of the file to be hashed.
     * @return The SHA-256 hash of the file as a String.
     */
    public static String calculateFileHash(String filePath) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(filePath);
                DigestInputStream dis = new DigestInputStream(fis, sha256)) {
            byte[] buffer = new byte[8192];
            // Read the file and update the message digest
            while (dis.read(buffer) != -1) {
                // Continuously reading file for hashing
            }
            byte[] digest = sha256.digest();
            return bytesToHex(digest);
        }
    }

    /**
     * Converts a byte array into a hexadecimal string.
     * Useful for representing hash values in a readable format.
     *
     * @param bytes The byte array to be converted.
     * @return A hexadecimal string representation of the byte array.
     */
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
