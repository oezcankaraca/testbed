import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class SuperPeer {

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private final int serverPort;
    private final int clientPort;
    private final String filePathToSend;
    private final String filePathToReceive;
    private volatile boolean fileReceived = false;

    public SuperPeer(int serverPort, int clientPort, String filePathToSend, String filePathToReceive) {
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.filePathToSend = filePathToSend;
        this.filePathToReceive = filePathToReceive;
        System.out.println("SuperPeer created with Server Port: " + serverPort + " and Client Port: " + clientPort);
    }

    public void startServer() throws Exception {
        int maxAttempts = 100; // Maximum number of connection attempts
        int attempts = 0; // Current number of attempts
        long startTime = System.currentTimeMillis(); // Start time for file receiving attempts

        while (!fileReceived && attempts < maxAttempts) {
            System.out.println("Waiting for the file to be received. Attempt: " + (attempts + 1));
            Thread.sleep(1000); // Wait until the file is received
            attempts++;
        }

        long duration = System.currentTimeMillis() - startTime; // Total duration of file receiving attempts

        if (fileReceived) {
            System.out.println("File received in " + duration / 1000 + " seconds after " + attempts + " attempts.");
            System.out.println("Starting server on Port " + serverPort);
            EventLoopGroup bossGroup = new NioEventLoopGroup();
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<Channel>() {
                            @Override
                            protected void initChannel(Channel ch) {
                                ch.pipeline().addLast(new FileSenderHandler(filePathToSend));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 128)
                        .childOption(ChannelOption.SO_KEEPALIVE, true);

                ChannelFuture f = b.bind(serverPort).sync();
                f.channel().closeFuture().sync();
            } finally {
                workerGroup.shutdownGracefully();
                bossGroup.shutdownGracefully();
            }
        } else {
            System.out.println("File was not received after " + maxAttempts + " attempts. Server not started.");
        }
    }

    public void startClient() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new FileReceiverHandler(filePathToReceive));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100; // Maximum number of connection attempts
            int attempts = 0; // Current number of attempts
            boolean connected = false;
            long startTime = System.currentTimeMillis(); // Start time for connection attempts

            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to establish a connection to " + superPeerIP + ". Attempt: " + (attempts + 1));
                    ChannelFuture f = b.connect(superPeerIP, clientPort).sync();
                    f.channel().closeFuture().sync();
                    connected = true; // Connection successful
                    fileReceived = true;
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 1 second waiting time between attempts
                }
            }

            long duration = System.currentTimeMillis() - startTime; // Total duration of connection attempts

            if (connected) {
                System.out.println("Connection successfully established in " + duration / 1000 + " seconds after "
                        + attempts + " attempts.");
            } else {
                System.out.println("Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        Thread.sleep(10000);
        System.out.println("****************Main method of SuperPeer started****************\n");
        int serverPort = 9090;
        int clientPort = 8080;
        String filePathToSend = "/app/receivedMydocumentFromLectureStudioServer.pdf";
        String filePathToReceive = "/app/receivedMydocumentFromLectureStudioServer.pdf";
        String peersEnvVar = System.getenv("TARGET_PEERS");
        List<String> myPeers = peersEnvVar != null ? Arrays.asList(peersEnvVar.split(",")) : new ArrayList<>();
        for (String peer : myPeers) {
            System.out.println("Data is going to be sent to the container p2p-containerlab-topology-" + peer);
        }
        SuperPeer superPeer = new SuperPeer(serverPort, clientPort, filePathToSend, filePathToReceive);

        Thread clientThread = new Thread(() -> {
            try {
                superPeer.startClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        clientThread.start();
        clientThread.join(); // Wait for the client thread to finish

        new Thread(() -> {
            try {
                superPeer.startServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        Thread.sleep(5000000);
    }
}