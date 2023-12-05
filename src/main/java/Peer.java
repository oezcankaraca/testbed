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
        // Initialisiert die Log4j-Konfiguration
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.ERROR); // Setzt das globale Log-Level auf ERROR
    }

    private final int port;

    public Peer(int port) {
        this.port = port;
        System.out.println("Peer-Konstruktor aufgerufen mit " + port);
    }

    public void startLectureStudioServer() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        System.out.println("WorkerGroup erstellt.");
        try {
            Bootstrap b = new Bootstrap();
            System.out.println("Bootstrap-Objekt erstellt.");
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            System.out.println("ChannelInitializer: Pipeline-Konfiguration.");
                            ch.pipeline().addLast(new FileReceiverHandler("/app/receivedMydocument.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Verbindungsaufbau zu: " + port);
            Thread.sleep(5000);
            ChannelFuture f = b.connect("172.100.100.10", port);
            System.out.println("Verbindung hergestellt zu:" + port);

            // Warten, bis der Channel geschlossen wird
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            System.out.println("WorkerGroup heruntergefahren.");
        }
    }

    public void startSuperPeer(String serverAddress) throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        System.out.println("WorkerGroup erstellt.");
        try {
            Bootstrap b = new Bootstrap();
            System.out.println("Bootstrap-Objekt erstellt.");
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            System.out.println("ChannelInitializer: Pipeline-Konfiguration.");
                            ch.pipeline().addLast(new FileReceiverHandler("/app/receivedMydocument.pdf"));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Verbindungsaufbau zu: " + port + "und mit IP Adress: " + serverAddress);
            Thread.sleep(5000);
            ChannelFuture f = b.connect(serverAddress, port);
            System.out.println("Verbindung hergestellt zu:" + port + "und mit IP Adress: " + serverAddress);

            // Warten, bis der Channel geschlossen wird
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            System.out.println("WorkerGroup heruntergefahren.");
        }
    }

    public static void main(String[] args) throws Exception {
        Thread.sleep(10000);
        System.out.println("Main-Methode gestartet.");
        int portLectureServerStudio = 8080;
        int portSuperPeer = 9090;
        System.out.println("Peer-Instanz wird erstellt.");
        String superPeerHost = System.getenv("SUPER_PEER");
        String serverAddress = null;

        if (superPeerHost.equals("lectureStudioServer")) {
            new Peer(portLectureServerStudio).startLectureStudioServer();
        } 
        else {
            InetAddress inetAddress = InetAddress.getByName("p2p-containerlab-topology-" + superPeerHost);
            serverAddress = inetAddress.getHostAddress(); // Holen Sie sich die IP-Adresse aus dem
            new Peer(portSuperPeer).startSuperPeer(serverAddress);
            System.out.println("The superpeer of this peer is not lecturestudioserver, it is " + superPeerHost);
        }
    }
}