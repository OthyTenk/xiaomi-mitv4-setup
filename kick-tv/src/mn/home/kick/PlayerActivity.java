package mn.home.kick;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.VideoSize;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A live stream, full screen. Left and right switch to the neighbouring live channel of the tab it
 * was opened from, up and down step through the qualities, OK shows the info bar, Menu (or holding
 * OK) toggles the channel as a favourite. A dropped stream is retried with a fresh playback URL,
 * since Kick's expire within minutes; an offline channel is checked again every minute.
 */
public class PlayerActivity extends Activity {
    static final String EXTRA_SLUGS = "slugs", EXTRA_INDEX = "index", EXTRA_SLUG = "slug";
    private static final long INFO_MS = 6_000L;
    private static final long OFFLINE_RECHECK_MS = 60_000L;
    private static final long[] RETRY_MS = {2_000L, 5_000L, 10_000L, 30_000L};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String[] slugs;
    private int index, failures, request;
    /** The chosen video height, or -1 for automatic. Kept while switching channels. */
    private int quality = -1;
    private boolean longPress;
    private Kick.Stream stream;
    private ExoPlayer player;
    private SurfaceView surface;
    private View info;
    private ProgressBar spinner;
    private TextView notice, live, name, star, position, title, meta;
    private Toast toast;

    private final Runnable hideInfo = () -> info.animate().alpha(0f).setDuration(300);
    private final Runnable reopen = () -> open(index);

