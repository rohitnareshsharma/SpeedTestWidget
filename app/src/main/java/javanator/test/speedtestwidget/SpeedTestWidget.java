package javanator.test.speedtestwidget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

/**
 * Widget to display speedtest functionality.
 * Its custom surface view so that drawing gets done
 * on non ui thread.
 * <p/>
 * <p/>
 * Created by rohit on 28/06/15.
 */
public class SpeedTestWidget extends SurfaceView implements SurfaceHolder.Callback {

    /*
     * Tag for logging.
     */
    private static final String TAG = SpeedTestWidget.class.getSimpleName();

    private static final int ARC_START_ANGLE = -225;

    private static final int ARC_BACKGROUND_SWEEP_ANGLE = 270;

    private static final float ARC_STROKE_WIDTH = 40.0f;

    private static final float OUTER_ARC_PADDING = 150f;

    private static final int[] TEXT_POINTER_VALUE_IN_MB = {0, 1, 2, 3, 4, 5, 10, 20, 50, 100};

    private static final String TEXT_POINTER_VALUE_POSTFIX = "M";

    private static final float CENTER_TEXT_SIZE = 60f;

    private Context mContext;

    private RenderingThread renderingThread;
    private CalculateDownloadSpeedThread calculateDownloadSpeedThread;
    private CalculateUploadSpeedThread calculateUploadSpeedThread;

    // Components for outer download arc
    private RectF rectForDownloadArc;
    private Paint paintForDownloadArc;

    // Components for inner upload arc
    private RectF rectForUploadArc;
    private Paint paintForUploadArc;

    // Component for outer text pointers.
    private Paint paintForTextPointers;
    private Path pathForTextPointers;
    private float verticalOffSetOfTextOnPath = -45.0f;
    private float pointersTextSize = 40.0f;

    // Component for eraser paint
    private Paint eraserPaint;
    private RectF rectForEraserPaint;

    // Component for center button
    private Paint paintForCenterButton;
    private RectF rectForCenterButton;

    // Component for center text
    private Paint paintForCenterText;
    private String centerText;

    private boolean centerButtonPressed = false;

    public SpeedTestWidget(Context context) {
        super(context);
        init(context);
    }

