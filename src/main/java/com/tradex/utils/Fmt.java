package com.tradex.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public final class Fmt {
    private Fmt() {}

    public static String money(BigDecimal v) {
        // NumberFormat is not thread-safe, so create per call.
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        f.setMinimumFractionDigits(2);
        f.setMaximumFractionDigits(2);
        return (v.signum() < 0 ? "-₹" : "₹") + f.format(v.abs());
    }

    public static String signedMoney(BigDecimal v) { return (v.signum() > 0 ? "+" : "") + money(v); }

    public static String pct(BigDecimal v) { return (v.signum() > 0 ? "+" : "") + v.toPlainString() + "%"; }
}
