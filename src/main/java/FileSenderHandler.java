import io.netty.channel.*;
import java.io.File;

public class FileSenderHandler extends SimpleChannelInboundHandler<Object> {
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
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        // Nicht benötigt in diesem Kontext
        throw new UnsupportedOperationException("Unimplemented method 'channelRead0'");
    }
}