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
            // Senden der Datei und Hinzufügen eines ChannelFutureListeners
            ctx.writeAndFlush(new DefaultFileRegion(file, 0, file.length())).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        System.out.println("File sent successfully: " + filePath);
                    } else {
                        System.err.println("Error sending file: " + future.cause());
                    }
                    ctx.close(); // Schließen des Channels nach dem Senden
                }
            });
        } else {
            System.err.println("File not found: " + filePath);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Channel inaktiv. Schließe den Channel.");
        ctx.close(); // Schließt den Channel, wenn er inaktiv wird
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("Ein Fehler ist aufgetreten: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close(); // Schließt den Channel im Fehlerfall
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // Nicht benötigt in diesem Kontext
        throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
    }
}
