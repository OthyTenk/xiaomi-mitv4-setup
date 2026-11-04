package mn.home.kick;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Downloads card pictures off the main thread and keeps the recent ones in memory. */
final class Images {
    private final ThreadPoolExecutor pool =
            new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<String, Bitmap> cache =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 6)) {
                @Override
                protected int sizeOf(String url, Bitmap bitmap) {
                    return bitmap.getByteCount() / 1024;
                }
            };

    /** Shows the picture at url in view, scaled down to about width pixels. */
    void load(ImageView view, String url, int width) {
        view.setTag(url);
        Bitmap hit = url == null ? null : cache.get(url);
        view.setImageBitmap(hit);
        if (url == null || hit != null) return;
        pool.execute(() -> {
            Bitmap bitmap = decode(url, width);
            main.post(() -> {
                if (bitmap != null) cache.put(url, bitmap);
                if (url.equals(view.getTag())) view.setImageBitmap(bitmap);
            });
        });
    }

    /** Drops downloads nobody is waiting for any more, such as those of a tab just left. */
    void cancelPending() {
        pool.getQueue().clear();
    }

    private static Bitmap decode(String url, int width) {
        try {
            byte[] data = Kick.bytes(url, "image/webp,image/*");
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            o.inJustDecodeBounds = false;
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= width) o.inSampleSize *= 2;
            // Half the memory of ARGB_8888; the cards sit on an opaque background anyway.
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (b != null && b.getWidth() > width * 5 / 4) {
                Bitmap scaled = Bitmap.createScaledBitmap(b, width, b.getHeight() * width / b.getWidth(), true);
                if (scaled != b) b.recycle();
                b = scaled;
            }
            return b;
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }
}
