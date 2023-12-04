import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

import java.io.File;
import java.io.IOException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LectureStudioServer {

    private final EventLoopGroup group = new NioEventLoopGroup();
    private final int port;
    private final String filePath = "/app/mydocument.pdf"; // File path
    private int successfulTransfers = 0; // Zähler für erfolgreiche Übertragungen
    private int totalPeers; // Gesamtanzahl der Peers

    public LectureStudioServer(int port) {
        this.port = port;
        System.out.println("LectureStudioServer initialized on port " + port);
    }

    private void sendFileToPeer(String peerIP, int peerPort) {

        Bootstrap b = new Bootstrap();
        b.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) {
                        ch.pipeline().addLast(new ChunkedWriteHandler(), new FileSenderHandler(filePath, peerIP));
                        System.out.println("Channel initialized for peer " + peerIP);
                    }
                });

        b.connect(new InetSocketAddress(peerIP, peerPort)).addListener(future -> {
            if (!future.isSuccess()) {
                System.err.println("Failed to connect with peer " + peerIP + ": " + future.cause());
            } else {
                System.out.println("Connection established with peer " + peerIP);
                updateTransferStatus();
            }
        });
    }

    private synchronized void updateTransferStatus() {
        successfulTransfers++;
        if (successfulTransfers == totalPeers) {
            System.out.println("All peers have received the file.");
        }
    }

    public void sendFileToAllPeers(Map<String, Integer> peerAddresses) {
        totalPeers = peerAddresses.size();
        System.out.println("Sending file to all peers...");
        for (Map.Entry<String, Integer> entry : peerAddresses.entrySet()) {
            sendFileToPeer(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Integer> resolvePeerAddressesAndPorts(String peersEnvVar) throws IOException {
        System.out.println("Resolving peer addresses and ports...");
        List<String> peerNames = peersEnvVar != null ? Arrays.asList(peersEnvVar.split(",")) : new ArrayList<>();
        Map<String, Integer> peerAddresses = new HashMap<>();

        for (String peerName : peerNames) {
            String peerIP = resolvePeerAddress("p2p-containerlab-topology-" + peerName);
            int peerPort = resolvePeerPort(peerName); // Verwendet die separate resolvePeerPort Methode
            if (peerIP != null) {
                peerAddresses.put(peerIP, peerPort);
            }
        }

        return peerAddresses;
    }

    private int resolvePeerPort(String peerName) {
        String portEnvVar = "PEER_" + peerName + "_PORT"; // Konstruiert den Namen der Umgebungsvariablen für den Port
        String portValue = System.getenv(portEnvVar); // Holt den Port aus der Umgebungsvariable
        return portValue != null ? Integer.parseInt(portValue) : -1; // Konvertiert den Port-Wert in eine Integer-Zahl
    }

    private String resolvePeerAddress(String peerName) throws IOException {
        try {
            InetAddress inetAddress = InetAddress.getByName(peerName);
            System.out.println("Resolved IP for " + peerName + ": " + inetAddress.getHostAddress());
            return inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            System.err.println("Unable to resolve IP address for peer " + peerName);
            return null;
        }
    }

    public static void main(String[] args) throws IOException {
        int port = 8080; // Server listening port
        String peersEnvVar = System.getenv("TARGET_PEERS");
        LectureStudioServer server = new LectureStudioServer(port);
        Map<String, Integer> peerAddresses = server.resolvePeerAddressesAndPorts(peersEnvVar);
        System.out.println("Peer IPs and Ports: " + peerAddresses);
        if (peerAddresses.isEmpty()) {
            System.out.println("No peers found for file transmission.");
        } else {
            server.sendFileToAllPeers(peerAddresses);
        }
    }
}

class FileSenderHandler extends SimpleChannelInboundHandler<Object> {
    private final String filePath;
    private final String peerIP; // Hinzufügen der Peer-IP

    public FileSenderHandler(String filePath, String peerIP) {
        this.filePath = filePath;
        this.peerIP = peerIP; // Speichern der Peer-IP
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        File file = new File(filePath);
        if (file.exists()) {
            System.out.println("Sending file: " + filePath + " to peer " + peerIP);
            ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length()));
        } else {
            System.err.println("File not found: " + filePath);
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
    }
}
