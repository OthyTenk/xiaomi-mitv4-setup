package mn.home.tsagagaar;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * Ulaanbaatar weather for a 1080p TV: current conditions, air quality, the next 24 hours and
 * 7 days on one screen. Data comes from Open-Meteo; OK on the remote refreshes it.
 */
public class MainActivity extends Activity {
    private static final String COORDS = "latitude=47.9077&longitude=106.8832";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast?" + COORDS
            + "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,weather_code,wind_speed_10m"
            + "&hourly=temperature_2m,weather_code,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max"
            + "&wind_speed_unit=ms&timezone=Asia%2FUlaanbaatar&forecast_days=7";
    private static final String AIR_URL = "https://air-quality-api.open-meteo.com/v1/air-quality?" + COORDS
            + "&current=pm2_5&hourly=pm2_5&past_hours=12&forecast_hours=1&timezone=Asia%2FUlaanbaatar";
    private static final TimeZone TZ = TimeZone.getTimeZone("Asia/Ulaanbaatar");
    private static final long REFRESH_MS = 30 * 60_000L;
    private static final long RETRY_MS = 2 * 60_000L;
    private static final String[] WEEKDAYS = {"", "Ням", "Даваа", "Мягмар", "Лхагва", "Пүрэв", "Баасан", "Бямба"};
    private static final int MUTED = 0xFFB8C7DD;
    // EPA PM2.5 breakpoints (2024 revision) and the AQI ranges they map to.
    private static final double[] PM_LO = {0.0, 9.1, 35.5, 55.5, 125.5, 225.5};
    private static final double[] PM_HI = {9.0, 35.4, 55.4, 125.4, 225.4, 325.4};
    private static final int[] AQI_LO = {0, 51, 101, 151, 201, 301};
    private static final int[] AQI_HI = {50, 100, 150, 200, 300, 500};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences cache;
    private SSLSocketFactory sslFactory;
    private boolean loading;
    private long nextRefresh;

    private LinearLayout root, hourly, daily, aqi;
    private ImageButton refreshButton;
    private ObjectAnimator spin;
    private ImageView icon;
    private TextView clock, date, temp, desc, details, aqiValue, aqiUnit, aqiLabel, footer;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateClock();
            if (System.currentTimeMillis() >= nextRefresh) refresh();
            handler.postDelayed(this, 20_000L);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        root = (LinearLayout) findViewById(R.id.root);
        hourly = (LinearLayout) findViewById(R.id.hourly);
        daily = (LinearLayout) findViewById(R.id.daily);
        aqi = (LinearLayout) findViewById(R.id.aqi);
        clock = (TextView) findViewById(R.id.clock);
        date = (TextView) findViewById(R.id.date);
        icon = (ImageView) findViewById(R.id.icon);
        temp = (TextView) findViewById(R.id.temp);
        desc = (TextView) findViewById(R.id.desc);
        details = (TextView) findViewById(R.id.details);
        aqiValue = (TextView) findViewById(R.id.aqi_value);
        aqiUnit = (TextView) findViewById(R.id.aqi_unit);
        aqiLabel = (TextView) findViewById(R.id.aqi_label);
        footer = (TextView) findViewById(R.id.footer);
        aqi.setBackground(rounded(0x33FFFFFF));

        // The only focusable view, so the remote's OK lands on it from the start.
        refreshButton = (ImageButton) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(v -> refresh());
        refreshButton.requestFocus();
        spin = ObjectAnimator.ofFloat(refreshButton, View.ROTATION, 0f, 360f);
        spin.setDuration(900);
        spin.setRepeatCount(ValueAnimator.INFINITE);
        spin.setInterpolator(new LinearInterpolator());

        cache = getSharedPreferences("weather", MODE_PRIVATE);
        long savedAt = cache.getLong("at", 0);
        nextRefresh = savedAt + REFRESH_MS;
        String forecast = cache.getString("forecast", null);
        if (forecast != null) render(forecast, cache.getString("air", null), savedAt);
        else desc.setText("Ачааллаж байна…");
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            refresh();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void refresh() {
        if (loading) return;
        loading = true;
        spin.start();
        footer.setText("Шинэчилж байна…");
        new Thread(() -> {
            String forecast = null, air = null;
            Exception error = null;
            try {
                forecast = get(FORECAST_URL);
                try {
                    air = get(AIR_URL);
                } catch (Exception e) {
                    // Keep showing the last air-quality reading rather than none.
                    air = cache.getString("air", null);
                }
            } catch (Exception e) {
                error = e;
            }
            String f = forecast, a = air;
            Exception err = error;
            handler.post(() -> onFetched(f, a, err));
        }).start();
    }

