package com.hoangdinh.taphoa.config;

import com.hoangdinh.taphoa.config.localdb.LocalDbPipeResolver;
import com.hoangdinh.taphoa.config.localdb.LocalDbPipeSocketFactory;
import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.pool.HikariPool;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Supplier;

/**
 * Quản lý kết nối tới SQL Server: một pool kết nối (HikariCP) dùng chung cho cả ứng dụng
 * và giao dịch (transaction) gắn với luồng đang chạy.
 *
 * <p>Nếu không cấu hình db.url thì dựng URL tới SQL Server LocalDB: driver mssql-jdbc không hỗ trợ
 * named pipe nên truyền {@link LocalDbPipeSocketFactory} để đọc/ghi qua pipe.
 */
public final class Database {

    private static final int SO_LAN_THU_KET_NOI = 5;

    private static volatile HikariDataSource dataSource;
    /** Kết nối của giao dịch đang mở trên luồng hiện tại (null nếu không trong giao dịch). */
    private static final ThreadLocal<Connection> TX = new ThreadLocal<>();

    private Database() {
    }

    /**
     * Mượn một kết nối. Trong giao dịch thì trả về kết nối của giao dịch (người gọi KHÔNG được đóng);
     * dùng {@link #release(Connection)} thay cho close().
     */
    public static Connection borrow() throws SQLException {
        Connection tx = TX.get();
        return tx != null ? tx : open();
    }

    public static void release(Connection c) throws SQLException {
        if (c != null && c != TX.get()) {
            c.close();
        }
    }

    /** Chạy work trong một giao dịch: thành công thì commit, có lỗi thì rollback toàn bộ. */
    public static <T> T inTransaction(Supplier<T> work) {
        if (TX.get() != null) {
            return work.get();          // đã ở trong giao dịch -> dùng chung
        }
        try (Connection c = open()) {
            c.setAutoCommit(false);
            TX.set(c);
            try {
                T result = work.get();
                c.commit();
                return result;
            } catch (RuntimeException e) {
                c.rollback();
                throw e;
            } finally {
                TX.remove();
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw Jdbc.translate(e);
        }
    }

    public static void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    /**
     * Mở sẵn kết nối ở luồng nền khi chương trình vừa khởi động (lần đầu mất vài giây để khởi động LocalDB),
     * để lúc người dùng bấm Đăng nhập thì kết nối đã sẵn sàng. Lỗi bỏ qua: sẽ báo khi đăng nhập.
     */
    public static void warmUp() {
        Thread t = new Thread(() -> {
            try (Connection c = open()) {
                c.isValid(5);
            } catch (Exception ignored) {
                // báo lỗi cụ thể khi người dùng đăng nhập
            }
        }, "db-warmup");
        t.setDaemon(true);
        t.start();
    }

    /** Đóng pool khi thoát chương trình. */
    public static synchronized void shutdown() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    /**
     * Lấy kết nối từ pool. Khi SQL Server từ chối kết nối (sai mật khẩu, chưa tạo CSDL), đôi khi pipe bị đóng
     * trước khi đọc được thông báo lỗi; thử lại vài lần để lấy đúng nguyên nhân hiển thị cho người dùng.
     */
    private static Connection open() throws SQLException {
        for (int lan = 1; ; lan++) {
            Exception loi;
            try {
                return dataSource().getConnection();
            } catch (HikariPool.PoolInitializationException e) {
                loi = e;   // lần kết nối đầu tiên thất bại; lần gọi sau Hikari sẽ thử lại
            } catch (SQLException e) {
                loi = e;
            }
            if (laPipeBiDong(loi) && lan < SO_LAN_THU_KET_NOI) {
                nghi(200);
                continue;
            }
            Throwable cause = loi instanceof HikariPool.PoolInitializationException && loi.getCause() != null
                    ? loi.getCause() : loi;
            if (laPipeBiDong(cause)) {
                throw new BusinessException(ErrorCode.DB_KET_NOI, ErrorCode.DB_KET_NOI.getMessage()
                        + ": SQL Server từ chối kết nối (kiểm tra db.user, db.password trong app.properties"
                        + " và đã chạy database/taphoa_db.sql chưa)");
            }
            if (cause instanceof SQLException sql) {
                throw sql;          // Jdbc.translate chuyển thành thông báo cụ thể
            }
            throw new BusinessException(ErrorCode.DB_KET_NOI,
                    ErrorCode.DB_KET_NOI.getMessage() + ": " + cause.getMessage());
        }
    }

    private static boolean laPipeBiDong(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof LocalDbPipeSocketFactory.PipeClosedException) {
                return true;
            }
        }
        return false;
    }

    private static void nghi(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static HikariDataSource dataSource() {
        HikariDataSource ds = dataSource;
        if (ds == null) {
            synchronized (Database.class) {
                if (dataSource == null) {
                    dataSource = create();
                }
                ds = dataSource;
            }
        }
        return ds;
    }

    private static HikariDataSource create() {
        try {
            String url = System.getenv().getOrDefault("TAPHOA_DB_URL", AppConfig.get("db.url", ""));
            if (url.isBlank()) {
                url = localDbUrl();
            }
            HikariDataSource ds = new HikariDataSource();
            ds.setPoolName("taphoa-pool");
            ds.setJdbcUrl(url);
            ds.setUsername(AppConfig.get("db.user", "app_user"));
            ds.setPassword(AppConfig.get("db.password", ""));
            ds.setMaximumPoolSize(4);
            ds.setMinimumIdle(1);
            ds.setConnectionTimeout(15_000);
            return ds;
        } catch (RuntimeException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new BusinessException(ErrorCode.DB_KET_NOI,
                    ErrorCode.DB_KET_NOI.getMessage() + ": " + cause.getMessage());
        }
    }

    private static String localDbUrl() {
        String pipe = LocalDbPipeResolver.resolve(AppConfig.get("db.localdb-instance", "MSSQLLocalDB"));
        return "jdbc:sqlserver://localhost;databaseName=" + AppConfig.get("db.name", "taphoa_db")
                + ";encrypt=false;loginTimeout=10"
                // không để driver tự kết nối lại khi lỗi: lần thử lại qua pipe che mất lỗi gốc (vd: chưa tạo CSDL)
                + ";connectRetryCount=0"
                + ";socketFactoryClass=" + LocalDbPipeSocketFactory.class.getName()
                + ";socketFactoryConstructorArg={" + pipe + "}";
    }
}
