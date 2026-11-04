package mn.home.kick;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Kick for a 1080p TV without Google services: favourite channels, Mongolian live streams, the
 * most watched streams worldwide, and channel search, as cards the remote walks through. OK on a
 * card plays it; Menu, or holding OK, adds the channel to favourites or removes it.
 */
public class MainActivity extends Activity {
    private static final String[] TABS = {"Дуртай", "Монгол", "Бүх улс", "Хайх"};
    private static final int FAVORITES = 0, MONGOLIAN = 1, WORLD = 2, SEARCH = 3;
    private static final int COLUMNS = 4;
    private static final long STALE_MS = 60_000L;
    /** Kick's own 18+ flag. Children use this TV, so those streams stay out of the browse tabs. */
    private static final boolean HIDE_MATURE = true;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final TextView[] tabs = new TextView[TABS.length];
    @SuppressWarnings("unchecked")
    private final List<Kick.Stream>[] lists = new List[TABS.length];
    private final long[] loadedAt = new long[TABS.length];
    private final int[] requests = new int[TABS.length];
    private final boolean[] loading = new boolean[TABS.length];
    private final Images images = new Images();
    private Set<String> favorites = new HashSet<>();
    private List<Kick.Stream> shown = Collections.emptyList();
    private int tab = -1, cardWidth;
    private String query = "";
    private boolean rebuilding, focusCards = true;

    private ScrollView scroll;
    private LinearLayout grid;
    private EditText search;
    private TextView status, empty;
    private ProgressBar progress;
    private Toast toast;

    private final Runnable searchAsTyped = () -> runSearch(false);

    /** What a card shows; it is the card's tag, so Menu knows which channel has focus. */
    private static final class Card {
        final Kick.Stream stream;
        final TextView name;

