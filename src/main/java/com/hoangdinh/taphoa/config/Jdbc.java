package com.hoangdinh.taphoa.config;

import com.hoangdinh.taphoa.exception.BusinessException;
import com.hoangdinh.taphoa.exception.ErrorCode;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Hàm tiện ích JDBC dùng cho các lớp DAO: mở PreparedStatement, gán tham số theo thứ tự dấu ?,
 * đọc ResultSet và chuyển SQLException thành thông báo lỗi tiếng Việt.
 */
public final class Jdbc {

    /** Chuyển một dòng của ResultSet thành đối tượng. */
    @FunctionalInterface
    public interface RowMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    // Mã lỗi SQL Server
    private static final int FK_OR_CHECK_VIOLATION = 547;
    private static final int UNIQUE_VIOLATION = 2627;
    private static final int UNIQUE_INDEX_VIOLATION = 2601;
    private static final int LOGIN_FAILED = 18456;
    private static final int CANNOT_OPEN_DATABASE = 4060;

    private Jdbc() {
    }

    public static <T> List<T> query(String sql, RowMapper<T> mapper, Object... params) {
        Connection c = null;
        try {
            c = Database.borrow();
            try (PreparedStatement ps = prepare(c, sql, false, params); ResultSet rs = ps.executeQuery()) {
                List<T> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapper.map(rs));
                }
                return list;
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            close(c);
        }
    }

    public static <T> Optional<T> queryOne(String sql, RowMapper<T> mapper, Object... params) {
        List<T> list = query(sql, mapper, params);
        return list.isEmpty() ? Optional.empty() : Optional.ofNullable(list.get(0));
    }

    /** Câu SELECT trả về một số nguyên (COUNT...). */
    public static long queryLong(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getLong(1), params).orElse(0L);
    }

    /** Câu SELECT trả về một số tiền (SUM...); NULL coi là 0. */
    public static BigDecimal queryMoney(String sql, Object... params) {
        return queryOne(sql, rs -> rs.getBigDecimal(1), params).orElse(BigDecimal.ZERO);
    }

    public static boolean exists(String sql, Object... params) {
        return !query(sql, rs -> Boolean.TRUE, params).isEmpty();
    }

    /** INSERT / UPDATE / DELETE; trả về số dòng bị ảnh hưởng. */
    public static int update(String sql, Object... params) {
        Connection c = null;
        try {
            c = Database.borrow();
            try (PreparedStatement ps = prepare(c, sql, false, params)) {
                return ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            close(c);
        }
    }

    /** INSERT vào bảng có cột IDENTITY; trả về khóa vừa sinh. */
    public static int insert(String sql, Object... params) {
        Connection c = null;
        try {
            c = Database.borrow();
            try (PreparedStatement ps = prepare(c, sql, true, params)) {
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Không lấy được khóa vừa thêm");
                    }
                    return keys.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw translate(e);
        } finally {
            close(c);
        }
    }

    // ---------- Chuẩn bị tham số ----------

    /** Mẫu LIKE '%từ khóa%'; các ký tự đặc biệt của LIKE trong từ khóa được hiểu theo nghĩa đen. */
    public static String like(String keyword) {
        String escaped = keyword.replace("[", "[[]").replace("%", "[%]").replace("_", "[_]");
        return "%" + escaped + "%";
    }

    /** Chuỗi nhập từ form: rỗng / toàn khoảng trắng -> NULL, còn lại cắt khoảng trắng hai đầu. */
    public static String text(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    // ---------- Đọc cột có thể NULL ----------

    public static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int v = rs.getInt(column);
        return rs.wasNull() ? null : v;
    }

    public static LocalDate getDate(ResultSet rs, String column) throws SQLException {
        Date d = rs.getDate(column);
        return d != null ? d.toLocalDate() : null;
    }

    public static LocalDateTime getDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp t = rs.getTimestamp(column);
        return t != null ? t.toLocalDateTime() : null;
    }

    public static <E extends Enum<E>> E getEnum(ResultSet rs, String column, Class<E> type) throws SQLException {
        String s = rs.getString(column);
        return s != null ? Enum.valueOf(type, s.trim()) : null;
    }

    // ---------- Nội bộ ----------

    private static PreparedStatement prepare(Connection c, String sql, boolean returnKeys, Object... params)
            throws SQLException {
        PreparedStatement ps = returnKeys ? c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
                : c.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, toSql(params[i]));
        }
        return ps;
    }

    private static Object toSql(Object v) {
        if (v instanceof Enum<?> e) {
            return e.name();
        }
        if (v instanceof LocalDate d) {
            return Date.valueOf(d);
        }
        if (v instanceof LocalDateTime t) {
            return Timestamp.valueOf(t);
        }
        return v;
    }

    private static void close(Connection c) {
        try {
            Database.release(c);
        } catch (SQLException ignored) {
            // kết nối hỏng sẽ bị pool loại bỏ
        }
    }

    /** Chuyển lỗi SQL thành BusinessException với thông báo người dùng đọc được. */
    static BusinessException translate(SQLException e) {
        int code = e.getErrorCode();
        if (code == FK_OR_CHECK_VIOLATION) {
            return new BusinessException(ErrorCode.DU_LIEU_DANG_DUOC_DUNG);
        }
        if (code == UNIQUE_VIOLATION || code == UNIQUE_INDEX_VIOLATION) {
            return new BusinessException(ErrorCode.DU_LIEU_TRUNG);
        }
        if (code == LOGIN_FAILED) {
            return new BusinessException(ErrorCode.DB_KET_NOI, ErrorCode.DB_KET_NOI.getMessage()
                    + ": sai tài khoản SQL Server (kiểm tra db.user, db.password trong app.properties)");
        }
        if (code == CANNOT_OPEN_DATABASE) {
            return new BusinessException(ErrorCode.DB_KET_NOI, ErrorCode.DB_KET_NOI.getMessage()
                    + ": chưa có cơ sở dữ liệu, hãy chạy file database/taphoa_db.sql trước");
        }
        String state = e.getSQLState();
        if (e instanceof SQLTransientConnectionException || state != null && state.startsWith("08")) {
            return new BusinessException(ErrorCode.DB_KET_NOI, ErrorCode.DB_KET_NOI.getMessage() + ": " + e.getMessage());
        }
        return new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi cơ sở dữ liệu: " + e.getMessage());
    }
}
