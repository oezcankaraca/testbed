import java.io.File;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class LectureStudioServer {

    private final int port;

    public LectureStudioServer(int port) {
        this.port = port;
        System.out.println("LectureStudioServer-Konstruktor aufgerufen mit Port: " + port);
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        System.out.println("BossGroup und WorkerGroup erstellt.");

        try {
            ServerBootstrap b = new ServerBootstrap();
            System.out.println("ServerBootstrap-Objekt erstellt.");
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     System.out.println("ChannelInitializer: Pipeline-Konfiguration für den Port " + port);
                     ch.pipeline().addLast(new FileSenderHandler("/app/mydocument.pdf", "172.100.100.12"));
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Server bindet an Port " + port);
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server gebunden an Port " + port);

            f.channel().closeFuture().sync();
            System.out.println("Server-Channel geschlossen.");
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("BossGroup und WorkerGroup heruntergefahren.");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Main-Methode des LectureStudioServers gestartet.");
        int port = 8080;
        System.out.println("LectureStudioServer-Instanz wird erstellt.");
        new LectureStudioServer(port).start();
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
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
        }
    
        // Rest des Codes bleibt gleich
    }
    
}
