package mn.home.kick;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The parts of kick.com's web API the app uses. None need a login: they are what the site's own
 * pages call. Kick changes them now and then, so this is the file to fix when lists stop loading.
 */
final class Kick {
    /** Kick sits behind Cloudflare, which is stricter with clients that do not look like a browser. */
    static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0.1; MiTV4) AppleWebKit/537.36"
            + " (KHTML, like Gecko) Chrome/66.0.3359.158 Safari/537.36";

    /** A live stream, or a channel that may be offline. */
    static final class Stream {
        String slug, name, title = "", category = "", thumbnail, avatar, playback;
        int viewers, followers;
        boolean live, mature;
        long startedAt;
    }

    private Kick() {
    }

    /** The most watched live streams in one language (ISO 639-1, e.g. "mn"), or in all of them. */
    static List<Stream> live(String language, int limit) throws Exception {
        String url = "https://web.kick.com/api/v1/livestreams?sort=viewer_count_desc&limit=" + limit
                + (language == null ? "" : "&language=" + language);
        JSONArray items = new JSONObject(get(url)).getJSONObject("data").getJSONArray("livestreams");
        List<Stream> streams = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.getJSONObject(i), channel = o.getJSONObject("channel");
            Stream s = new Stream();
            s.slug = channel.getString("slug");
            s.name = text(channel, "username", s.slug);
            s.avatar = text(channel, "profile_pic", null);
            s.title = text(o, "title", "");
            s.viewers = o.optInt("viewer_count");
            JSONObject category = o.optJSONObject("category");
            if (category != null) s.category = text(category, "name", "");
            JSONObject thumb = o.optJSONObject("thumbnail");
            if (thumb != null) s.thumbnail = smallest(text(thumb, "srcset", ""), 640, text(thumb, "src", null));
            s.startedAt = time(text(o, "start_time", null));
            s.mature = o.optBoolean("is_mature");
            s.live = true;
            streams.add(s);
        }
        return streams;
    }

    /** One channel and its current stream, if any. The playback URL expires within minutes. */
    static Stream channel(String slug) throws Exception {
        JSONObject o = new JSONObject(get("https://kick.com/api/v2/channels/" + URLEncoder.encode(slug, "UTF-8")));
        Stream s = new Stream();
        s.slug = text(o, "slug", slug);
        JSONObject user = o.optJSONObject("user");
        s.name = user == null ? s.slug : text(user, "username", s.slug);
        s.avatar = user == null ? null : text(user, "profile_pic", null);
        s.followers = o.optInt("followers_count");
        JSONObject stream = o.optJSONObject("livestream");
        if (stream != null && stream.optBoolean("is_live", true)) {
            s.live = true;
            s.playback = text(o, "playback_url", null);
            s.title = text(stream, "session_title", "");
            s.viewers = stream.optInt("viewer_count");
            JSONArray categories = stream.optJSONArray("categories");
            if (categories != null && categories.length() > 0) {
                s.category = text(categories.getJSONObject(0), "name", "");
            }
            JSONObject thumb = stream.optJSONObject("thumbnail");
            if (thumb != null) s.thumbnail = text(thumb, "url", null);
            s.startedAt = time(text(stream, "start_time", null));
            s.mature = stream.optBoolean("is_mature");
        }
        return s;
    }

    /**
     * Several channels at once: live ones first, most viewers first, then the rest in the given
     * order. A channel that fails to load (renamed, banned) stays in the list as offline, so it can
     * still be removed from favourites; only when every one fails is that an error.
     */
    static List<Stream> channels(List<String> slugs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<Stream>> pending = new ArrayList<>();
            for (String slug : slugs) pending.add(pool.submit(() -> channel(slug)));
            List<Stream> live = new ArrayList<>(), offline = new ArrayList<>();
            Exception error = null;
            int failed = 0;
            for (int i = 0; i < pending.size(); i++) {
                Stream s;
                try {
                    s = pending.get(i).get();
                } catch (Exception e) {
                    error = e;
                    failed++;
                    s = new Stream();
                    s.slug = s.name = slugs.get(i);
                }
                (s.live ? live : offline).add(s);
            }
            if (failed > 0 && failed == slugs.size()) throw error;
            Collections.sort(live, (a, b) -> Integer.compare(b.viewers, a.viewers));
            live.addAll(offline);
            return live;
        } finally {
            pool.shutdownNow();
        }
    }

    /** Channels whose name matches; live ones come with their stream, like a favourite would. */
    static List<Stream> search(String query) throws Exception {
        JSONObject o = new JSONObject(get("https://kick.com/api/search?searched_word="
                + URLEncoder.encode(query, "UTF-8")));
        JSONArray items = o.optJSONArray("channels");
        List<String> live = new ArrayList<>();
        List<Stream> offline = new ArrayList<>();
        for (int i = 0; items != null && i < items.length(); i++) {
            JSONObject c = items.getJSONObject(i);
            String slug = c.getString("slug");
            if (c.optBoolean("isLive")) {
                live.add(slug);
                continue;
            }
            Stream s = new Stream();
            s.slug = slug;
            JSONObject user = c.optJSONObject("user");
            s.name = user == null ? slug : text(user, "username", slug);
            s.avatar = user == null ? null : text(user, "profilePic", null);
            s.followers = c.optInt("followers_count");
            offline.add(s);
        }
        List<Stream> found = live.isEmpty() ? new ArrayList<Stream>() : channels(live);
        found.addAll(offline);
        return found;
    }

    static String get(String url) throws IOException {
        return new String(bytes(url, "application/json"), StandardCharsets.UTF_8);
    }

    static byte[] bytes(String url, String accept) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", USER_AGENT);
        c.setRequestProperty("Accept", accept);
        c.setConnectTimeout(15_000);
        c.setReadTimeout(20_000);
        try {
            int status = c.getResponseCode();
            if (status != 200) throw new IOException("HTTP " + status);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (InputStream in = c.getInputStream()) {
                byte[] buf = new byte[16384];
                for (int n; (n = in.read(buf)) != -1; ) out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            c.disconnect();
        }
    }

    /** org.json reads a JSON null as the string "null"; this keeps it a fallback. */
    private static String text(JSONObject o, String key, String fallback) {
        return o.isNull(key) ? fallback : o.optString(key, fallback);
    }

    /** The smallest picture in a srcset ("url 640w, url 1280w") that is at least minWidth wide. */
    private static String smallest(String srcset, int minWidth, String fallback) {
        String best = fallback;
        int bestWidth = Integer.MAX_VALUE;
        for (String entry : srcset.split(",")) {
            String[] parts = entry.trim().split("\\s+");
            if (parts.length != 2 || !parts[1].endsWith("w")) continue;
            try {
                int width = Integer.parseInt(parts[1].substring(0, parts[1].length() - 1));
                if (width >= minWidth && width < bestWidth) {
                    best = parts[0];
                    bestWidth = width;
                }
            } catch (NumberFormatException ignored) {
                // Not a width descriptor; skip it.
            }
        }
        return best;
    }

    /** Kick's UTC times, as "2026-10-31T04:42:42Z" or "2026-10-31 04:42:42"; 0 if unreadable. */
    private static long time(String value) {
        if (value == null || value.length() < 19) return 0;
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return f.parse(value.substring(0, 19).replace('T', ' ')).getTime();
        } catch (ParseException e) {
            return 0;
        }
    }
}
