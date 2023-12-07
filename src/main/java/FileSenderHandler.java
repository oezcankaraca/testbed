import io.netty.channel.*;
import java.io.File;

public class FileSenderHandler extends SimpleChannelInboundHandler<Object> {
    private final String filePath;

    public FileSenderHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        File file = new File(filePath);
        if (file.exists()) {
            System.out.println("Sending file: " + filePath);
            // Send the file and add a ChannelFutureListener
            ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length())).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws InterruptedException {
                    if (future.isSuccess()) {
                        Thread.sleep(1000);
                        System.out.println("File sent successfully: " + filePath);
                    } else {
                        System.err.println("Error sending file: " + future.cause());
                    }
                    ctx.close(); // Close the channel after sending
                }
            });
        } else {
            System.err.println("File not found: " + filePath);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.close(); // Close the channel when it becomes inactive
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("An error occurred: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Close the channel in case of an error
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // Not needed in this context
        throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
    }
}
