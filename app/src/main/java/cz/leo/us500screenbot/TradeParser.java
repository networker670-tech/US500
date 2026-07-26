package cz.leo.us500screenbot;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TradeParser {
    private static final Pattern TIME_PATTERN = Pattern.compile("(?<!\\d)([01]?\\d|2[0-3])[:.]([0-5]\\d)(?!\\d)");
    private static final Pattern PRICE_PATTERN = Pattern.compile("(?<!\\d)(\\d{4,6}(?:[.,]\\d{1,2})?)(?!\\d)");
    private static final Pattern CLOSE_LABEL_PATTERN = Pattern.compile(
            "(?i)(?:^|\\s)(?:C|CLOSE|ZAV[ŘR]ENO|ZAV[ÍI]RAC[ÍI]|CLOSE PRICE)\\s*[:=]?\\s*(\\d{4,6}(?:[.,]\\d{1,2})?)");

    private TradeParser() {}

    public static final class Zone {
        public final BigDecimal high;
        public final BigDecimal low;
        public Zone(BigDecimal high, BigDecimal low) {
            this.high = high;
            this.low = low;
        }
    }

    public static final class Candle {
        public final String time;
        public final BigDecimal close;
        public Candle(String time, BigDecimal close) {
            this.time = time;
            this.close = close;
        }
    }

    public static Zone parseZone(String raw) {
        String text = normalize(raw);
        BigDecimal high = findKeywordPrice(text,
                "HORNÍ", "HORNI", "UPPER", "HIGH", "TOP", "ZONE HIGH", "ENTRY HIGH");
        BigDecimal low = findKeywordPrice(text,
                "DOLNÍ", "DOLNI", "LOWER", "LOW", "BOTTOM", "ZONE LOW", "ENTRY LOW");

        if (high != null && low != null) {
            return orderedZone(high, low);
        }

        List<BigDecimal> prices = extractPrices(text);
        if (prices.size() < 2) return null;

        // In Discord screenshots the first two unique index-like values are normally the zone.
        return orderedZone(prices.get(0), prices.get(1));
    }

    public static Candle parseCandle(String raw) {
        String text = normalize(raw);
        String time = extractBestTime(text);
        BigDecimal close = extractClose(text);
        if (time == null || close == null) return null;
        return new Candle(time, close);
    }

    public static BigDecimal parseCandleClose(String raw) {
        return extractClose(normalize(raw));
    }

    public static String normalizeTime(String value) {
        if (value == null) return null;
        Matcher m = TIME_PATTERN.matcher(value.trim());
        if (!m.find()) return null;
        int hour = Integer.parseInt(m.group(1));
        return String.format(Locale.US, "%02d:%s", hour, m.group(2));
    }

    public static BigDecimal parseDecimal(String value) {
        if (value == null) return null;
        String cleaned = value.replace(" ", "").replace(',', '.').replaceAll("[^0-9.\\-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.chars().filter(ch -> ch == '.').count() > 1) return null;
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Zone orderedZone(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0 ? new Zone(a, b) : new Zone(b, a);
    }

    private static String normalize(String raw) {
        if (raw == null) return "";
        return raw.replace('\u00A0', ' ')
                .replaceAll("(?<=\\d)\\s+(?=\\d{3}(?:[.,]|\\b))", "")
                .toUpperCase(Locale.ROOT);
    }

    private static BigDecimal findKeywordPrice(String text, String... keywords) {
        for (String keyword : keywords) {
            Pattern p = Pattern.compile("(?i)" + Pattern.quote(keyword) + "[^\\d]{0,24}(\\d{4,6}(?:[.,]\\d{1,2})?)");
            Matcher m = p.matcher(text);
            if (m.find()) return parseDecimal(m.group(1));
        }
        return null;
    }

    private static List<BigDecimal> extractPrices(String text) {
        Matcher matcher = PRICE_PATTERN.matcher(text);
        Set<String> unique = new LinkedHashSet<>();
        while (matcher.find()) {
            BigDecimal value = parseDecimal(matcher.group(1));
            if (isPlausiblePrice(value)) unique.add(value.toPlainString());
        }
        List<BigDecimal> result = new ArrayList<>();
        for (String s : unique) result.add(new BigDecimal(s));
        return result;
    }

    private static String extractBestTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(text);
        List<String> times = new ArrayList<>();
        while (matcher.find()) {
            int hour = Integer.parseInt(matcher.group(1));
            times.add(String.format(Locale.US, "%02d:%s", hour, matcher.group(2)));
        }
        if (times.isEmpty()) return null;
        // The Android status-bar clock is usually read first; chart/crosshair time tends to appear later.
        return times.get(times.size() - 1);
    }

    private static BigDecimal extractClose(String text) {
        Matcher labeled = CLOSE_LABEL_PATTERN.matcher(text);
        BigDecimal last = null;
        while (labeled.find()) {
            BigDecimal value = parseDecimal(labeled.group(1));
            if (isPlausiblePrice(value)) last = value;
        }
        if (last != null) return last;

        // Trading charts often show an OHLC row. If a line contains O/H/L/C, choose its last price.
        for (String line : text.split("\\R")) {
            String upper = line.toUpperCase(Locale.ROOT);
            if (upper.contains("O") && upper.contains("H") && upper.contains("L") && upper.contains("C")) {
                List<BigDecimal> prices = extractPrices(line);
                if (prices.size() >= 4) return prices.get(prices.size() - 1);
            }
        }

        List<BigDecimal> fallback = extractPrices(text);
        if (fallback.isEmpty()) return null;
        // Crosshair/data-window value usually appears after scale labels in OCR order.
        return fallback.get(fallback.size() - 1);
    }

    private static boolean isPlausiblePrice(BigDecimal value) {
        if (value == null) return false;
        return value.compareTo(new BigDecimal("1000")) >= 0
                && value.compareTo(new BigDecimal("100000")) <= 0;
    }
}
