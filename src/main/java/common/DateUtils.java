package common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Utility: Xử lý ngày tháng
 */
public class DateUtils {

    private static final DateTimeFormatter DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DISPLAY_FORMAT);
    }

    public static boolean isExpiringSoon(LocalDate hanSuDung, int thresholdDays) {
        if (hanSuDung == null) return false;
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), hanSuDung);
        return daysLeft <= thresholdDays;
    }

    public static boolean isExpired(LocalDate hanSuDung) {
        if (hanSuDung == null) return false;
        return hanSuDung.isBefore(LocalDate.now()) || hanSuDung.isEqual(LocalDate.now());
    }

    public static long daysUntilExpiry(LocalDate hanSuDung) {
        if (hanSuDung == null) return -1;
        return ChronoUnit.DAYS.between(LocalDate.now(), hanSuDung);
    }
}
