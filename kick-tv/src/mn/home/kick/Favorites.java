package mn.home.kick;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Favourite channels by slug, newest first. They live on this TV only; Kick has no login here. */
final class Favorites {
    private Favorites() {
    }

    static List<String> get(Context context) {
        List<String> slugs = new ArrayList<>();
        try {
            JSONArray saved = new JSONArray(prefs(context).getString("favorites", "[]"));
            for (int i = 0; i < saved.length(); i++) slugs.add(saved.getString(i));
        } catch (JSONException ignored) {
            // Unreadable list: start again from none.
        }
        return slugs;
    }

    /** Adds the channel, or removes it if it was there. Returns whether it is a favourite now. */
    static boolean toggle(Context context, String slug) {
        List<String> slugs = get(context);
        boolean added = !slugs.remove(slug);
        if (added) slugs.add(0, slug);
        prefs(context).edit().putString("favorites", new JSONArray(slugs).toString()).apply();
        return added;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("kick", Context.MODE_PRIVATE);
    }
}
