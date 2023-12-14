
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class LectureStudioServer {
    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }
    private final int port;
    private final List<String> ipAddresses;

    public LectureStudioServer(int port, List<String> ipAddresses) {
        this.port = port;
        this.ipAddresses = ipAddresses;
        System.out.println("\nLectureStudioServer constructor called with Port: " + port);
    }

    public void start() throws Exception {
        for (String ipAddress : ipAddresses) {
            new Thread(() -> {
                try {
                    startServerOnAddress(ipAddress);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void startServerOnAddress(String ipAddress) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        int maxAttempts = 100; // Maximale Anzahl an Bindungsversuchen
        int attempts = 0; // Aktuelle Anzahl an Versuchen
        boolean bound = false;
        long startTime = System.currentTimeMillis(); // Startzeit für die Bindungsversuche
    
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new FileSenderHandler("/app/mydocument.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
    
            while (!bound && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to bind the server to IP " + ipAddress + " and Port " + port + ". Attempt: " + (attempts + 1));
                    ChannelFuture f = b.bind(InetAddress.getByName(ipAddress), port).sync();
                    f.channel().closeFuture().sync();
                    bound = true; // Bindung erfolgreich
                    System.out.println("Server successfully bound to IP " + ipAddress + " and Port " + port + "mit Duration: ");
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 3 Sekunden Wartezeit zwischen den Versuchen
                }
            }
    
            long duration = System.currentTimeMillis() - startTime; // Gesamtdauer der Bindungsversuche
            System.out.println("Duration: " + duration);

            if (!bound) {
                System.out.println("Server could not be bound to IP " + ipAddress + " and Port " + port + " after " + maxAttempts + " attempts.");
            }
        } finally {
            if (!bound) {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        }
    }
    private static List<String> extractIPAddressesFromEnv() {
        List<String> ipAddresses = new ArrayList<>();
        Map<String, String> env = System.getenv();

        for (String envKey : env.keySet()) {
            if (envKey.startsWith("CONNECTION_")) {
                String connectionValue = env.get(envKey);
                System.out.println(connectionValue);
                String firstIPAddress = extractFirstIPAddress(connectionValue);
                if (firstIPAddress != null) {
                    ipAddresses.add(firstIPAddress);
                }
            }
        }
        return ipAddresses;
    }

    private static String extractFirstIPAddress(String connectionInfo) {
        String[] parts = connectionInfo.split(",");
        if (parts.length > 0) {
            String[] subParts = parts[0].split(":");
            return subParts[1];
        }
        return null;
    }
    
    public static void main(String[] args) throws Exception {
        Thread.sleep(10000);
        System.out.println("****************Main method of LectureStudioServer started****************\n");
        String peersEnvVar = System.getenv("TARGET_PEERS");
        List<String> myPeers = peersEnvVar != null ? Arrays.asList(peersEnvVar.split(",")) : new ArrayList<>();
        for (String peer : myPeers) {
            System.out.println("Data is going to be sent to the container p2p-containerlab-topology-" + peer);
        }
        int port = 8080;
        List<String> ipAddresses = extractIPAddressesFromEnv();
        System.out.println("Erste IP-Adressen: " + ipAddresses);

        new LectureStudioServer(port, ipAddresses).start();

        Thread.sleep(5000000);
    }

}