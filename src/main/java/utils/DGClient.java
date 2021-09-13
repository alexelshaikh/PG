package utils;

import core.BaseSequence;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class DGClient implements AutoCloseable {

    private static DGClient INSTANCE;
    public static String HOST = "127.0.0.1";
    public static int START_PORT = 6000;
    public static int CHANNELS_COUNT = Runtime.getRuntime().availableProcessors();

    private final ChannelHandler[] handlers;

    public synchronized static DGClient getInstance() {
        if (INSTANCE == null)
            INSTANCE = new DGClient(START_PORT, CHANNELS_COUNT);

        return INSTANCE;
    }

    private DGClient(int startPort, int count) {
        this.handlers = new ChannelHandler[count];
        for (int i = 0; i < count; i++)
            handlers[i] = new ChannelHandler(startPort + i);
    }

    public static void setHost(String host) {
        DGClient.HOST = host;
    }

    public static void setStartPort(int startPort) {
        DGClient.START_PORT = startPort;
    }

    public static void setChannelsCount(int channelsCount) {
        DGClient.CHANNELS_COUNT = channelsCount;
    }

    public boolean isConnected() {
        for (ChannelHandler ch : handlers) {
            if (!ch.isConnected())
                return false;
        }
        return false;
    }

    public int getChannelsCount() {
        return DGClient.CHANNELS_COUNT;
    }

    @Override
    public void close() {
        for (ChannelHandler ch : handlers)
            ch.close();
    }

    public float dg(BaseSequence seq, float temp) {
        return dg(0, seq, temp);
    }

    public float dg(int fromSocketNum, BaseSequence seq, float temp) {
        int safeId = fromSocketNum % this.handlers.length;
        ChannelHandler ch;
        while (!(ch=this.handlers[safeId]).lock.tryLock()) {
            safeId = (safeId + 1) % this.handlers.length;
        }
        try {
            return ch.sendSeqReceiveDgLockFree(seq, temp);
        }
        finally {
            ch.lock.unlock();
        }
    }

    private static class ChannelHandler implements AutoCloseable {

        private static final int MAX_BUFF_LEN = 4;

        private SocketChannel channel;
        private final int port;
        private final Lock lock;
        private final ByteBuffer readBuffer;

        public ChannelHandler(int port) {
            this.port = port;
            this.lock = new ReentrantLock(true);
            this.readBuffer = ByteBuffer.allocate(MAX_BUFF_LEN);
            connect();
        }

        private boolean isConnected() {
            return this.channel != null && this.channel.isConnected();
        }

        private void connect() {
            if (isConnected())
                return;
            try {
                lock.lock();
                channel = SocketChannel.open();
                channel.connect(new InetSocketAddress(HOST, port));
                if (channel.isConnectionPending())
                    channel.finishConnect();

                channel.configureBlocking(true);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                lock.unlock();
            }
        }

        private float readDeltaG() {
            readBuffer.clear();
            int bytesAvailable = -10;
            try{
                bytesAvailable = channel.read(readBuffer);
            } catch (IOException e) {
                closeAndThrow(e);
            }
            if (bytesAvailable <= 0) {
                closeAndThrow("connection closed from server");
            }
            if (bytesAvailable > MAX_BUFF_LEN)
                closeAndThrow("too long message from server");

            readBuffer.flip();
            return parseFloatLE(readBuffer);
        }

        public void send(String msg) {
            try {
                channel.write(ByteBuffer.wrap(msg.getBytes()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {
            try {
                channel.close();
            }
            catch (Exception ignored) {
            }
        }

        private float sendSeqReceiveDgLockFree(BaseSequence seq, float temp) {
            send(seq.toString() + "," + temp);
            return readDeltaG();
        }

        private void closeAndThrow(String msg) {
            close();
            throw new RuntimeException(msg);
        }

        private void closeAndThrow(Exception e) {
            close();
            throw new RuntimeException(e);
        }
    }

    private static float parseFloatLE(ByteBuffer buff) {
        try {
            return buff.order(ByteOrder.LITTLE_ENDIAN).getFloat();
        }
        catch (Exception ignored) {
            return Float.MAX_VALUE;
        }
    }
}