    private final Player.Listener listener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int state) {
            spinner.setVisibility(state == Player.STATE_BUFFERING ? View.VISIBLE : View.GONE);
            if (state == Player.STATE_READY) {
                failures = 0;
                notice.setVisibility(View.GONE);
            } else if (state == Player.STATE_ENDED) {
                showNotice("Дамжуулалт дууслаа");
                handler.postDelayed(reopen, OFFLINE_RECHECK_MS);
            }
        }

        @Override
        public void onPlayerError(PlaybackException e) {
            if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
                // Paused or stalled past what the playlist still holds: jump back to live.
                player.seekToDefaultPosition();
                player.prepare();
                return;
            }
            retry("Холболт тасарлаа (" + e.getErrorCodeName() + ")");
        }

        @Override
        public void onVideoSizeChanged(VideoSize size) {
            fit(size);
        }

        @Override
        public void onTracksChanged(Tracks tracks) {
            render();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Watching is not idling: keep the screen, and Xiaomi's screensaver, off.
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.player);
        surface = (SurfaceView) findViewById(R.id.surface);
        spinner = (ProgressBar) findViewById(R.id.spinner);
        notice = (TextView) findViewById(R.id.notice);
        info = findViewById(R.id.info);
        live = (TextView) findViewById(R.id.live);
        name = (TextView) findViewById(R.id.name);
        star = (TextView) findViewById(R.id.star);
        position = (TextView) findViewById(R.id.position);
        title = (TextView) findViewById(R.id.title);
        meta = (TextView) findViewById(R.id.meta);

        slugs = getIntent().getStringArrayExtra(EXTRA_SLUGS);
        String slug = getIntent().getStringExtra(EXTRA_SLUG);
        if ((slugs == null || slugs.length == 0) && slug != null) slugs = new String[]{slug};
        if (slugs == null || slugs.length == 0) {
            finish();
            return;
        }
        index = Math.min(Math.max(0, getIntent().getIntExtra(EXTRA_INDEX, 0)), slugs.length - 1);
    }

    // Android 6 has no multi-window, so the player only needs to live while the activity is in front.
    @Override
    protected void onResume() {
        super.onResume();
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new HlsMediaSource.Factory(new DefaultHttpDataSource.Factory()
                        .setUserAgent(Kick.USER_AGENT)
                        .setConnectTimeoutMs(15_000)
                        .setReadTimeoutMs(15_000)))
                // Start near 720p60 rather than at the lowest rung; the Wi-Fi here manages it.
                .setBandwidthMeter(new DefaultBandwidthMeter.Builder(this).setInitialBitrateEstimate(4_000_000).build())
                .build();
        player.setAudioAttributes(AudioAttributes.DEFAULT, true);
        player.setVideoSurfaceView(surface);
        player.addListener(listener);
        applyQuality();
        open(index);
    }

    @Override
    protected void onPause() {
        super.onPause();
        request++;
        if (player != null) {
            // This TV's decoder is slow to let go, so release() can report a timeout error; it must
            // not reach the listener, which would schedule a reconnect after the player is gone.
            player.removeListener(listener);
            player.release();
            player = null;
        }
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                if (slugs.length > 1) open(index - 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_CHANNEL_UP:
                if (slugs.length > 1) open(index + 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                stepQuality(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                stepQuality(1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (event.getRepeatCount() == 0) {
                    longPress = false;
                    event.startTracking();
                }
                return true;
            case KeyEvent.KEYCODE_MENU:
                toggleFavorite();
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                togglePause();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            longPress = true;
            toggleFavorite();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            if (!longPress) toggleInfo();
            longPress = false;
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    /** Fetches the channel for a fresh playback URL, then plays it. */
    private void open(int i) {
        if (player == null) return;
        index = (i % slugs.length + slugs.length) % slugs.length;
        handler.removeCallbacks(reopen);
        String slug = slugs[index];
        if (stream == null || !slug.equals(stream.slug)) {
            stream = null;
            failures = 0;
        }
        player.stop();
        player.clearMediaItems();
        spinner.setVisibility(View.VISIBLE);
        notice.setVisibility(View.GONE);
        render();
        showInfo();
        int req = ++request;
        new Thread(() -> {
            Kick.Stream s = null;
            Exception error = null;
            try {
                s = Kick.channel(slug);
            } catch (Exception e) {
                error = e;
            }
            Kick.Stream result = s;
            Exception err = error;
            handler.post(() -> {
                if (req == request && player != null) onChannel(result, err);
            });
        }).start();
    }

    private void onChannel(Kick.Stream s, Exception error) {
        if (s == null) {
            retry("Kick-тэй холбогдож чадсангүй (" + error.getClass().getSimpleName() + ")");
            return;
        }
        stream = s;
        render();
        if (!s.live || s.playback == null) {
            spinner.setVisibility(View.GONE);
            showNotice(s.name + " одоо шууд дамжуулалт хийхгүй байна"
                    + (slugs.length > 1 ? "\n◀ ▶ товчоор өөр суваг сонгоно уу" : ""));
            handler.postDelayed(reopen, OFFLINE_RECHECK_MS);
            return;
        }
        player.setMediaItem(MediaItem.fromUri(s.playback));
        player.prepare();
        player.setPlayWhenReady(true);
    }

    private void retry(String why) {
        long delay = RETRY_MS[Math.min(failures, RETRY_MS.length - 1)];
        failures++;
        spinner.setVisibility(View.GONE);
        showNotice(why + "\n" + (delay / 1000) + " секундын дараа дахин оролдоно");
        handler.removeCallbacks(reopen);
        handler.postDelayed(reopen, delay);
    }

    /** Letterboxes the picture instead of stretching a stream that is not 16:9. */
    private void fit(VideoSize size) {
        View parent = (View) surface.getParent();
        int w = parent.getWidth(), h = parent.getHeight();
        if (size.width == 0 || size.height == 0 || w == 0 || h == 0) return;
        float aspect = size.width * size.pixelWidthHeightRatio / size.height;
        if (w > h * aspect) w = Math.round(h * aspect);
        else h = Math.round(w / aspect);
        FrameLayout.LayoutParams p = (FrameLayout.LayoutParams) surface.getLayoutParams();
        if (p.width == w && p.height == h) return;
        p.width = w;
        p.height = h;
        p.gravity = Gravity.CENTER;
        surface.setLayoutParams(p);
    }

    /** Steps through automatic and every height the stream offers, highest first. */
    private void stepQuality(int step) {
        List<Integer> options = new ArrayList<>();
        options.add(-1);
        options.addAll(heights());
        if (options.size() == 1) return;
        int at = Math.max(0, options.indexOf(quality));
        quality = options.get(Math.max(0, Math.min(options.size() - 1, at + step)));
        applyQuality();
        say("Чанар: " + (quality < 0 ? "автомат" : quality + "p"));
        showInfo();
    }

    private List<Integer> heights() {
        List<Integer> heights = new ArrayList<>();
        for (Tracks.Group group : player.getCurrentTracks().getGroups()) {
            if (group.getType() != C.TRACK_TYPE_VIDEO) continue;
            for (int i = 0; i < group.length; i++) {
                int h = group.getTrackFormat(i).height;
                if (h > 0 && group.isTrackSupported(i) && !heights.contains(h)) heights.add(h);
            }
        }
        Collections.sort(heights, Collections.reverseOrder());
        return heights;
    }

    private void applyQuality() {
        TrackSelectionParameters.Builder p = player.getTrackSelectionParameters().buildUpon()
                .clearVideoSizeConstraints()
                .setMinVideoSize(0, 0);
        if (quality > 0) p.setMaxVideoSize(Integer.MAX_VALUE, quality).setMinVideoSize(0, quality);
        player.setTrackSelectionParameters(p.build());
    }

    private void toggleFavorite() {
        String slug = slugs[index];
        String who = stream != null ? stream.name : slug;
        boolean added = Favorites.toggle(this, slug);
        say(added ? "★ " + who + " дуртайд нэмэгдлээ" : who + " дуртайгаас хасагдлаа");
        render();
        showInfo();
    }

    private void togglePause() {
        boolean play = !player.getPlayWhenReady();
        player.setPlayWhenReady(play);
        if (play) notice.setVisibility(View.GONE);
        else showNotice("Түр зогссон");
    }

    private void render() {
        Kick.Stream s = stream;
        String slug = slugs[index];
        name.setText(s != null ? s.name : slug);
        star.setVisibility(Favorites.get(this).contains(slug) ? View.VISIBLE : View.GONE);
        live.setVisibility(s != null && s.live ? View.VISIBLE : View.GONE);
        position.setText(slugs.length > 1 ? (index + 1) + " / " + slugs.length : "");
        title.setText(s != null ? s.title : "");
        List<String> parts = new ArrayList<>();
        if (s != null && s.live) {
            parts.add(MainActivity.count(s.viewers) + " үзэгч");
            if (!s.category.isEmpty()) parts.add(s.category);
            if (s.startedAt > 0) {
                long minutes = Math.max(0, (System.currentTimeMillis() - s.startedAt) / 60_000);
                parts.add(minutes < 60 ? minutes + " мин шууд" : minutes / 60 + " цаг " + minutes % 60 + " мин шууд");
            }
        }
        Format video = player == null ? null : player.getVideoFormat();
        if (video != null && video.height > 0) {
            parts.add(video.height + "p" + (video.frameRate >= 49 ? "60" : "") + (quality < 0 ? " (автомат)" : ""));
        }
        meta.setText(join(parts));
    }

    private void showInfo() {
        handler.removeCallbacks(hideInfo);
        info.animate().cancel();
        info.setAlpha(1f);
        handler.postDelayed(hideInfo, INFO_MS);
    }

    private void toggleInfo() {
        if (info.getAlpha() < 0.5f) {
            render();
            showInfo();
        } else {
            handler.removeCallbacks(hideInfo);
            info.animate().alpha(0f).setDuration(200);
        }
    }

    private void showNotice(String text) {
        notice.setText(text);
        notice.setVisibility(View.VISIBLE);
    }

    private void say(String text) {
        if (toast != null) toast.cancel();
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private static String join(List<String> parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() > 0) b.append(" · ");
            b.append(part);
        }
        return b.toString();
    }
}
