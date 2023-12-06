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
        if(port == 808)
        {
            System.out.println("Peer-Konstruktor aufgerufen mit lectureStudioServer und " + port);
        } else if (port == 9090)
        {
            System.out.println("Peer-Konstruktor aufgerufen mit superPeer und " + port);
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
    
            int maxAttempts = 5; // Maximale Anzahl von Verbindungsversuchen
            int attempts = 0;    // Aktuelle Anzahl von Versuchen
            boolean connected = false;
    
            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Versuche, eine Verbindung herzustellen zu lecturestudioserver. Versuch: " + (attempts + 1));
                    ChannelFuture f = b.connect("172.100.100.10", port).sync();
                    f.channel().closeFuture().sync();
                    connected = true;  // Verbindung erfolgreich
                    System.out.println("Verbindung erfolgreich hergestellt zu: " + port + " und mit lecturestudioserver");
                } catch (Exception e) {
                    attempts++;
                    System.out.println("Verbindung fehlgeschlagen. Versuche erneut in 5 Sekunden...");
                    Thread.sleep(5000); // 5 Sekunden Wartezeit zwischen den Versuchen
                }
            }
            if (!connected) {
                System.out.println("Verbindung konnte nicht hergestellt werden nach " + maxAttempts + " Versuchen.");
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
    
            int maxAttempts = 5; // Maximale Anzahl von Verbindungsversuchen
            int attempts = 0;    // Aktuelle Anzahl von Versuchen
            boolean connected = false;
    
            while (!connected && attempts < maxAttempts) {
                try {
                    System.out.println("Versuche, eine Verbindung herzustellen zu: " + serverAddress + " Versuch: " + (attempts + 1));
                    ChannelFuture f = b.connect(serverAddress, port).sync();
                    f.channel().closeFuture().sync();
                    connected = true;  // Verbindung erfolgreich
                    System.out.println("Verbindung erfolgreich hergestellt zu: " + port + " und mit Peer " + superPeerHost + ", mit IP Adresse: " + serverAddress);
                } catch (Exception e) {
                    attempts++;
                    Thread.sleep(3000); // 5 Sekunden Wartezeit zwischen den Versuchen
                }
            }
            if (!connected) {
                System.out.println("Verbindung konnte nicht hergestellt werden nach " + maxAttempts + " Versuchen.");
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
    

    public static void main(String[] args) throws Exception {
        Thread.sleep(20000);
        System.out.println("Main-Methode des Peer gestartet.\n");
        int portLectureServerStudio = 8080;
        int portSuperPeer = 9090;
        String superPeerHost = System.getenv("SUPER_PEER");
        String serverAddress = null;

        if (superPeerHost.equals("lecturestudioserver")) {
            new Peer(portLectureServerStudio).startLectureStudioServer();
        } 
        else {
            InetAddress inetAddress = InetAddress.getByName("p2p-containerlab-topology-" + superPeerHost);
            serverAddress = inetAddress.getHostAddress(); // Holen Sie sich die IP-Adresse aus dem
            new Peer(portSuperPeer).startSuperPeer(serverAddress, superPeerHost);
            System.out.println("SuperPeer von diesem Peer ist " + superPeerHost);
        }

        Thread.sleep(5000000);
    }
}