package com.gcrj.largeimageviewlibrary;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by gcrj on 2016/7/31.
 */

public class LargeImageView extends View implements ScaleGestureDetector.OnScaleGestureListener {

    private final static String TAG = "LargeImageView";
    private final static int WHAT_DECODE_SAMPLE = 1;
    private final static int WHAT_DECODE_DRAW = 2;

    private Handler mHandler;

    private GestureDetector gestureDectector;
    private ScaleGestureDetector scaleDetector;
    private Scroller flingScroller;

    private BitmapRegionDecoder decoder;
    private Bitmap sampleBitmap;
    private Bitmap drawBitmap;
    private int mWidth;
    private int mHeight;
    private int mImageWidth;
    private int mImageHeight;
    private Point mPoint = new Point();
    private float mScale = 1.0f;
    private float mMaxScale = 1.0f;
    private float mMinScale = 0.25f;
    private int mSampleDecodeScale = 1;
    private boolean first = true;
    private boolean needDecodeDrawAfterFling = false;

    public LargeImageView(Context context) {
        super(context);
        init(context);
    }

    public LargeImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        gestureDectector = new GestureDetector(context, new GestureListener());
        scaleDetector = new ScaleGestureDetector(context, this);
        flingScroller = new Scroller(context);
        HandlerThread thread = new HandlerThread("LargeImageView");
        thread.start();
        mHandler = new Handler(thread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                switch (msg.what) {
                    case WHAT_DECODE_SAMPLE:
                        options.inSampleSize = mSampleDecodeScale;
                        sampleBitmap = decoder.decodeRegion(new Rect(0, 0, mImageWidth, mImageHeight - 1), options);
                        drawBitmap = null;
                        postInvalidate();
                        if (first) {
                            first = false;
                            mHandler.sendMessage(mHandler.obtainMessage(WHAT_DECODE_DRAW, mPoint.x, mPoint.y, mScale));
                        }
                        break;
                    case WHAT_DECODE_DRAW:
                        int centerX = msg.arg1;
                        int centerY = msg.arg2;
                        float scale = Float.parseFloat(msg.obj.toString());
                        if (!mPoint.equals(centerX, centerY) || scale != mScale) {
                            return;
                        }

                        options.inSampleSize = 1;
                        int left = (int) (centerX - mWidth * scale / 2);
                        int top = (int) (centerY - mHeight * scale / 2);
                        int right = (int) (centerX + mWidth * scale / 2);
                        int bottom = (int) (centerY + mHeight * scale / 2);
                        if (left < 0) {
                            left = 0;
                        }
                        if (top < 0) {
                            top = 0;
                        }
                        if (right > mImageWidth) {
                            right = mImageWidth;
                        }
                        if (bottom >= mImageHeight) {
                            bottom = mImageHeight - 1;
                        }

                        if (!mPoint.equals(centerX, centerY) || scale != mScale) {
                            return;
                        }
                        drawBitmap = decoder.decodeRegion(new Rect(left, top, right, bottom), options);

                        postInvalidate();
                        break;
                }
            }
        }

        ;
    }

    public void setImage(InputStream is) throws IOException {
        decoder = BitmapRegionDecoder.newInstance(is, false);
        if (mWidth > 0) {
            initBitmap();
        }
    }

    private void initBitmap() {
        first = true;
        int scale = 1;
        mImageWidth = decoder.getWidth();
        mImageHeight = decoder.getHeight();
        if (mImageWidth > mWidth || mImageHeight > mHeight) {
            int widthScale = (int) Math.ceil(1.0f * mImageWidth / mWidth);
            int heightScale = (int) Math.ceil(1.0f * mImageHeight / mHeight);
            scale = widthScale > heightScale ? widthScale : heightScale;
            for (int i = 0; ; i++) {
                if (scale > Math.pow(2, i) && scale <= Math.pow(2, i + 1)) {
                    scale = (int) Math.pow(2, i + 1);
                    Log.d(TAG, "initBitmap: scale = " + scale);
                    break;
                }
            }
            mSampleDecodeScale = scale;
        }
        mPoint.set(mImageWidth / 2, mImageHeight / 2);
        if (mImageWidth < mWidth || mImageHeight < mHeight) {
            float widthScale = 1.0f * mImageWidth / mWidth;
            float heightScale = 1.0f * mImageHeight / mHeight;
            mMaxScale = mScale = widthScale < heightScale ? widthScale : heightScale;
            mMinScale = mMaxScale / 4;
        }
        Log.d(TAG, "initBitmap: mScale = " + mScale);
        mHandler.sendEmptyMessage(WHAT_DECODE_SAMPLE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sampleBitmap == null || mWidth == 0 || mHeight == 0) {
            return;
        }

        if (drawBitmap == null) {
            int realWidth = mImageWidth / mSampleDecodeScale;
            int realHeight = mImageHeight / mSampleDecodeScale;
            int left = (int) ((mPoint.x - mWidth * mScale / 2) / mSampleDecodeScale);
            int top = (int) ((mPoint.y - mHeight * mScale / 2) / mSampleDecodeScale);
            int right = (int) ((mPoint.x + mWidth * mScale / 2) / mSampleDecodeScale);
            int bottom = (int) ((mPoint.y + mHeight * mScale / 2) / mSampleDecodeScale);
            if (left < 0) {
                left = 0;
            }
            if (top < 0) {
                top = 0;
            }
            if (right > realWidth) {
                right = realWidth;
            }
            if (bottom >= realHeight) {
                bottom = realHeight - 1;
            }
            canvas.drawBitmap(sampleBitmap, new Rect(left, top, right, bottom), new Rect(0, 0, mWidth, mHeight), null);
        } else {
            canvas.drawBitmap(drawBitmap, null, new Rect(0, 0, mWidth, mHeight), null);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleDetector.onTouchEvent(event);
        if (!scaleDetector.isInProgress()) {
            gestureDectector.onTouchEvent(event);
            int action = event.getAction();
            if (flingScroller.isFinished() && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
                mHandler.sendMessage(mHandler.obtainMessage(WHAT_DECODE_DRAW, mPoint.x, mPoint.y, mScale));
            }
        }
        return true;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            Log.d("aa", "onScroll " + distanceX + "  " + distanceY);
            flingScroller.abortAnimation();
            scroll(distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            Log.d("aa", "onFling " + velocityX + " " + velocityY);
            needDecodeDrawAfterFling = true;
            flingScroller.fling(mPoint.x, mPoint.y, (int) -velocityX / 3, (int) -velocityY / 3, (int) (mWidth * mScale / 2), (int) (mImageWidth - mWidth * mScale / 2), (int) (mHeight * mScale / 2), (int) (mImageHeight - 1 - mHeight * mScale / 2));
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float offsetScale;
            if (mScale == mMinScale) {
                offsetScale = 0.25f;
            } else {
                offsetScale = 2;
                if (mScale / 2 < mMinScale) {
                    offsetScale = mScale / mMinScale;
                }
            }
            Log.d("aa", "onDoubleTap " + offsetScale + "  " + e.getX() + " " + e.getY());
            scale(offsetScale, e.getX(), e.getY());
            return true;
        }

    }

    private void scroll(float distanceX, float distanceY) {
        float realDistanceX = distanceX * mScale;
        float realDistanceY = distanceY * mScale;
        int centerX = (int) Math.ceil(mPoint.x + realDistanceX);
        int centerY = (int) Math.ceil(mPoint.y + realDistanceY);
        Rect rect = new Rect();
        rect.left = (int) (centerX - mWidth * mScale / 2);
        rect.top = (int) (centerY - mHeight * mScale / 2);
        rect.right = (int) (centerX + mWidth * mScale / 2);
        rect.bottom = (int) (centerY + mHeight * mScale / 2);
        if (rect.left < 0) {
            rect.left = 0;
            rect.right = (int) (mWidth * mScale);
        }
        if (rect.top < 0) {
            rect.top = 0;
            rect.bottom = (int) (mHeight * mScale);
        }
        if (rect.right > mImageWidth) {
            rect.right = mImageWidth;
            rect.left = (int) (mImageWidth - mWidth * mScale);
            if (rect.left < 0) {
                rect.left = 0;
            }
        }
        if (rect.bottom >= mImageHeight) {
            rect.bottom = mImageHeight - 1;
            rect.top = (int) (mImageHeight - 1 - mHeight * mScale);
            if (rect.top < 0) {
                rect.top = 0;
            }
        }

        mHandler.removeMessages(WHAT_DECODE_SAMPLE);
        mPoint.set(rect.centerX(), rect.centerY());
        mHandler.sendEmptyMessage(WHAT_DECODE_SAMPLE);
    }

    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        Log.d("aa", "onScale " + scaleGestureDetector.getScaleFactor() + "  " + scaleGestureDetector.getFocusX() + " " + scaleGestureDetector.getFocusY());
        float offsetScale = scaleGestureDetector.getScaleFactor();
        if (offsetScale == 1.0f || offsetScale == 0f) {
            return true;
        }
        float focusX = scaleGestureDetector.getFocusX();
        float focusY = scaleGestureDetector.getFocusY();
        scale(offsetScale, focusX, focusY);
        return true;
    }

    private void scale(float offsetScale, float focusX, float focusY) {
        flingScroller.abortAnimation();
        mHandler.removeMessages(WHAT_DECODE_SAMPLE);
        RectF rectF = new RectF();
        rectF.left = mPoint.x - mWidth * mScale / 2;
        rectF.top = mPoint.y - mHeight * mScale / 2;
        rectF.right = mPoint.x + mWidth * mScale / 2;
        rectF.bottom = mPoint.y + mHeight * mScale / 2;
        float percentX = focusX / mWidth;
        float percentY = focusY / mHeight;
        PointF point = new PointF();
        point.x = rectF.left + rectF.width() * percentX;
        point.y = rectF.top + rectF.height() * percentY;

        mScale /= offsetScale;
        if (mScale > mMaxScale) {
            mScale = mMaxScale;
        }
        if (mScale < mMinScale) {
            mScale = mMinScale;
        }
        rectF.left = point.x - mWidth * mScale * percentX;
        rectF.top = point.y - mHeight * mScale * percentY;
        if (rectF.left < 0) {
            rectF.left = 0;
        }
        if (rectF.top < 0) {
            rectF.top = 0;
        }
        rectF.right = rectF.left + mWidth * mScale;
        rectF.bottom = rectF.top + mHeight * mScale;
        if (rectF.right > mImageWidth) {
            rectF.right = mImageWidth;
        }
        if (rectF.bottom >= mImageHeight) {
            rectF.bottom = mImageHeight - 1;
        }

        mPoint.set((int) rectF.centerX(), (int) rectF.centerY());
        mHandler.sendEmptyMessage(WHAT_DECODE_SAMPLE);
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (flingScroller.computeScrollOffset()) {
            Log.d("bb", flingScroller.getCurrX() - flingScroller.getStartX() + " " + (flingScroller.getCurrY() - flingScroller.getStartY()));
            scroll(flingScroller.getCurrX() - flingScroller.getStartX(), flingScroller.getCurrY() - flingScroller.getStartY());
        } else {
            if (needDecodeDrawAfterFling) {
                Log.d("bb", "needDecodeDrawAfterFling");
                mHandler.sendMessage(mHandler.obtainMessage(WHAT_DECODE_DRAW, mPoint.x, mPoint.y, mScale));
                needDecodeDrawAfterFling = false;
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        if (decoder != null) {
            initBitmap();
        }
    }
}
