import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;

public class Peer {

    private final int port;

    public Peer(int port) {
        this.port = port;
        System.out.println("Peer initialized on port " + port);
    }

    public void start() throws Exception {
        Thread.sleep(5000);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<Channel>() {
                 @Override
                 protected void initChannel(Channel ch) {
                     ch.pipeline().addLast(new ChunkedWriteHandler(), new FileReceiverHandler("/app/receivedFileFromLectureStudioServer.pdf"));
                     System.out.println("Channel initialized for incoming connections.");
                 }
             })
             .option(ChannelOption.SO_BACKLOG, 128)
             .childOption(ChannelOption.SO_KEEPALIVE, true);

            System.out.println("Starting peer server on port " + port);
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Peer server started and listening on port " + port);

            f.channel().closeFuture().sync();
        } catch (Exception e) {
            System.err.println("Error in starting peer server: " + e.getMessage());
            throw e;
        } finally {
            System.out.println("Shutting down peer server...");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("Peer server shut down.");
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080; // Port, auf dem dieser Peer lauschen wird
        new Peer(port).start();
    }
}