    public SpeedTestWidget(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SpeedTestWidget(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public SpeedTestWidget(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        mContext = context;
        setZOrderOnTop(true);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        // Start the rendering thread.
        renderingThread = new RenderingThread(holder, this);
        renderingThread.setRunning(true);
        renderingThread.start();

        initStartAnimation();
    }

    private void initStartAnimation() {
        postDelayed(new Runnable() {
            @Override
            public void run() {
                renderingThread.showDownloadSweepingAngle(270);
                renderingThread.showDownloadSweepingAngle(0);
            }
        }, 1000);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        // Stop the rendering thread.
        renderingThread.setRunning(false);

        // Stop the downloading thread. if any
        if(calculateDownloadSpeedThread != null) {
            calculateDownloadSpeedThread.setRunning(false);
        }

        // Stop the Uploading thread. if any
        if(calculateUploadSpeedThread != null) {
            calculateUploadSpeedThread.setRunning(false);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        int touchX = (int) event.getX();
        int touchY = (int) event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (rectForCenterButton.contains(touchX, touchY)) {
                    centerButtonPressed = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (rectForCenterButton.contains(touchX, touchY) &&
                    centerButtonPressed &&
                    centerText.equals(mContext.getString(R.string.start))) {

                    startButtonPressed();
                }
                centerButtonPressed = false;
                break;

        }
        return true;
    }

    private void startButtonPressed() {
        centerText = mContext.getString(R.string.wait);
        renderingThread.setRunning(true);

        calculateDownloadSpeedThread = new CalculateDownloadSpeedThread(this);
        calculateDownloadSpeedThread.setRunning(true);
        calculateDownloadSpeedThread.start();
    }

    protected void doDraw(Canvas canvas,
                          int downloadArcForegroundSweepAngle,
                          int uploadArcForegroundSweepAngle ) {

        initRequiredRectAndPaint();

        // Draw the text pointers
        drawTextPointers(canvas);

        // Draw the background of download arc
        paintForDownloadArc.setColor(Color.DKGRAY);
        canvas.drawArc(rectForDownloadArc,
                ARC_START_ANGLE,
                ARC_BACKGROUND_SWEEP_ANGLE,
                true,
                paintForDownloadArc);

        // Draw the download arc on the basis of speed obtained.
        paintForDownloadArc.setColor(Color.GRAY);

        if(downloadArcForegroundSweepAngle > 0) {
            canvas.drawArc(rectForDownloadArc,
                    ARC_START_ANGLE,
                    downloadArcForegroundSweepAngle,
                    true,
                    paintForDownloadArc);
        }

        // Draw the background of upload arc
        paintForUploadArc.setColor(Color.BLUE);
        canvas.drawArc(rectForUploadArc,
                ARC_START_ANGLE,
                ARC_BACKGROUND_SWEEP_ANGLE,
                true,
                paintForUploadArc);

        // Draw the upload arc on the basis of speed obtained.
        paintForUploadArc.setColor(Color.GREEN);
        if(uploadArcForegroundSweepAngle > 0) {
            canvas.drawArc(rectForUploadArc,
                    ARC_START_ANGLE,
                    uploadArcForegroundSweepAngle,
                    true,
                    paintForUploadArc);
        }


        // Clear the inner arc lines with eraser
        canvas.drawArc(rectForEraserPaint, 0, 360, true, eraserPaint);

        // Draw the center button
        canvas.drawArc(rectForCenterButton, 0, 360, true, paintForCenterButton);

        float xPos = rectForCenterButton.centerX();
        float yPos = (int) (rectForCenterButton.centerY() - (
                (paintForCenterText.descent() +
                        paintForCenterText.ascent()) / 2));

        // Draw the center text
        canvas.drawText(centerText, 0, centerText.length(),
                xPos,
                yPos,
                paintForCenterText);
    }

    private void drawTextPointers(Canvas canvas) {
        PathMeasure measure = new PathMeasure(pathForTextPointers, false);

        float originalDistance = 0;

        int numberOfTextPointers = TEXT_POINTER_VALUE_IN_MB.length;

        for (int i = 0; i < numberOfTextPointers; i++) {

            int value = TEXT_POINTER_VALUE_IN_MB[i];
            String pointerValue = value + TEXT_POINTER_VALUE_POSTFIX;

            float pointerWidth = paintForTextPointers.measureText(pointerValue);
            float hOffSet = originalDistance;
            if (i != 0 && i != (numberOfTextPointers - 1)) {
                hOffSet -= (pointerWidth / 2);
            } else if (i == (numberOfTextPointers - 1)) {
                hOffSet -= pointerWidth;
            }

            canvas.drawTextOnPath(
                    pointerValue,
                    pathForTextPointers,
                    hOffSet,
                    verticalOffSetOfTextOnPath,
                    paintForTextPointers);

            originalDistance += measure.getLength() / (numberOfTextPointers - 1);
        }
    }

    private void initRequiredRectAndPaint() {

        initRectAndPaintForDownloadArc();
        initRectAndPaintForUploadArc();
        initPaintForTextPointers();
        initRectAndPaintForEraser();
        initRectAndPaintForCenterButton();
        initPaintForCenterText();
    }

    private void initPaintForCenterText() {

        if (paintForCenterText == null) {

            paintForCenterText = new Paint();
            paintForCenterText.setAntiAlias(true);
            paintForCenterText.setColor(Color.WHITE);
            paintForCenterText.setTextAlign(Paint.Align.CENTER);
            paintForCenterText.setTextSize(CENTER_TEXT_SIZE);

        }

        if (centerText == null) {
            centerText = mContext.getString(R.string.start);
        }
    }

    private void initRectAndPaintForCenterButton() {

        if (rectForCenterButton == null || paintForCenterButton == null) {

            rectForCenterButton = new RectF();
            int left = 0;
            int width = getWidth();
            int height = getHeight();
            int top = 0;

            rectForCenterButton.set(left + OUTER_ARC_PADDING + (3 * ARC_STROKE_WIDTH),
                    top + OUTER_ARC_PADDING + (3 * ARC_STROKE_WIDTH),
                    left + width - OUTER_ARC_PADDING - (3 * ARC_STROKE_WIDTH),
                    top + height - OUTER_ARC_PADDING - (3 * ARC_STROKE_WIDTH));

            paintForCenterButton = new Paint();
            paintForCenterButton.setStyle(Paint.Style.FILL_AND_STROKE);
            paintForCenterButton.setAntiAlias(true);
            paintForCenterButton.setStrokeWidth(ARC_STROKE_WIDTH);
            paintForCenterButton.setColor(Color.BLUE);

        }
    }


    private void initRectAndPaintForEraser() {

        if (rectForEraserPaint == null || eraserPaint == null) {

            rectForEraserPaint = new RectF();
            int left = 0;
            int width = getWidth();
            int height = getHeight();
            int top = 0;

            rectForEraserPaint.set(left + OUTER_ARC_PADDING + (2 * ARC_STROKE_WIDTH),
                    top + OUTER_ARC_PADDING + (2 * ARC_STROKE_WIDTH),
                    left + width - OUTER_ARC_PADDING - (2 * ARC_STROKE_WIDTH),
                    top + height - OUTER_ARC_PADDING - (2 * ARC_STROKE_WIDTH));

            eraserPaint = new Paint();
            eraserPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            eraserPaint.setAntiAlias(true);
            eraserPaint.setStrokeWidth(ARC_STROKE_WIDTH);
            eraserPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));


        }
    }

    private void initRectAndPaintForDownloadArc() {
        // Check for rectForDownloadArc is initialized or not.
        if (rectForDownloadArc == null || paintForDownloadArc == null) {
            rectForDownloadArc = new RectF();
            int left = 0;
            int width = getWidth();
            int height = getHeight();
            int top = 0;

            rectForDownloadArc.set(left + OUTER_ARC_PADDING,
                    top + OUTER_ARC_PADDING,
                    left + width - OUTER_ARC_PADDING,
                    top + height - OUTER_ARC_PADDING);

            paintForDownloadArc = new Paint();
            paintForDownloadArc.setStyle(Paint.Style.STROKE);
            paintForDownloadArc.setAntiAlias(true);
            paintForDownloadArc.setStrokeWidth(ARC_STROKE_WIDTH);
        }
    }

    private void initRectAndPaintForUploadArc() {
        // Check for rectForUploadArc is initialized or not.
        if (rectForUploadArc == null || paintForUploadArc == null) {
            rectForUploadArc = new RectF();
            int left = 0;
            int width = getWidth();
            int height = getHeight();
            int top = 0;

            rectForUploadArc.set(left + OUTER_ARC_PADDING + ARC_STROKE_WIDTH,
                    top + OUTER_ARC_PADDING + ARC_STROKE_WIDTH,
                    left + width - OUTER_ARC_PADDING - ARC_STROKE_WIDTH,
                    top + height - OUTER_ARC_PADDING - ARC_STROKE_WIDTH);

            paintForUploadArc = new Paint();
            paintForUploadArc.setStyle(Paint.Style.STROKE);
            paintForUploadArc.setAntiAlias(true);
            paintForUploadArc.setStrokeWidth(ARC_STROKE_WIDTH);
        }
    }

    private void initPaintForTextPointers() {
        // Check for rectForDownloadArc is initialized or not.
        if (paintForTextPointers == null) {

            paintForTextPointers = new Paint();
            paintForTextPointers.setTextSize(pointersTextSize);
            paintForTextPointers.setColor(Color.BLACK);

            pathForTextPointers = new Path();
            pathForTextPointers.addArc(rectForDownloadArc,
                    ARC_START_ANGLE,
                    ARC_BACKGROUND_SWEEP_ANGLE);
        }
    }

    public void setDownloadSpeed(float speedInMbps) {
        renderingThread.showDownloadSweepingAngle(calculateSweepAngleOnSpeedBasis(speedInMbps));
    }

    public void setDownloadCompleted() {
        centerText = mContext.getString(R.string.wait);
        renderingThread.setRunning(true);

        calculateUploadSpeedThread = new CalculateUploadSpeedThread(this);
        calculateUploadSpeedThread.setRunning(true);
        calculateUploadSpeedThread.start();
    }

    public void setUploadCompleted() {
        centerText = mContext.getString(R.string.start);
        renderingThread.setRunning(true);
    }


    public void setUploadSpeed(float speedInMbps) {
        renderingThread.showUploadSweepingAngle(calculateSweepAngleOnSpeedBasis(speedInMbps));
    }

    private int calculateSweepAngleOnSpeedBasis(float speedInMbps) {

        int sweepingAngle = 0;

        // Start with the first index.
        int i = 1;
        while (i < TEXT_POINTER_VALUE_IN_MB.length) {
            if (speedInMbps <= TEXT_POINTER_VALUE_IN_MB[i]) {
                break;
            }
            i++;
        }

        if (i >= TEXT_POINTER_VALUE_IN_MB.length) {
            return ARC_BACKGROUND_SWEEP_ANGLE;
        }

        int numberOfDegreeInEachSection = ARC_BACKGROUND_SWEEP_ANGLE / TEXT_POINTER_VALUE_IN_MB.length;
        sweepingAngle = (i - 1) * numberOfDegreeInEachSection;

        // We have calculated the floor text pointer sweepingAngle.
        // Calculate the rest increase as well.
        // Lets calculate the difference of speed in the section where our speed is
        // residing.
        float diffOfSpeedInResidingSection = TEXT_POINTER_VALUE_IN_MB[i] - TEXT_POINTER_VALUE_IN_MB[i - 1];
        float perDegreeMbIncreaseInResidingSection = diffOfSpeedInResidingSection / numberOfDegreeInEachSection;
        float speedInMppsLeftToBeConsideredInSweepingAngle = speedInMbps - TEXT_POINTER_VALUE_IN_MB[i - 1];

        // Prepare the array for binary search to find the nearest index where speed belongs
        float[] arrayOfSpeedValues = new float[numberOfDegreeInEachSection];
        for (int y = 0; y < numberOfDegreeInEachSection; y++) {
            arrayOfSpeedValues[y] = y * perDegreeMbIncreaseInResidingSection;
        }

        int nearestIndex = getClosestIndex(arrayOfSpeedValues, speedInMppsLeftToBeConsideredInSweepingAngle);
        sweepingAngle += nearestIndex * perDegreeMbIncreaseInResidingSection;

        return sweepingAngle;
    }

    /**
     * Use binary search to find closest index.
     *
     * @param a
     * @param x
     * @return
     */
    public static int getClosestIndex(float[] a, float x) {

        int low = 0;
        int high = a.length - 1;

        if (high < 0)
            throw new IllegalArgumentException("The array cannot be empty");

        while (low < high) {
            int mid = (low + high) / 2;
            float d1 = Math.abs(a[mid] - x);
            float d2 = Math.abs(a[mid + 1] - x);
            if (d2 <= d1) {
                low = mid + 1;
            } else {
                high = mid;
            }
        }
        return high;
    }

    // Thread doing the ui rendering of SpeedTestWidget.
    private class RenderingThread extends Thread {

        private final Integer INCREMENT_IN_PROGRESS = 4;

        private boolean mRunning;

        private Canvas mCanvas;

        private SurfaceHolder mSurfaceHolder;

        private SpeedTestWidget mSpeedTestWidget;

        private int startingDownloadSweepAngle = 0;

        private int startingUploadSweepAngle = 0;

        private int targetDownloadSweepAngle = 0;

        private int targetUploadSweepAngle = 0;

        private Queue<Integer> downloadValuesToBeDisplayed = new LinkedList<>();

        private Queue<Integer> uploadValuesToBeDisplayed = new LinkedList<>();

        public RenderingThread(SurfaceHolder surfaceHolder,
                               SpeedTestWidget speedTestWidget) {

            mSurfaceHolder = surfaceHolder;
            mRunning = false;
            mSpeedTestWidget = speedTestWidget;

            // Set name of the thread.
            setName(RenderingThread.class.getSimpleName());
        }

        public void showDownloadSweepingAngle(Integer downloadSweepAngle) {
            synchronized (mSpeedTestWidget) {
                downloadValuesToBeDisplayed.add(downloadSweepAngle);
                mSpeedTestWidget.notify();
            }
        }

        public void showUploadSweepingAngle(Integer uploadSweepAngle) {
            synchronized (mSpeedTestWidget) {
                uploadValuesToBeDisplayed.add(uploadSweepAngle);
                mSpeedTestWidget.notify();
            }
        }

        private Integer getDownloadSweepAngleToDisplay() {
            Integer top;
            synchronized (mSpeedTestWidget) {
                if(downloadValuesToBeDisplayed.isEmpty()) {
                    top = null;
                }
                else {
                    top  = downloadValuesToBeDisplayed.remove();
                }
            }

            return top;
        }

        private Integer getUploadSweepAngleToDisplay() {
            Integer top;
            synchronized (mSpeedTestWidget) {
                if(uploadValuesToBeDisplayed.isEmpty()) {
                    top = null;
                } else {
                    top  = uploadValuesToBeDisplayed.remove();
                }
            }

            return top;
        }

        public void setRunning(boolean running) {
            mRunning = running;
            synchronized (mSpeedTestWidget) {
                mSpeedTestWidget.notify();
            }
        }

        private void pauseDrawing() {
            synchronized (mSpeedTestWidget) {
                // After drawing once put your self on hold.
                try {
                    mSpeedTestWidget.wait();
                } catch (InterruptedException e) {
                    Log.e(TAG, e.getMessage(), e);
                }
            }
        }

        @Override
        public void run() {
            super.run();
            while (mRunning) {

                Integer tempTargetDownloadSweepAngle = getDownloadSweepAngleToDisplay();
                Integer tempTargetUploadSweepAnngle = getUploadSweepAngleToDisplay();

                if (tempTargetDownloadSweepAngle == null && tempTargetUploadSweepAnngle == null) {
                    drawOnCanvas();
                    pauseDrawing();
                    continue;
                }

                if (tempTargetDownloadSweepAngle != null) {
                    targetDownloadSweepAngle = tempTargetDownloadSweepAngle;
                }

                if (tempTargetUploadSweepAnngle != null) {
                    targetUploadSweepAngle = tempTargetUploadSweepAnngle;
                }

                showProgressAnimation();

                startingDownloadSweepAngle = targetDownloadSweepAngle;
                startingUploadSweepAngle = targetUploadSweepAngle;

                drawOnCanvas();

            }
        }

        private void showProgressAnimation() {
            while (Math.abs(startingDownloadSweepAngle - targetDownloadSweepAngle) > INCREMENT_IN_PROGRESS ||
                    Math.abs(startingUploadSweepAngle - targetUploadSweepAngle) > INCREMENT_IN_PROGRESS) {

                if (startingDownloadSweepAngle < targetDownloadSweepAngle) {
                    startingDownloadSweepAngle += INCREMENT_IN_PROGRESS;
                } else if (startingDownloadSweepAngle > targetDownloadSweepAngle) {
                    startingDownloadSweepAngle -= INCREMENT_IN_PROGRESS;
                }

                if (startingUploadSweepAngle < targetUploadSweepAngle) {
                    startingUploadSweepAngle += INCREMENT_IN_PROGRESS;
                } else if (startingUploadSweepAngle > targetUploadSweepAngle) {
                    startingUploadSweepAngle -= INCREMENT_IN_PROGRESS;
                }

                drawOnCanvas();
            }
        }

        private void drawOnCanvas() {
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null) {
                mSpeedTestWidget.doDraw(mCanvas, startingDownloadSweepAngle, startingUploadSweepAngle);
                mSurfaceHolder.unlockCanvasAndPost(mCanvas);
            }
        }

    }


