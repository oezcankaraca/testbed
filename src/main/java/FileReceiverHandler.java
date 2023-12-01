import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileReceiverHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private FileOutputStream fileOutputStream;
    private File file;
    private long totalReceivedBytes = 0; // Variable zum Zählen der insgesamt empfangenen Bytes

    public FileReceiverHandler(String outputPath) {
        this.file = new File(outputPath);
        System.out.println("FileReceiverHandler initialized. Output path: " + outputPath);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("File created: " + file.getAbsolutePath());
            }
            fileOutputStream = new FileOutputStream(file);
            System.out.println("File output stream opened for " + file.getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error while opening file output stream: " + e.getMessage());
            ctx.close();
            e.printStackTrace();
        }
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        try {
            byte[] bytes = new byte[msg.readableBytes()];
            msg.readBytes(bytes);
            fileOutputStream.write(bytes);
            totalReceivedBytes += bytes.length; // Aktualisieren der Gesamtanzahl der empfangenen Bytes
        } catch (IOException e) {
            System.err.println("Error while writing to file: " + e.getMessage());
            ctx.close();
            e.printStackTrace();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
                System.out.println("File output stream closed.");
                System.out.println("Total received bytes: " + totalReceivedBytes); // Gesamtzahl der empfangenen Bytes ausgeben
            }
        } catch (IOException e) {
            System.err.println("Error while closing file output stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (fileOutputStream != null) {
            try {
                fileOutputStream.close();
                System.out.println("File output stream closed due to an exception.");
            } catch (IOException e) {
                System.err.println("Error while closing file output stream after an exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        System.err.println("An exception occurred: " + cause.getMessage());
        ctx.close();
        cause.printStackTrace();
    }
}
