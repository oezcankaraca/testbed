import io.netty.channel.*;
import io.netty.handler.stream.ChunkedFile;

import java.io.File;

public class FileSenderHandler extends SimpleChannelInboundHandler<Channel> {

    private final String filePath;

    public FileSenderHandler(String filePath) {
        this.filePath = filePath;
        System.out.println("FileSenderHandler erstellt mit Dateipfad: " + filePath);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        File file = new File(filePath);
        System.out.println("FileSenderHandler: Verbindung aktiv. Datei wird gesendet: " + file.getAbsolutePath());
        if (file.exists() && !file.isDirectory()) {
            ctx.writeAndFlush(new ChunkedFile(file)).addListener(ChannelFutureListener.CLOSE);
            System.out.println("FileSenderHandler: Datei gesendet und Channel wird geschlossen.");
        } else {
            System.err.println("FileSenderHandler: Datei existiert nicht oder ist ein Verzeichnis.");
            ctx.close();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Channel msg) throws Exception {
        // Nichts zu tun hier
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        System.err.println("FileSenderHandler: Ein Fehler ist aufgetreten: " + cause.getMessage());
        cause.printStackTrace();
        ctx.close();
    }
}
