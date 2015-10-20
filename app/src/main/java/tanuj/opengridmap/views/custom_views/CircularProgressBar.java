package tanuj.opengridmap.views.custom_views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class CircularProgressBar extends View {
    private static final String TAG = CircularProgressBar.class.getSimpleName();

    private static final float DEFAULT_STROKE_WIDTH = 4;

    private static final int DEFAULT_MIN = 0;

    private static final int DEFAULT_MAX = 100;

    private static final int DEFAULT_START_ANGLE = -90;

    private static final int DEFAULT_COLOR = Color.DKGRAY;

    private float strokeWidth = DEFAULT_STROKE_WIDTH;
    private float progress = 0;
    private int min = DEFAULT_MIN;
    private int max = DEFAULT_MAX;
    private int startAngle = DEFAULT_START_ANGLE;
    private int color = DEFAULT_COLOR;
    private RectF rectF;
    private Paint backgroundPaint;
    private Paint foregroundPaint;

    public CircularProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        rectF = new RectF();

        TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.CircleProgressBar, 0, 0);

        try {
            strokeWidth = typedArray.getDimension(R.styleable.CircleProgressBar_progressBarThickness,
                    strokeWidth);
            progress = typedArray.getFloat(R.styleable.CircleProgressBar_progress, progress);
            min = typedArray.getInt(R.styleable.CircleProgressBar_min, min);
            max = typedArray.getInt(R.styleable.CircleProgressBar_max, max);
            color = typedArray.getInt(R.styleable.CircleProgressBar_progressbarColor, color);
        }
        finally {
            typedArray.recycle();
        }

        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(adjustAlpha(color, 0.3f));
        backgroundPaint.setStyle(Paint.Style.STROKE);
        backgroundPaint.setStrokeWidth(strokeWidth);

        foregroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        foregroundPaint.setColor(color);
        foregroundPaint.setStyle(Paint.Style.STROKE);
        foregroundPaint.setStrokeWidth(strokeWidth);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int height = getDefaultSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        final int width = getDefaultSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int min = Math.min(width, height);
        setMeasuredDimension(min, min);
        rectF.set(0 + strokeWidth / 2, 0 + strokeWidth / 2, min - strokeWidth / 2,
                min - strokeWidth / 2);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawOval(rectF, backgroundPaint);
        float angle = 360 * progress / max;
        canvas.drawArc(rectF, startAngle, angle, false, foregroundPaint);
    }

    public void setProgress(int progress) {
        this.progress = progress;
        invalidate();
    }
}
