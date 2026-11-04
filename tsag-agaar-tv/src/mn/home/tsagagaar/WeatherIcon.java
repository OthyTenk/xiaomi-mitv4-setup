package mn.home.tsagagaar;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Weather symbols drawn from shapes: the TV's fonts have no weather emoji. */
final class WeatherIcon extends Drawable {
    static final int SUN = 0, MOON = 1, SUN_CLOUD = 2, MOON_CLOUD = 3, CLOUD = 4,
            FOG = 5, DRIZZLE = 6, RAIN = 7, SNOW = 8, THUNDER = 9;

    private static final int YELLOW = 0xFFFFC83D, MOON_COLOR = 0xFFF2E6B6, WHITE = 0xFFF2F6FB,
            FOG_COLOR = 0xFFCFD8E3, DROP = 0xFF6FB7FF;

    private final int kind;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    WeatherIcon(int kind) {
        this.kind = kind;
        paint.setStrokeCap(Paint.Cap.ROUND);
    }

    /** Maps a WMO weather code (as Open-Meteo reports it) to a symbol. */
    static int forCode(int code, boolean isDay) {
        switch (code) {
            case 0: return isDay ? SUN : MOON;
            case 1: case 2: return isDay ? SUN_CLOUD : MOON_CLOUD;
            case 45: case 48: return FOG;
            case 51: case 53: case 55: case 56: case 57: return DRIZZLE;
            case 61: case 63: case 65: case 66: case 67: case 80: case 81: case 82: return RAIN;
            case 71: case 73: case 75: case 77: case 85: case 86: return SNOW;
            case 95: case 96: case 99: return THUNDER;
            default: return CLOUD;
        }
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        float size = Math.min(b.width(), b.height());
        c.save();
        c.translate(b.left + (b.width() - size) / 2f, b.top + (b.height() - size) / 2f);
        c.scale(size / 100f, size / 100f);  // everything below is drawn in a 100x100 box
        switch (kind) {
            case SUN: sun(c, 50, 50, 21); break;
            case MOON: moon(c, 50, 50, 28); break;
            case SUN_CLOUD: sun(c, 36, 36, 16); cloud(c, 4, 10); break;
            case MOON_CLOUD: moon(c, 36, 34, 20); cloud(c, 4, 10); break;
            case CLOUD: cloud(c, 0, 0); break;
            case FOG: cloud(c, 0, -12); fog(c); break;
            case DRIZZLE: cloud(c, 0, -12); drops(c, 2); break;
            case RAIN: cloud(c, 0, -12); drops(c, 3); break;
            case SNOW: cloud(c, 0, -12); flakes(c); break;
            case THUNDER: cloud(c, 0, -12); bolt(c); break;
        }
        c.restore();
    }

    private void sun(Canvas c, float cx, float cy, float r) {
        fill(YELLOW);
        c.drawCircle(cx, cy, r, paint);
        stroke(YELLOW, r * 0.22f);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i;
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            c.drawLine(cx + cos * r * 1.35f, cy + sin * r * 1.35f, cx + cos * r * 1.75f, cy + sin * r * 1.75f, paint);
        }
    }

    private void moon(Canvas c, float cx, float cy, float r) {
        Path disc = new Path(), bite = new Path();
        disc.addCircle(cx, cy, r, Path.Direction.CW);
        bite.addCircle(cx + r * 0.45f, cy - r * 0.3f, r * 0.85f, Path.Direction.CW);
        disc.op(bite, Path.Op.DIFFERENCE);
        fill(MOON_COLOR);
        c.drawPath(disc, paint);
    }

    /** A cloud spanning x 18..86, y 30..76 before the offset. */
    private void cloud(Canvas c, float dx, float dy) {
        fill(WHITE);
        c.drawCircle(36 + dx, 60 + dy, 15, paint);
        c.drawCircle(56 + dx, 50 + dy, 20, paint);
        c.drawCircle(73 + dx, 62 + dy, 13, paint);
        c.drawRoundRect(new RectF(18 + dx, 58 + dy, 86 + dx, 76 + dy), 9, 9, paint);
    }

    private void fog(Canvas c) {
        stroke(FOG_COLOR, 5);
        for (int i = 0; i < 3; i++) c.drawLine(22 + i * 4, 74 + i * 9, 82 - i * 4, 74 + i * 9, paint);
    }

    private void drops(Canvas c, int n) {
        stroke(DROP, 5);
        float start = n == 3 ? 36 : 43;
        for (int i = 0; i < n; i++) {
            float x = start + i * 15;
            c.drawLine(x, 72, x - 5, 88, paint);
        }
    }

    private void flakes(Canvas c) {
        stroke(WHITE, 3.5f);
        float[][] at = {{34, 80}, {52, 90}, {70, 80}};
        for (float[] p : at) {
            for (int i = 0; i < 3; i++) {
                double a = Math.PI / 3 * i;
                float dx = (float) Math.cos(a) * 6, dy = (float) Math.sin(a) * 6;
                c.drawLine(p[0] - dx, p[1] - dy, p[0] + dx, p[1] + dy, paint);
            }
        }
    }

    private void bolt(Canvas c) {
        Path p = new Path();
        p.moveTo(56, 62);
        p.lineTo(43, 81);
        p.lineTo(52, 81);
        p.lineTo(46, 97);
        p.lineTo(64, 74);
        p.lineTo(55, 74);
        p.lineTo(62, 62);
        p.close();
        fill(YELLOW);
        c.drawPath(p, paint);
    }

    private void fill(int color) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
    }

    private void stroke(int color, float width) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(width);
        paint.setColor(color);
    }

    @Override
    public void setAlpha(int alpha) {
        paint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter filter) {
        paint.setColorFilter(filter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}
