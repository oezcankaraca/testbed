import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileReceiverHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private FileOutputStream fileOutputStream;
    private File file;
    private long totalReceivedBytes = 0; 

    public FileReceiverHandler(String outputPath) {
        this.file = new File(outputPath);
        // System.out.println("FileReceiverHandler initialized. Output path: " + outputPath);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        try {
            if (!file.exists()) {
                file.createNewFile();
                System.out.println("File created: " + file.getAbsolutePath());
            }
            fileOutputStream = new FileOutputStream(file);
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
            totalReceivedBytes += bytes.length; // Update the total received bytes count
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
                System.out.println("Total received bytes: " + totalReceivedBytes); // Print the total received bytes
            }
        } catch (IOException e) {
            System.err.println("Error while closing file output stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        closeFileStream();
        System.err.println("An exception occurred: " + cause.getMessage());
        ctx.close();
        cause.printStackTrace();
    }

    private void closeFileStream() {
        try {
            if (fileOutputStream != null) {
                fileOutputStream.close();
                System.out.println("File output stream closed.");
            }
        } catch (IOException e) {
            System.err.println("Error while closing file output stream: " + e.getMessage());
            e.printStackTrace();
        }
    }
}