    private void onFetched(String forecast, String air, Exception error) {
        loading = false;
        spin.cancel();
        refreshButton.setRotation(0f);
        long now = System.currentTimeMillis();
        if (forecast == null) {
            nextRefresh = now + RETRY_MS;
            long savedAt = cache.getLong("at", 0);
            footer.setText("Сүлжээний алдаа (" + (error == null ? "?" : error.getClass().getSimpleName()) + ")"
                    + (savedAt > 0 ? " · сүүлд шинэчилсэн " + hhmm(savedAt) : ""));
            return;
        }
        nextRefresh = now + REFRESH_MS;
        cache.edit().putString("forecast", forecast).putString("air", air).putLong("at", now).apply();
        render(forecast, air, now);
    }

    private void render(String forecastJson, String airJson, long fetchedAt) {
        try {
            JSONObject forecast = new JSONObject(forecastJson);
            JSONObject current = forecast.getJSONObject("current");
            JSONObject days = forecast.getJSONObject("daily");
            boolean isDay = current.optInt("is_day", 1) == 1;
            int code = current.optInt("weather_code", -1);

            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, isDay
                    ? new int[]{0xFF173A6B, 0xFF2D649F} : new int[]{0xFF0A1428, 0xFF1B2D52}));
            icon.setImageDrawable(new WeatherIcon(WeatherIcon.forCode(code, isDay)));
            temp.setText(degrees(current.getDouble("temperature_2m")));
            desc.setText(describe(code));
            details.setText("Мэдрэгдэх " + degrees(current.getDouble("apparent_temperature"))
                    + "\nСалхи " + Math.round(current.getDouble("wind_speed_10m")) + " м/с"
                    + "\nЧийгшил " + current.getInt("relative_humidity_2m") + "%"
                    + "\nӨдөр " + degrees(days.getJSONArray("temperature_2m_max").getDouble(0))
                    + " · Шөнө " + degrees(days.getJSONArray("temperature_2m_min").getDouble(0)));
            renderHourly(forecast.getJSONObject("hourly"), current.getString("time"));
            renderDaily(days);
            renderAir(airJson);
            footer.setText("Шинэчилсэн " + hhmm(fetchedAt) + " · Open-Meteo · OK товч: шинэчлэх");
        } catch (Exception e) {
            footer.setText("Мэдээг уншиж чадсангүй: " + e.getMessage());
        }
    }

    /** Every third hour, starting with the next one: 8 cells cover the coming day. */
    private void renderHourly(JSONObject h, String currentTime) throws Exception {
        JSONArray times = h.getJSONArray("time"), temps = h.getJSONArray("temperature_2m"),
                codes = h.getJSONArray("weather_code"), isDay = h.getJSONArray("is_day");
        String thisHour = currentTime.substring(0, 13) + ":00";
        int first = 0;
        for (int i = 0; i < times.length(); i++) {
            if (times.getString(i).equals(thisHour)) {
                first = i + 1;
                break;
            }
        }
        hourly.removeAllViews();
        for (int n = 0, i = first; n < 8 && i < times.length(); n++, i += 3) {
            LinearLayout cell = column(false);
            cell.addView(label(times.getString(i).substring(11, 16), 15, MUTED, false));
            cell.addView(image(WeatherIcon.forCode(codes.getInt(i), isDay.getInt(i) == 1), 34));
            cell.addView(label(degrees(temps.getDouble(i)), 19, Color.WHITE, true));
            hourly.addView(cell, weighted());
        }
    }

    private void renderDaily(JSONObject d) throws Exception {
        JSONArray times = d.getJSONArray("time"), codes = d.getJSONArray("weather_code"),
                max = d.getJSONArray("temperature_2m_max"), min = d.getJSONArray("temperature_2m_min"),
                rain = d.getJSONArray("precipitation_probability_max");
        SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        iso.setTimeZone(TZ);
        Calendar cal = Calendar.getInstance(TZ);
        daily.removeAllViews();
        for (int i = 0; i < times.length() && i < 7; i++) {
            String day = times.getString(i);
            cal.setTime(iso.parse(day));
            String name = i == 0 ? "Өнөөдөр" : i == 1 ? "Маргааш" : WEEKDAYS[cal.get(Calendar.DAY_OF_WEEK)];
            LinearLayout cell = column(true);
            if (i == 0) cell.setBackground(rounded(0x40FFFFFF));
            cell.addView(label(name, 18, Color.WHITE, true));
            cell.addView(label(day.substring(5), 14, MUTED, false));
            cell.addView(image(WeatherIcon.forCode(codes.getInt(i), true), 46));
            cell.addView(label(degrees(max.getDouble(i)) + " / " + degrees(min.getDouble(i)), 20, Color.WHITE, true));
            cell.addView(label("Тунадас " + (rain.isNull(i) ? 0 : rain.getInt(i)) + "%", 14, 0xFF9FC5E8, false));
            daily.addView(cell, weighted());
        }
    }

    /**
     * US AQI from the PM2.5 NowCast, coloured the way AirNow colours it. Open-Meteo's own us_aqi
     * uses a 24-hour mean, which still reads "moderate" hours after the evening smog has cleared.
     */
    private void renderAir(String airJson) {
        double pm25 = Double.NaN;
        try {
            JSONObject air = new JSONObject(airJson);
            String now = air.getJSONObject("current").getString("time");
            JSONArray times = air.getJSONObject("hourly").getJSONArray("time");
            JSONArray values = air.getJSONObject("hourly").getJSONArray("pm2_5");
            double[] newestFirst = new double[12];
            int n = 0;
            for (int i = times.length() - 1; i >= 0 && n < newestFirst.length; i--) {
                if (times.getString(i).compareTo(now) > 0) continue;
                newestFirst[n++] = values.isNull(i) ? Double.NaN : values.getDouble(i);
            }
            pm25 = nowCast(Arrays.copyOf(newestFirst, n));
        } catch (Exception ignored) {
            // No reading yet: the card shows a dash.
        }
        int index = Double.isNaN(pm25) ? -1 : aqi(pm25);
        int bg, fg = Color.WHITE;
        String level;
        if (index < 0) {
            bg = 0x33FFFFFF;
            level = "Мэдээ алга";
        } else if (index <= 50) {
            bg = 0xFF2E9E4F;
            level = "Сайн";
        } else if (index <= 100) {
            bg = 0xFFE3C21B;
            fg = 0xFF1A1A1A;
            level = "Дунд зэрэг";
        } else if (index <= 150) {
            bg = 0xFFEF7D1A;
            level = "Мэдрэг хүмүүст\nэрүүл бус";
        } else if (index <= 200) {
            bg = 0xFFD5333B;
            level = "Эрүүл бус";
        } else if (index <= 300) {
            bg = 0xFF8E3F97;
            level = "Маш эрүүл бус";
        } else {
            bg = 0xFF7E0023;
            level = "Аюултай";
        }
        aqi.setBackground(rounded(bg));
        aqiValue.setText(index < 0 ? "—" : String.valueOf(index));
        aqiUnit.setText(index < 0 ? "AQI" : "AQI · PM2.5 " + Math.round(pm25) + " мкг/м³");
        aqiLabel.setText(level);
        for (int i = 0; i < aqi.getChildCount(); i++) ((TextView) aqi.getChildAt(i)).setTextColor(fg);
    }

    /** EPA NowCast of hourly PM2.5, newest reading first; NaN without two of the last three hours. */
    private static double nowCast(double[] c) {
        int recent = 0;
        for (int i = 0; i < Math.min(3, c.length); i++) if (!Double.isNaN(c[i])) recent++;
        if (recent < 2) return Double.NaN;
        double min = Double.MAX_VALUE, max = 0;
        for (double v : c) {
            if (Double.isNaN(v)) continue;
            min = Math.min(min, v);
            max = Math.max(max, v);
        }
        double weight = max > 0 ? Math.max(min / max, 0.5) : 1, factor = 1, sum = 0, weights = 0;
        for (double v : c) {
            if (!Double.isNaN(v)) {
                sum += factor * v;
                weights += factor;
            }
            factor *= weight;
        }
        return sum / weights;
    }

    private static int aqi(double pm25) {
        double c = Math.floor(pm25 * 10) / 10;  // EPA truncates to one decimal
        for (int i = 0; i < PM_HI.length; i++) {
            if (c <= PM_HI[i]) {
                return (int) Math.round((AQI_HI[i] - AQI_LO[i]) / (PM_HI[i] - PM_LO[i]) * (c - PM_LO[i]) + AQI_LO[i]);
            }
        }
        return 500;
    }

    private void updateClock() {
        Calendar now = Calendar.getInstance(TZ);
        clock.setText(String.format(Locale.US, "%02d:%02d", now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE)));
        date.setText((now.get(Calendar.MONTH) + 1) + "-р сарын " + now.get(Calendar.DAY_OF_MONTH) + ", "
                + WEEKDAYS[now.get(Calendar.DAY_OF_WEEK)] + " гараг");
    }

    private TextView label(String text, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private ImageView image(int kind, int sizeDp) {
        ImageView v = new ImageView(this);
        v.setImageDrawable(new WeatherIcon(kind));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    private LinearLayout column(boolean card) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        if (card) l.setBackground(rounded(0x1FFFFFFF));
        return l;
    }

    private LinearLayout.LayoutParams weighted() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        p.setMargins(dp(5), 0, dp(5), 0);
        return p;
    }

    private GradientDrawable rounded(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(14));
        return g;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String hhmm(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm", Locale.US);
        f.setTimeZone(TZ);
        return f.format(new Date(millis));
    }

    /** Whole degrees with a real minus sign. */
    private static String degrees(double celsius) {
        long v = Math.round(celsius);
        return (v < 0 ? "−" + -v : String.valueOf(v)) + "°";
    }

    /** WMO weather interpretation codes, as Open-Meteo reports them. */
    private static String describe(int code) {
        switch (code) {
            case 0: return "Цэлмэг";
            case 1: return "Голдуу цэлмэг";
            case 2: return "Багавтар үүлтэй";
            case 3: return "Үүлэрхэг";
            case 45: case 48: return "Манантай";
            case 51: case 53: case 55: return "Шиврээ бороо";
            case 56: case 57: return "Хүйтэн шиврээ";
            case 61: return "Бага зэргийн бороо";
            case 63: return "Бороо";
            case 65: return "Хүчтэй бороо";
            case 66: case 67: return "Хүйтэн бороо";
            case 71: return "Бага зэргийн цас";
            case 73: return "Цас";
            case 75: return "Их цас";
            case 77: return "Цасан ширхэг";
            case 80: case 81: return "Түр зуурын бороо";
            case 82: return "Түр зуурын хүчтэй бороо";
            case 85: return "Түр зуурын цас";
            case 86: return "Түр зуурын их цас";
            case 95: return "Аянга цахилгаантай";
            case 96: case 99: return "Мөндөртэй аянга";
            default: return "";
        }
    }

    private String get(String url) throws Exception {
        try {
            return fetch(url);
        } catch (SSLException e) {
            // Certificate checks can still fail on firmware this old; the data is public anyway.
            return fetch("http://" + url.substring("https://".length()));
        }
    }

    private String fetch(String url) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        if (c instanceof HttpsURLConnection) ((HttpsURLConnection) c).setSSLSocketFactory(sslFactory());
        c.setConnectTimeout(15_000);
        c.setReadTimeout(20_000);
        try {
            int status = c.getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[8192];
                for (int n; (n = in.read(buf)) != -1; ) out.write(buf, 0, n);
            }
            return out.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    /** Android 6 lacks the Let's Encrypt roots Open-Meteo chains to, so the bundled ones are trusted too. */
    private synchronized SSLSocketFactory sslFactory() throws Exception {
        if (sslFactory != null) return sslFactory;
        KeyStore bundled = KeyStore.getInstance(KeyStore.getDefaultType());
        bundled.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        int[] roots = {R.raw.isrg_root_x1, R.raw.isrg_root_x2};
        for (int i = 0; i < roots.length; i++) {
            try (InputStream in = getResources().openRawResource(roots[i])) {
                bundled.setCertificateEntry("isrg" + i, cf.generateCertificate(in));
            }
        }
        X509TrustManager system = trustManager(null), extra = trustManager(bundled);
        X509TrustManager either = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                system.checkClientTrusted(chain, authType);
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                try {
                    system.checkServerTrusted(chain, authType);
                } catch (CertificateException e) {
                    extra.checkServerTrusted(chain, authType);
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return system.getAcceptedIssuers();
            }
        };
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new TrustManager[]{either}, null);
        return sslFactory = context.getSocketFactory();
    }

    private static X509TrustManager trustManager(KeyStore store) throws Exception {
        TrustManagerFactory f = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        f.init(store);
        for (TrustManager tm : f.getTrustManagers()) {
            if (tm instanceof X509TrustManager) return (X509TrustManager) tm;
        }
        throw new IllegalStateException("no X509TrustManager");
    }
}
