import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetAddress;

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
        if(port == 808) {
            System.out.println("Peer constructor called with lectureStudioServer and " + port);
        } else if (port == 9090) {
            System.out.println("Peer constructor called with superPeer and " + port);
        }
    }

    public void startLectureStudioServer() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline().addLast(new FileReceiverHandler("/app/receivedMydocumentFromLectureStudioServer.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);
    
            int maxAttempts = 5; // Maximum number of connection attempts
            int attempts = 0;    // Current number of attempts
            boolean connected = false;
    
            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to connect to lecturestudioserver. Attempt: " + (attempts + 1));
                    ChannelFuture f = b.connect("172.100.100.10", port).sync();
                    f.channel().closeFuture().sync();
                    connected = true;  // Connection successful
                    System.out.println("Connection successfully established to: " + port + " and with lecturestudioserver");
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 3 seconds waiting time between attempts
                }
            }
            if (!connected) {
                System.out.println("Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }    

    public void startSuperPeer(String serverAddress, String superPeerHost) throws Exception {
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
    
            int maxAttempts = 5; // Maximum number of connection attempts
            int attempts = 0;    // Current number of attempts
            boolean connected = false;
    
            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Attempting to connect to: " + serverAddress + " Attempt: " + (attempts + 1));
                    ChannelFuture f = b.connect(serverAddress, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true;  // Connection successful
                    System.out.println("Connection successfully established to: " + port + " and with super peer " + superPeerHost + ", with IP address: " + serverAddress);
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 3 seconds waiting time between attempts
                }
            }
            if (!connected) {
                System.out.println("Connection could not be established after " + maxAttempts + " attempts.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) throws Exception {
        Thread.sleep(20000);
        System.out.println("****************Main method of Peer started****************\n");
        int portLectureServerStudio = 8080;
        int portSuperPeer = 9090;
        String superPeerHost = System.getenv("SUPER_PEER");
        String serverAddress = null;

        if (superPeerHost.equals("lecturestudioserver")) {
            new Peer(portLectureServerStudio).startLectureStudioServer();
        } 
        else {
            InetAddress inetAddress = InetAddress.getByName("p2p-containerlab-topology-" + superPeerHost);
            serverAddress = inetAddress.getHostAddress(); // Get the IP address from the
            new Peer(portSuperPeer).startSuperPeer(serverAddress, superPeerHost);
        }

        Thread.sleep(5000000);
    }
}