        Card(Kick.Stream stream, TextView name) {
            this.stream = stream;
            this.name = name;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        scroll = (ScrollView) findViewById(R.id.scroll);
        grid = (LinearLayout) findViewById(R.id.grid);
        search = (EditText) findViewById(R.id.search);
        status = (TextView) findViewById(R.id.status);
        empty = (TextView) findViewById(R.id.empty);
        progress = (ProgressBar) findViewById(R.id.progress);
        // 40dp page sides and 6dp inside the scroll view (main.xml), 16dp between cards.
        int width = getResources().getDisplayMetrics().widthPixels - dp(80 + 12);
        cardWidth = (width - dp(16) * (COLUMNS - 1)) / COLUMNS;

        LinearLayout bar = (LinearLayout) findViewById(R.id.tabs);
        for (int i = 0; i < TABS.length; i++) {
            TextView t = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, bar, false);
            t.setId(View.generateViewId());
            t.setText(TABS[i]);
            int index = i;
            // Like Android TV's own rows: moving onto a tab opens it, OK on it reloads it.
            t.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus || rebuilding) return;
                focusCards = false;
                select(index);
            });
            t.setOnClickListener(v -> {
                if (index == SEARCH) search.requestFocus();
                else load(index, true);
            });
            bar.addView(t);
            tabs[i] = t;
        }

        search.setNextFocusUpId(tabs[SEARCH].getId());
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (event == null || event.getAction() == KeyEvent.ACTION_DOWN) runSearch(true);
            return true;
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                handler.removeCallbacks(searchAsTyped);
                if (s.toString().trim().length() >= 2) handler.postDelayed(searchAsTyped, 900);
            }
        });

        favorites = new HashSet<>(Favorites.get(this));
        int first = favorites.isEmpty() ? MONGOLIAN : FAVORITES;
        select(first);
        rebuilding = true;
        tabs[first].requestFocus();
        rebuilding = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The player can add or remove favourites too.
        Set<String> now = new HashSet<>(Favorites.get(this));
        if (!now.equals(favorites)) {
            favorites = now;
            invalidate(FAVORITES);
            if (tab == FAVORITES) load(FAVORITES, true);
            else show(shown);
        } else if (tab >= 0 && !(tab == SEARCH && query.isEmpty())) {
            load(tab, false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        pool.shutdownNow();
        images.cancelPending();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            View focused = getCurrentFocus();
            if (focused != null && focused.getTag() instanceof Card) toggleFavorite((Card) focused.getTag());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void select(int index) {
        if (index == tab) return;
        tab = index;
        for (int i = 0; i < tabs.length; i++) tabs[i].setSelected(i == index);
        scroll.scrollTo(0, 0);
        search.setVisibility(index == SEARCH ? View.VISIBLE : View.GONE);
        if (index == SEARCH && query.isEmpty()) show(Collections.<Kick.Stream>emptyList());
        else load(index, false);
    }

    /** Shows what the tab already has, and fetches it again if that is missing or a minute old. */
    private void load(int index, boolean force) {
        List<Kick.Stream> cached = lists[index];
        if (index == tab) {
            if (cached != null) {
                show(cached);
            } else {
                clearCards();
                empty.setVisibility(View.GONE);
                progress.setVisibility(View.VISIBLE);
            }
        }
        boolean fresh = cached != null && System.currentTimeMillis() - loadedAt[index] < STALE_MS;
        if (!force && (fresh || loading[index])) return;
        if (index == FAVORITES && favorites.isEmpty()) {
            onLoaded(index, ++requests[index], new ArrayList<Kick.Stream>(), null);
            return;
        }
        if (index == tab) status.setText("Ачааллаж байна…");
        loading[index] = true;
        int request = ++requests[index];
        List<String> slugs = Favorites.get(this);
        String q = query;
        pool.execute(() -> {
            List<Kick.Stream> result = null;
            Exception error = null;
            try {
                switch (index) {
                    case FAVORITES:
                        result = Kick.channels(slugs);
                        break;
                    case MONGOLIAN:
                        result = Kick.live("mn", 100);
                        break;
                    case WORLD:
                        result = Kick.live(null, 48);
                        break;
                    default:
                        result = Kick.search(q);
                        break;
                }
            } catch (Exception e) {
                error = e;
            }
            List<Kick.Stream> r = result;
            Exception err = error;
            handler.post(() -> onLoaded(index, request, r, err));
        });
    }

    private void onLoaded(int index, int request, List<Kick.Stream> result, Exception error) {
        if (request != requests[index] || isDestroyed()) return;
        loading[index] = false;
        if (result == null) {
            if (index != tab) return;
            progress.setVisibility(View.GONE);
            status.setText("Сүлжээний алдаа (" + error.getClass().getSimpleName() + ")");
            if (lists[index] == null) {
                clearCards();
                empty.setText("Kick-тэй холбогдож чадсангүй.\nДээрх таб дээр OK дарж дахин оролдоно уу.");
                empty.setVisibility(View.VISIBLE);
            }
            return;
        }
        if (HIDE_MATURE && (index == MONGOLIAN || index == WORLD)) {
            List<Kick.Stream> kept = new ArrayList<>();
            for (Kick.Stream s : result) if (!s.mature) kept.add(s);
            result = kept;
        }
        lists[index] = result;
        loadedAt[index] = System.currentTimeMillis();
        if (index == tab) show(result);
    }

    private void invalidate(int index) {
        lists[index] = null;
        requests[index]++;
        loading[index] = false;
    }

    private void runSearch(boolean submitted) {
        handler.removeCallbacks(searchAsTyped);
        String q = search.getText().toString().trim();
        if (submitted) {
            InputMethodManager ime = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            ime.hideSoftInputFromWindow(search.getWindowToken(), 0);
        }
        if (q.isEmpty() || (q.equals(query) && !submitted)) return;
        query = q;
        // Typing should not lose the keyboard; only a finished search moves on to the results.
        focusCards = submitted;
        invalidate(SEARCH);
        load(SEARCH, true);
    }

    /** Lays the list out as rows of cards, keeping focus on the same channel if it is still there. */
    private void show(List<Kick.Stream> list) {
        shown = list;
        progress.setVisibility(View.GONE);
        View focused = getCurrentFocus();
        String focusedSlug = null;
        int focusedAt = -1;
        if (focused != null && focused.getTag() instanceof Card) {
            focusedSlug = ((Card) focused.getTag()).stream.slug;
            focusedAt = indexOfCard(focused);
        }

        // Removing the focused card hands focus to the first tab; that must not switch tabs.
        rebuilding = true;
        clearCards();
        List<View> cards = new ArrayList<>();
        View again = null;
        LinearLayout row = null;
        int up = tab == SEARCH ? search.getId() : tabs[tab].getId();
        for (int i = 0; i < list.size(); i++) {
            if (i % COLUMNS == 0) {
                row = new LinearLayout(this);
                row.setClipChildren(false);
                LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                p.bottomMargin = dp(18);
                grid.addView(row, p);
            }
            Kick.Stream s = list.get(i);
            View card = card(s);
            if (i < COLUMNS) card.setNextFocusUpId(up);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i % COLUMNS > 0) p.leftMargin = dp(16);
            row.addView(card, p);
            cards.add(card);
            if (s.slug.equals(focusedSlug)) again = card;
        }
        if (focusedSlug != null) {
            if (again == null && !cards.isEmpty()) again = cards.get(Math.min(focusedAt, cards.size() - 1));
            (again != null ? again : tabs[tab]).requestFocus();
        } else if (focusCards && !cards.isEmpty()) {
            focusCards = false;
            cards.get(0).requestFocus();
        }
        rebuilding = false;

        int live = 0;
        for (Kick.Stream s : list) if (s.live) live++;
        String message;
        switch (tab) {
            case FAVORITES:
                status.setText(list.isEmpty() ? "" : live + " шууд · " + (list.size() - live) + " офлайн");
                message = "Дуртай суваг алга.\nКарт дээр Menu (≡) дарах эсвэл OK-г удаан дарж нэмнэ.";
                break;
            case SEARCH:
                status.setText(query.isEmpty() ? "" : list.size() + " илэрц");
                message = query.isEmpty() ? "Дээрх талбарт сувгийн нэрийг бичнэ үү."
                        : "\"" + query + "\" нэртэй суваг олдсонгүй.";
                break;
            default:
                status.setText(live + " шууд дамжуулалт");
                message = "Одоогоор шууд дамжуулалт алга.";
                break;
        }
        empty.setText(message);
        empty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private View card(Kick.Stream s) {
        View v = LayoutInflater.from(this).inflate(R.layout.card, grid, false);
        Card card = new Card(s, (TextView) v.findViewById(R.id.name));
        v.setTag(card);
        v.findViewById(R.id.frame).getLayoutParams().height = (cardWidth - dp(10)) * 9 / 16;
        ImageView thumb = (ImageView) v.findViewById(R.id.thumb);
        TextView badge = (TextView) v.findViewById(R.id.badge);
        TextView viewers = (TextView) v.findViewById(R.id.viewers);
        TextView title = (TextView) v.findViewById(R.id.title);
        TextView category = (TextView) v.findViewById(R.id.category);

        if (s.live && s.thumbnail != null) {
            images.load(thumb, s.thumbnail, cardWidth);
        } else {
            // Offline, or no stream picture yet: the channel's avatar, centred.
            thumb.setScaleType(ImageView.ScaleType.FIT_CENTER);
            int pad = dp(12);
            thumb.setPadding(pad, pad, pad, pad);
            images.load(thumb, s.avatar, dp(120));
        }
        if (!s.live) {
            badge.setText("ОФЛАЙН");
            badge.setTextColor(0xFFFFFFFF);
            badge.setBackgroundResource(R.drawable.badge_offline);
        }
        viewers.setText(count(s.viewers) + " үзэгч");
        viewers.setVisibility(s.live && s.viewers > 0 ? View.VISIBLE : View.GONE);
        title.setText(s.title.isEmpty() ? s.name : s.title);
        category.setText(s.live ? s.category : s.followers > 0 ? count(s.followers) + " дагагч" : "");
        bindName(card);

        v.setOnClickListener(x -> play(s));
        v.setOnLongClickListener(x -> {
            toggleFavorite(card);
            return true;
        });
        v.setOnFocusChangeListener((x, hasFocus) ->
                x.animate().scaleX(hasFocus ? 1.05f : 1f).scaleY(hasFocus ? 1.05f : 1f).setDuration(120));
        return v;
    }

    private void bindName(Card card) {
        card.name.setText((favorites.contains(card.stream.slug) ? "★ " : "") + card.stream.name);
    }

    private void toggleFavorite(Card card) {
        Kick.Stream s = card.stream;
        boolean added = Favorites.toggle(this, s.slug);
        favorites = new HashSet<>(Favorites.get(this));
        say(added ? "★ " + s.name + " дуртайд нэмэгдлээ" : s.name + " дуртайгаас хасагдлаа");
        if (tab == FAVORITES && !added) {
            List<Kick.Stream> rest = new ArrayList<>(shown);
            rest.remove(s);
            lists[FAVORITES] = rest;
            show(rest);
        } else {
            invalidate(FAVORITES);
            bindName(card);
        }
    }

    /** Opens the player; left and right there walk through the live channels of this tab. */
    private void play(Kick.Stream s) {
        if (!s.live) {
            say(s.name + " одоо шууд дамжуулалт хийхгүй байна");
            return;
        }
        ArrayList<String> slugs = new ArrayList<>();
        for (Kick.Stream x : shown) if (x.live) slugs.add(x.slug);
        startActivity(new Intent(this, PlayerActivity.class)
                .putExtra(PlayerActivity.EXTRA_SLUGS, slugs.toArray(new String[0]))
                .putExtra(PlayerActivity.EXTRA_INDEX, Math.max(0, slugs.indexOf(s.slug))));
    }

    private void clearCards() {
        images.cancelPending();
        grid.removeAllViews();
    }

    private int indexOfCard(View card) {
        int n = 0;
        for (int r = 0; r < grid.getChildCount(); r++) {
            ViewGroup row = (ViewGroup) grid.getChildAt(r);
            for (int c = 0; c < row.getChildCount(); c++, n++) if (row.getChildAt(c) == card) return n;
        }
        return 0;
    }

    private void say(String text) {
        if (toast != null) toast.cancel();
        toast = Toast.makeText(this, text, Toast.LENGTH_SHORT);
        toast.show();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    static String count(int n) {
        return String.format(Locale.US, "%,d", n);
    }
}
