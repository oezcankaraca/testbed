import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Peer {

    private final int port;

    public Peer(int port) {
        this.port = port;
        System.out.println("Peer-Konstruktor aufgerufen mit " + port);
    }

    public void start() throws Exception {
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

            System.out.println("Verbindungsaufbau zu: "+ port);
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

    public static void main(String[] args) throws Exception {
        System.out.println("Main-Methode gestartet.");
        int port = 8080;
        System.out.println("Peer-Instanz wird erstellt.");
         String superPeerHost = System.getenv("SUPER_PEER"); // Hostname des SuperPeers
        System.out.println(superPeerHost);
        if(superPeerHost.equals("lectureStudioServer")){
            new Peer(port).start();
        }
        else {
            System.out.println("This peer has no superpeer from lectureStudioServer");
        }
    }
}