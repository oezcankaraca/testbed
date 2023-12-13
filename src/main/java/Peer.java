import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.Level;

public class Peer {

    static {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR);
    }

    private final int port;

    public Peer(int port) {
        this.port = port;
        if (port == 8080) {
            System.out.println("Peer constructor called with lectureStudioServer and " + port);
        } else if (port == 9090) {
            System.out.println("Peer constructor called with superPeer and " + port);
        }
    }

    public void startLectureStudioServer() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(
                                    new FileReceiverHandler("/app/receivedMydocumentFromLectureStudioServer.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100; // Maximum number of connection attempts
            int attempts = 0; // Current number of attempts
            boolean connected = false;
            long startTime = System.currentTimeMillis(); // Start time for connection attempts

            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to connect to " + superPeerIP + ". Attempt: " + (attempts + 1));
                    ChannelFuture f = b.connect(superPeerIP, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true; // Connection successful
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 1 second waiting time between attempts
                }
            }

            long duration = System.currentTimeMillis() - startTime; // Total duration of connection attempts

            if (connected) {
                System.out
                        .println("Connection successfully established to: " + port + " and with lecturestudioserver in "
                                + duration / 1000 + " seconds after " + attempts + " attempts.");
            } else {
                System.out.println("Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void startSuperPeer(String superPeerHost) throws Exception {
        String superPeerIP = System.getenv("SUPER_PEER_IP_ADDRES");
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new FileReceiverHandler("/app/receivedMydocumentFromSuperPeer.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            int maxAttempts = 100; // Maximum number of connection attempts
            int attempts = 0; // Current number of attempts
            boolean connected = false;
            long startTime = System.currentTimeMillis(); // Start time for connection attempts

            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to connect to: " + superPeerIP + " Attempt: " + (attempts + 1));
                    ChannelFuture f = b.connect(superPeerIP, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true; // Connection successful
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 1 second waiting time between attempts
                }
            }

            long duration = System.currentTimeMillis() - startTime; // Total duration of connection attempts

            if (connected) {
                System.out.println("Connection successfully established to: " + port + " and with super peer "
                        + superPeerHost + ", with IP address: " + superPeerIP + " in " + duration / 1000
                        + " seconds after " + attempts + " attempts.");
            } else {
                System.out.println("Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        Thread.sleep(10000);
        System.out.println("****************Main method of Peer started****************\n");
        int portLectureServerStudio = 8080;
        int portSuperPeer = 9090;
        String superPeerHost = System.getenv("SUPER_PEER");


        if (superPeerHost.equals("lectureStudioServer")) {
            System.out.println("Super peer of this peer is lecturestudioserver");
            new Peer(portLectureServerStudio).startLectureStudioServer();
        } else {

            System.out.println("Super peer of this peer is " + superPeerHost + "\n");
            new Peer(portSuperPeer).startSuperPeer(superPeerHost);
        }

        Thread.sleep(5000000);
    }
}