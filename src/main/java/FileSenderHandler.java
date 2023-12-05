import io.netty.channel.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;

public class FileSenderHandler extends SimpleChannelInboundHandler<Channel> {

    private final String filePath;

    public FileSenderHandler(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        File file = new File(filePath);
        ctx.writeAndFlush(new ChunkedFile(file)).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Channel msg) throws Exception {
        // Nichts zu tun hier
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
