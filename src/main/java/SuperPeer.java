import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
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

    public SuperPeer(int serverPort, int clientPort, String filePathToSend, String filePathToReceive) {
        this.serverPort = serverPort;
        this.clientPort = clientPort;
        this.filePathToSend = filePathToSend;
        this.filePathToReceive = filePathToReceive;
        System.out.println("SuperPeer erstellt mit Server-Port: " + serverPort + " und Client-Port: " + clientPort);
    }

    public void startServer() throws Exception {
        System.out.println("Starte Server auf Port " + serverPort);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     //System.out.println("Server: ChannelInitializer für Port " + serverPort);
                     ch.pipeline().addLast(new FileSenderHandler(filePathToSend));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.bind(serverPort).sync();
            //System.out.println("Server gebunden an Port " + serverPort);
            f.channel().closeFuture().sync();
            //System.out.println("Server-Channel auf Port " + serverPort + " geschlossen.");
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public void startClient() throws Exception {
        //System.out.println("Starte Client, verbinde mit Server auf Port " + clientPort);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
             .channel(NioSocketChannel.class)
             .handler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     //System.out.println("Client: ChannelInitializer für Verbindung zu: " + clientPort);
                     ch.pipeline().addLast(new FileReceiverHandler(filePathToReceive));
                 }
             })
             .option(ChannelOption.SO_KEEPALIVE, true);

            ChannelFuture f = b.connect("172.100.100.10", clientPort).sync();
            //System.out.println("Client verbunden mit Server: " + clientPort);
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        Thread.sleep(20000);
        System.out.println("Main-Methode des SuperPeer gestartet.");
        int serverPort = 9090; // Port für den Server-Teil
        int clientPort = 8080; // Port für den Client-Teil
        String filePathToSend = "/app/receivedMydocumentFromLectureStudioServer.pdf";
        String filePathToReceive = "/app/receivedMydocumentFromLectureStudioServer.pdf";

        SuperPeer superPeer = new SuperPeer(serverPort, clientPort, filePathToSend, filePathToReceive);

        // Starten des Client-Teils in einem separaten Thread
        new Thread(() -> {
            try {
                superPeer.startClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Starten des Server-Teils in einem separaten Thread
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


