package common;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Utility: Format tiền VNĐ
 */
public class CurrencyFormatter {

    private static final DecimalFormat formatter;

    static {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("vi", "VN"));
        symbols.setGroupingSeparator(',');
        formatter = new DecimalFormat("#,### ₫", symbols);
    }

    public static String format(BigDecimal amount) {
        if (amount == null) return "0 ₫";
        return formatter.format(amount);
    }

    public static String format(long amount) {
        return format(BigDecimal.valueOf(amount));
    }
}
