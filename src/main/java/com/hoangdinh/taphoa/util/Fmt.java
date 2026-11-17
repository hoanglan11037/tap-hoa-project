package com.hoangdinh.taphoa.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import javafx.scene.control.DatePicker;
import javafx.util.StringConverter;

/** Định dạng tiền, ngày giờ kiểu Việt Nam và đọc số tiền người dùng nhập. */
public final class Fmt {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DecimalFormat MONEY;

    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.ROOT);
        s.setGroupingSeparator('.');
        MONEY = new DecimalFormat("#,##0", s);
    }

    private Fmt() {
    }

    public static String money(BigDecimal v) {
        return v == null ? "" : MONEY.format(v) + " đ";
    }

    /** Số tiền dạng rút gọn cho ô thống kê: 186,4 tr₫. */
    public static String moneyShort(BigDecimal v) {
        if (v == null) {
            return "0 đ";
        }
        if (v.compareTo(BigDecimal.valueOf(1_000_000)) >= 0) {
            return String.format(Locale.ROOT, "%.1f tr₫", v.doubleValue() / 1_000_000).replace('.', ',');
        }
        return money(v);
    }

    /**
     * Ô nhập tiền: chỉ nhận chữ số và tự chèn dấu chấm phân cách hàng nghìn khi gõ (1350000 -> 1.350.000).
     */
    public static void moneyField(javafx.scene.control.TextField field) {
        field.textProperty().addListener((o, oldV, newV) -> {
            if (newV == null) {
                return;
            }
            String digits = newV.replaceAll("\\D", "");
            if (digits.length() > 13) {
                digits = digits.substring(0, 13);
            }
            String formatted = digits.isEmpty() ? "" : MONEY.format(new BigDecimal(digits));
            if (!formatted.equals(newV)) {
                field.setText(formatted);
                field.positionCaret(formatted.length());
            }
        });
    }

    /** Ô nhập số nguyên dương (số lượng). */
    public static void intField(javafx.scene.control.TextField field) {
        field.textProperty().addListener((o, oldV, newV) -> {
            if (newV != null && !newV.matches("\\d{0,7}")) {
                field.setText(oldV);
            }
        });
    }

    /** "+12%" / "-5%" so với kỳ trước; kỳ trước bằng 0 thì trả về null. */
    public static String percentChange(BigDecimal now, BigDecimal before) {
        if (before == null || before.signum() == 0 || now == null) {
            return null;
        }
        double pct = (now.doubleValue() - before.doubleValue()) * 100 / before.doubleValue();
        return (pct >= 0 ? "+" : "") + String.format(Locale.ROOT, "%.0f%%", pct);
    }

    public static String moneyPlain(BigDecimal v) {
        return v == null ? "" : MONEY.format(v);
    }

    /** Đọc "1.350.000", "1350000", "1 350 000 đ" -> 1350000; rỗng -> null. */
    public static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String digits = text.replaceAll("[.\\s,đĐ₫]", "");
        if (!digits.matches("\\d{1,13}")) {
            throw new IllegalArgumentException("Số tiền không hợp lệ: " + text);
        }
        return new BigDecimal(digits);
    }

    public static Integer parseInt(String text, String fieldName) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " phải là số nguyên");
        }
    }

    public static String date(LocalDate d) {
        return d == null ? "" : DATE.format(d);
    }

    public static String time(LocalDateTime t) {
        return t == null ? "" : TIME.format(t);
    }

    public static String dateTime(LocalDateTime t) {
        return t == null ? "" : DATE_TIME.format(t);
    }

    /** Hiển thị và nhập ngày theo dd/MM/yyyy. */
    public static void setupDatePicker(DatePicker picker) {
        picker.setPromptText("dd/mm/yyyy");
        picker.setConverter(new StringConverter<>() {
            @Override
            public String toString(LocalDate d) {
                return date(d);
            }

            @Override
            public LocalDate fromString(String s) {
                if (s == null || s.isBlank()) {
                    return null;
                }
                try {
                    return LocalDate.parse(s.trim(), DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        });
    }

    public static String initials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0].substring(0, 1);
        String last = parts.length > 1 ? parts[parts.length - 1].substring(0, 1) : "";
        return (first + last).toUpperCase(Locale.ROOT);
    }

    public static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