    private class CalculateDownloadSpeedThread extends Thread {

        boolean mRunning = false;

        SpeedTestWidget mSpeedTestWidget;

        public CalculateDownloadSpeedThread(SpeedTestWidget speedTestWidget) {
            mSpeedTestWidget = speedTestWidget;
        }

        public void setRunning(boolean running) {
            mRunning = running;
        }

        @Override
        public void run() {
            super.run();

            if (!mRunning) {
                return;
            }

            int i = 0;
            while (i < 30) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                i++;

                if (!mRunning) {
                    break;
                }

                int speed = randInt(5, 20);
                setDownloadSpeed(speed);

            }

            setDownloadCompleted();
        }
    }

    private class CalculateUploadSpeedThread extends Thread {

        boolean mRunning = false;

        SpeedTestWidget mSpeedTestWidget;

        public CalculateUploadSpeedThread(SpeedTestWidget speedTestWidget) {
            mSpeedTestWidget = speedTestWidget;
        }

        public void setRunning(boolean running) {
            mRunning = running;
        }

        @Override
        public void run() {
            super.run();

            if (!mRunning) {
                return;
            }

            int i = 0;
            while (i < 30) {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                i++;

                if (!mRunning) {
                    break;
                }

                int speed = randInt(3, 7);
                setUploadSpeed(speed);

            }

            setUploadCompleted();
        }
    }

    /**
     * Returns a pseudo-random number between min and max, inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     *
     * @param min Minimum value
     * @param max Maximum value.  Must be greater than min.
     * @return Integer between min and max, inclusive.
     * @see java.util.Random#nextInt(int)
     */
    public static int randInt(int min, int max) {

        // NOTE: Usually this should be a field rather than a method
        // variable so that it is not re-seeded every call.
        Random rand = new Random();

        // nextInt is normally exclusive of the top value,
        // so add 1 to make it inclusive
        int randomNum = rand.nextInt((max - min) + 1) + min;

        return randomNum;
    }

}