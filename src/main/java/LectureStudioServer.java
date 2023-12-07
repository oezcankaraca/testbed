import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    public LectureStudioServer(int port) {
        this.port = port;
        System.out.println("\nLectureStudioServer constructor called with Port: " + port);
    }

    public void start() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
    
        int maxAttempts = 100;
        int attempts = 0;
        boolean bound = false;
        long startTime = System.currentTimeMillis(); // Start time for binding attempts
    
        while (!bound && attempts < maxAttempts) {
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
    
                System.out.println("Attempting to bind the server to Port " + port + ". Attempt: " + (attempts + 1));
                ChannelFuture f = b.bind(port).sync();
                System.out.println("Server successfully bound to Port " + port + "\n");
                f.channel().closeFuture().sync();
                bound = true;
            } catch (Exception e) {
                Thread.sleep(1000); // 1 seconds waiting time between attempts
                attempts++;
            } finally {
                if (bound) {
                    workerGroup.shutdownGracefully();
                    bossGroup.shutdownGracefully();
                }
            }
        }
    
        long duration = System.currentTimeMillis() - startTime; // Total duration of binding attempts
    
        if (bound) {
            System.out.println("Server successfully bound to Port " + port + " in " + duration / 1000 + " seconds after " + attempts + " attempts.");
        } else {
            System.out.println("Server could not be bound after " + maxAttempts + " attempts.");
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }        

    public static void main(String[] args) throws Exception {
        Thread.sleep(20000);
        System.out.println("****************Main method of LectureStudioServer started****************\n");
        String peersEnvVar = System.getenv("TARGET_PEERS");
        List<String> myPeers = peersEnvVar != null ? Arrays.asList(peersEnvVar.split(",")) : new ArrayList<>();
        for (String peer : myPeers) {
            System.out.println("Data is going to be sent to the container p2p-containerlab-topology-" + peer);
        }
        int port = 8080;
        new LectureStudioServer(port).start();

        Thread.sleep(5000000);
    }

}
