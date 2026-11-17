package com.hoangdinh.taphoa.config.localdb;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import javax.net.SocketFactory;

/**
 * SocketFactory trả về "socket" đọc/ghi qua named pipe của Windows.
 * mssql-jdbc nhận lớp này qua tham số {@code socketFactoryClass}; {@code socketFactoryConstructorArg}
 * là đường dẫn pipe, ví dụ {@code \\.\pipe\LOCALDB#8D58873B\tsql\query}.
 */
public class LocalDbPipeSocketFactory extends SocketFactory {

    private final String pipePath;

    public LocalDbPipeSocketFactory(String pipePath) {
        this.pipePath = pipePath;
    }

    @Override
    public Socket createSocket() {
        return new PipeSocket(pipePath);
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        Socket s = createSocket();
        s.connect(null);
        return s;
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket(host, port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return createSocket("", port);
    }

    @Override
    public Socket createSocket(InetAddress host, int port, InetAddress localHost, int localPort) throws IOException {
        return createSocket("", port);
    }

    /**
     * SQL Server đã đóng pipe trước khi client đọc hết dữ liệu. Hay gặp khi server từ chối kết nối
     * (sai mật khẩu, chưa có CSDL): server gửi thông báo lỗi rồi ngắt ngay, phần chưa đọc bị mất.
     */
    public static final class PipeClosedException extends IOException {
        PipeClosedException(IOException cause) {
            super(cause.getMessage(), cause);
        }
    }

    /** TDS là giao thức hỏi-đáp tuần tự nên đọc/ghi đồng bộ trên cùng một handle là đủ. */
    private static final class PipeSocket extends Socket {
        private final String path;
        private RandomAccessFile pipe;
        private InputStream in;
        private OutputStream out;
        private volatile int soTimeout;

        PipeSocket(String path) {
            this.path = path;
        }

        @Override
        public void connect(SocketAddress endpoint) throws IOException {
            connect(endpoint, 0);
        }

        @Override
        public void connect(SocketAddress endpoint, int timeout) throws IOException {
            pipe = new RandomAccessFile(path, "rw");
            in = new TimeoutInputStream(new FileInputStream(pipe.getFD()));
            out = new FileOutputStream(pipe.getFD());
        }

        /**
         * Đọc pipe có tôn trọng soTimeout như socket thật. Driver dùng soTimeout rất ngắn để kiểm tra
         * kết nối rảnh trong pool còn sống không; nếu đọc chặn vô hạn thì request sẽ treo.
         * available() trên pipe của Windows dùng PeekNamedPipe nên không chặn.
         */
        private final class TimeoutInputStream extends InputStream {
            private final FileInputStream delegate;

            TimeoutInputStream(FileInputStream delegate) {
                this.delegate = delegate;
            }

            private void awaitData() throws IOException {
                int timeout = soTimeout;
                if (timeout <= 0) {
                    return;
                }
                long deadline = System.nanoTime() + timeout * 1_000_000L;
                while (delegate.available() == 0) {
                    if (System.nanoTime() >= deadline) {
                        throw new SocketTimeoutException("Read timed out");
                    }
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InterruptedIOException();
                    }
                }
            }

            @Override
            public int read() throws IOException {
                awaitData();
                try {
                    return delegate.read();
                } catch (IOException e) {
                    throw new PipeClosedException(e);
                }
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                awaitData();
                try {
                    return delegate.read(b, off, len);
                } catch (IOException e) {
                    throw new PipeClosedException(e);
                }
            }

            @Override
            public int available() throws IOException {
                return delegate.available();
            }
        }

        @Override
        public InputStream getInputStream() {
            return in;
        }

        @Override
        public OutputStream getOutputStream() {
            return out;
        }

        @Override
        public boolean isConnected() {
            return pipe != null;
        }

        @Override
        public boolean isClosed() {
            return pipe == null;
        }

        @Override
        public synchronized void close() throws IOException {
            if (pipe != null) {
                pipe.close();
                pipe = null;
            }
        }

        @Override
        public void setSoTimeout(int timeout) {
            soTimeout = timeout;
        }

        @Override
        public int getSoTimeout() {
            return soTimeout;
        }

        @Override
        public void setTcpNoDelay(boolean on) {
        }

        @Override
        public void setKeepAlive(boolean on) {
        }

        @Override
        public void setSoLinger(boolean on, int linger) {
        }

        @Override
        public void shutdownInput() {
        }

        @Override
        public void shutdownOutput() {
        }
    }
}
