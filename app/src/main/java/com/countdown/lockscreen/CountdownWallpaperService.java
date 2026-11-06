package com.countdown.lockscreen;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.service.wallpaper.WallpaperService;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CountdownWallpaperService extends WallpaperService {

    @Override
    public Engine onCreateEngine() {
        return new Fixed2DWallpaperEngine();
    }

    private class Fixed2DWallpaperEngine extends Engine {

        private SurfaceHolder mSurfaceHolder;
        private boolean mVisible = false;
        private int mWidth = 1080;
        private int mHeight = 2400;

        private final Handler mHandler = new Handler(Looper.getMainLooper());

        // 2D Wallpaper Bitmaps from the app
        private Bitmap mBgBitmap1;
        private Bitmap mBgBitmap2;
        private int mCurrentWallpaperIndex = 0; // 0 = Alpine Sunset, 1 = Misty Forest

        // Touch & Double-Tap detection
        private long mLastTapTime = 0;

        // Graphics tools
        private Paint mTextPaint;
        private Paint mGlassCardPaint;
        private Paint mBorderPaint;
        private Paint mGlowPaint;
        private Paint mBitmapPaint;

        private final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault());

        // Fixed motionless update runnable (updates once every second for live information)
        private final Runnable mDrawRunnable = new Runnable() {
            @Override
            public void run() {
                drawFrame();
                if (mVisible) {
                    // Update live clock and countdown once every second
                    long now = System.currentTimeMillis();
                    long delay = 1000 - (now % 1000);
                    mHandler.postDelayed(mDrawRunnable, delay > 0 ? delay : 1000);
                }
            }
        };

        @Override
        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
            mSurfaceHolder = surfaceHolder;
            setTouchEventsEnabled(true);

            initPaints();
            loadWallpapers();
        }

        private void initPaints() {
            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
            mGlassCardPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBorderPaint.setStyle(Paint.Style.STROKE);
            mGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        }

        private void loadWallpapers() {
            try {
                mBgBitmap1 = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_app_bg);
            } catch (Exception ignored) {}
            try {
                mBgBitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.wallpaper_app_bg2);
            } catch (Exception ignored) {}
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            mVisible = visible;
            if (visible) {
                mHandler.removeCallbacks(mDrawRunnable);
                mHandler.post(mDrawRunnable);
            } else {
                mHandler.removeCallbacks(mDrawRunnable);
            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mSurfaceHolder = holder;
            mWidth = Math.max(width, 100);
            mHeight = Math.max(height, 100);
            if (mVisible) {
                drawFrame();
            }
        }

        @Override
        public void onTouchEvent(MotionEvent event) {
            super.onTouchEvent(event);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                long now = System.currentTimeMillis();
                if (now - mLastTapTime < 380) {
                    // Double tap: Launch full interactive app over lock screen
                    try {
                        Intent launchIntent = new Intent(CountdownWallpaperService.this, MainActivity.class);
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(launchIntent);
                    } catch (Exception ignored) {}
                } else {
                    // Single tap: toggle between Alpine Sunset and Misty Forest wallpaper
                    mCurrentWallpaperIndex = (mCurrentWallpaperIndex + 1) % 2;
                    drawFrame();
                }
                mLastTapTime = now;
            }
        }

        private void drawFrame() {
            if (!mVisible || mSurfaceHolder == null || mWidth <= 0 || mHeight <= 0) {
                return;
            }

            Canvas canvas = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canvas = mSurfaceHolder.lockHardwareCanvas();
                } else {
                    canvas = mSurfaceHolder.lockCanvas();
                }

                if (canvas != null) {
                    renderFixed2DScene(canvas);
                }
            } catch (Exception ignored) {
            } finally {
                if (canvas != null) {
                    try {
                        mSurfaceHolder.unlockCanvasAndPost(canvas);
                    } catch (Exception ignored) {}
                }
            }
        }

        /**
         * Renders the 100% 2D, motionless, fixed lock screen layout.
         * Exactly matching the theme, fonts, words, numbers, and letters from the app.
         * Zero shaking, zero motion.
         */
        private void renderFixed2DScene(Canvas canvas) {
            // 1. DRAW 2D WALLPAPER (EXACT SCENIC PHOTO FROM THE APP - FIXED & MOTIONLESS)
            Bitmap currentBg = (mCurrentWallpaperIndex == 1 && mBgBitmap2 != null) ? mBgBitmap2 : mBgBitmap1;
            if (currentBg != null && !currentBg.isRecycled()) {
                Rect srcRect = new Rect(0, 0, currentBg.getWidth(), currentBg.getHeight());
                Rect dstRect = new Rect(0, 0, mWidth, mHeight);
                canvas.drawBitmap(currentBg, srcRect, dstRect, mBitmapPaint);
            } else {
                canvas.drawColor(Color.parseColor("#020617"));
            }

            // 2. DRAW 2D VIGNETTE TINT (From index.html: dark gradient at top and bottom for contrast)
            LinearGradient vignetteGrad = new LinearGradient(
                0, 0, 0, mHeight,
                new int[]{
                    Color.parseColor("#B3020617"), // Top 70% dark
                    Color.parseColor("#40020617"), // Translucent middle
                    Color.parseColor("#D9020617")  // Bottom 85% dark
                },
                new float[]{0f, 0.45f, 1f},
                Shader.TileMode.CLAMP
            );
            mGlassCardPaint.setShader(vignetteGrad);
            canvas.drawRect(0, 0, mWidth, mHeight, mGlassCardPaint);
            mGlassCardPaint.setShader(null);

            float cx = mWidth / 2f;

            // 3. 2D FIXED LAYOUT DIMENSIONS
            float uiWidth = Math.min(mWidth * 0.90f, 400f * (mWidth / 400f));
            float left = cx - (uiWidth / 2f);
            float right = left + uiWidth;

            float card1Top = mHeight * 0.08f;
            float card1Height = mHeight * 0.14f;

            float card2Top = card1Top + card1Height + (mHeight * 0.018f);
            float card2Height = mHeight * 0.38f;

            float card3Top = card2Top + card2Height + (mHeight * 0.018f);
            float card3Height = mHeight * 0.22f;

            // 4. DRAW 2D GLASS PANELS (Matches app's glassmorphism panels)
            drawGlassPanel(canvas, left, card1Top, right, card1Top + card1Height, 26f);
            drawGlassPanel(canvas, left, card2Top, right, card2Top + card2Height, 28f);
            drawGlassPanel(canvas, left, card3Top, right, card3Top + card3Height, 26f);

            // Date and time calculations
            Date now = new Date();
            Calendar nowCal = Calendar.getInstance();

            // Calculate countdown to Project Decline target (30 Nov 2026, 18:00)
            Calendar targetCal = Calendar.getInstance();
            targetCal.set(2026, Calendar.NOVEMBER, 30, 18, 0, 0);
            long diff = targetCal.getTimeInMillis() - nowCal.getTimeInMillis();
            if (diff < 0) diff = 0;

            long days = diff / (24 * 60 * 60 * 1000);
            long hours = (diff / (60 * 60 * 1000)) % 24;
            long minutes = (diff / (60 * 1000)) % 60;
            long seconds = (diff / 1000) % 60;

            // =========================================================================
            // CARD 1: SPOKEN WORD CLOCK (EXACT WORDS, FONTS, AND CASING FROM APP)
            // =========================================================================
            float c1PillY = card1Top + (card1Height * 0.24f);
            drawFixedText(canvas, "WORD CLOCK  •  PHRASED VIEW", cx, c1PillY, uiWidth * 0.028f,
                Color.parseColor("#38BDF8"), true, 0.12f, Typeface.SANS_SERIF);

            float c1TitleY = card1Top + (card1Height * 0.58f);
            String spokenWordTime = getSpokenWordClock(nowCal);
            drawFixedText(canvas, spokenWordTime, cx, c1TitleY, uiWidth * 0.040f,
                Color.WHITE, true, 0.06f, Typeface.SERIF);

            float c1DateY = card1Top + (card1Height * 0.84f);
            String spokenDate = mDateFormat.format(now).toUpperCase(Locale.getDefault());
            drawFixedText(canvas, spokenDate, cx, c1DateY, uiWidth * 0.030f,
                Color.parseColor("#94A3B8"), false, 0.06f, Typeface.SANS_SERIF);

            // =========================================================================
            // CARD 2: MAIN COUNTDOWN SECTION (EXACT WORDS, NUMBERS, BADGES FROM APP)
            // =========================================================================
            float c2MetaY = card2Top + (card2Height * 0.10f);

            // Meta badges: ★ High  #Work  Project Decline  30 Nov 2026
            drawFixedText(canvas, "★ HIGH    #WORK    PROJECT DECLINE    30 NOV 2026", cx, c2MetaY,
                uiWidth * 0.030f, Color.parseColor("#F43F5E"), true, 0.08f, Typeface.MONOSPACE);

            // Two Main Countdown Number Boxes: [DAYS LEFT] and [HOURS LEFT]
            float boxWidth = (uiWidth * 0.86f) / 2f;
            float boxHeight = card2Height * 0.38f;
            float boxTop = card2Top + (card2Height * 0.18f);

            float box1Left = left + (uiWidth * 0.05f);
            float box2Left = box1Left + boxWidth + (uiWidth * 0.04f);

            drawFixedCountdownBox(canvas, box1Left, boxTop, boxWidth, boxHeight,
                String.format(Locale.US, "%d", days), "Days Left");
            drawFixedCountdownBox(canvas, box2Left, boxTop, boxWidth, boxHeight,
                String.format(Locale.US, "%d", hours), "Hours Left");

            // Live status line: Live: 14m 42s  •  ~75 Working Hrs Left
            float c2LiveY = boxTop + boxHeight + (card2Height * 0.10f);
            String liveStr = "LIVE: " + String.format(Locale.US, "%02dm %02ds", minutes, seconds) + "  •  ~75 WORKING HRS LEFT";
            drawFixedText(canvas, liveStr, cx, c2LiveY, uiWidth * 0.030f,
                Color.parseColor("#E2E8F0"), false, 0.04f, Typeface.MONOSPACE);

            // Schedule indicators: 📅 All Days (24h)   🔔 Alerts Active
            float c2SchedY = c2LiveY + (card2Height * 0.08f);
            drawFixedText(canvas, "📅 All Days (24h)    🔔 Alerts Active (24h, 2h)", cx, c2SchedY,
                uiWidth * 0.026f, Color.parseColor("#34D399"), false, 0.04f, Typeface.SANS_SERIF);

            // Gradient Progress Bar
            float c2BarY = c2SchedY + (card2Height * 0.08f);
            float barWidth = uiWidth * 0.88f;
            float barLeft = cx - (barWidth / 2f);
            drawProgressBar(canvas, barLeft, c2BarY, barWidth, 6f, 0.28f);

            // =========================================================================
            // CARD 3: T-MINUS MILESTONES (EXACT WORDS AND PROGRESS FROM APP)
            // =========================================================================
            float c3HeaderY = card3Top + (card3Height * 0.20f);
            drawFixedText(canvas, "T-MINUS MILESTONES", cx, c3HeaderY, uiWidth * 0.030f,
                Color.parseColor("#CBD5E1"), true, 0.12f, Typeface.SANS_SERIF);

            float m1Y = card3Top + (card3Height * 0.45f);
            drawFixedText(canvas, "• Draft Review Due                    In 2 Days", cx, m1Y, uiWidth * 0.030f,
                Color.parseColor("#10B981"), false, 0.03f, Typeface.SANS_SERIF);

            float m2Y = card3Top + (card3Height * 0.68f);
            drawFixedText(canvas, "• Alpha Prototype Submission    In 8 Days", cx, m2Y, uiWidth * 0.030f,
                Color.parseColor("#38BDF8"), false, 0.03f, Typeface.SANS_SERIF);

            float m3Y = card3Top + (card3Height * 0.90f);
            drawFixedText(canvas, "• Production Sign-off                  In 25 Days", cx, m3Y, uiWidth * 0.030f,
                Color.parseColor("#A855F7"), false, 0.03f, Typeface.SANS_SERIF);

            // Footer Carousel hint
            float footerY = card3Top + card3Height + (mHeight * 0.035f);
            drawFixedText(canvas, "‹   ●  ○  ○   ›    AUTO-ROTATING", cx, footerY, uiWidth * 0.028f,
                Color.parseColor("#94A3B8"), true, 0.10f, Typeface.SANS_SERIF);
        }

        private void drawGlassPanel(Canvas canvas, float l, float t, float r, float b, float radius) {
            RectF rect = new RectF(l, t, r, b);

            // Translucent glass fill from app (bg-slate-950/60)
            mGlassCardPaint.setColor(Color.parseColor("#590F172A"));
            canvas.drawRoundRect(rect, radius, radius, mGlassCardPaint);

            // Border from app (border-white/10)
            mBorderPaint.setColor(Color.parseColor("#26FFFFFF"));
            mBorderPaint.setStrokeWidth(1.2f);
            canvas.drawRoundRect(rect, radius, radius, mBorderPaint);
        }

        // Draw 2D fixed motionless text
        private void drawFixedText(Canvas canvas, String text, float x, float y, float size,
                                   int color, boolean bold, float letterSpacing, Typeface tf) {
            mTextPaint.setTypeface(tf != null ? tf : Typeface.DEFAULT);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor(color);
            mTextPaint.setTextSize(size);
            mTextPaint.setFakeBoldText(bold);
            mTextPaint.setLetterSpacing(letterSpacing);
            mTextPaint.clearShadowLayer();
            canvas.drawText(text, x, y, mTextPaint);
        }

        // Draw 2D fixed countdown box (Days / Hours)
        private void drawFixedCountdownBox(Canvas canvas, float x, float y, float w, float h,
                                           String number, String label) {
            RectF boxRect = new RectF(x, y, x + w, y + h);

            // Container fill
            mGlassCardPaint.setColor(Color.parseColor("#66020617"));
            canvas.drawRoundRect(boxRect, 18f, 18f, mGlassCardPaint);

            mBorderPaint.setColor(Color.parseColor("#26FFFFFF"));
            mBorderPaint.setStrokeWidth(1.2f);
            canvas.drawRoundRect(boxRect, 18f, 18f, mBorderPaint);

            float midX = x + (w / 2f);

            // Big Number (font-mono-num font-extrabold text-white)
            float numY = y + (h * 0.60f);
            mTextPaint.setTypeface(Typeface.MONOSPACE);
            mTextPaint.setTextAlign(Paint.Align.CENTER);
            mTextPaint.setColor(Color.WHITE);
            mTextPaint.setTextSize(w * 0.52f);
            mTextPaint.setFakeBoldText(true);
            mTextPaint.setLetterSpacing(0f);
            mTextPaint.setShadowLayer(14f, 0, 0, Color.parseColor("#8038BDF8"));
            canvas.drawText(number, midX, numY, mTextPaint);
            mTextPaint.clearShadowLayer();

            // Label (text-sky-400 font-semibold uppercase)
            float labelY = y + (h * 0.85f);
            mTextPaint.setTypeface(Typeface.SANS_SERIF);
            mTextPaint.setColor(Color.parseColor("#38BDF8"));
            mTextPaint.setTextSize(w * 0.16f);
            mTextPaint.setFakeBoldText(true);
            mTextPaint.setLetterSpacing(0.08f);
            canvas.drawText(label, midX, labelY, mTextPaint);
        }

        private void drawProgressBar(Canvas canvas, float l, float t, float w, float h, float ratio) {
            RectF bg = new RectF(l, t, l + w, t + h);
            mGlassCardPaint.setColor(Color.parseColor("#4D020617"));
            canvas.drawRoundRect(bg, 3f, 3f, mGlassCardPaint);

            float fillW = w * ratio;
            RectF fill = new RectF(l, t, l + fillW, t + h);
            LinearGradient grad = new LinearGradient(
                l, t, l + fillW, t,
                new int[]{Color.parseColor("#38BDF8"), Color.parseColor("#818CF8"), Color.parseColor("#FB7185")},
                null, Shader.TileMode.CLAMP
            );
            mGlowPaint.setShader(grad);
            canvas.drawRoundRect(fill, 3f, 3f, mGlowPaint);
            mGlowPaint.setShader(null);
        }

        // Spoken Word Clock Generator (Matches index.html spokenWordClock)
        private String getSpokenWordClock(Calendar cal) {
            int hour24 = cal.get(Calendar.HOUR_OF_DAY);
            int hour12 = cal.get(Calendar.HOUR);
            if (hour12 == 0) hour12 = 12;
            int minute = cal.get(Calendar.MINUTE);

            String[] hourWords = {
                "", "ONE", "TWO", "THREE", "FOUR", "FIVE", "SIX",
                "SEVEN", "EIGHT", "NINE", "TEN", "ELEVEN", "TWELVE"
            };

            int nextHour = (hour12 % 12) + 1;
            String currentHourWord = hourWords[hour12];
            String nextHourWord = hourWords[nextHour];

            String timeOfDay;
            if (hour24 < 12) timeOfDay = "IN THE MORNING";
            else if (hour24 < 17) timeOfDay = "IN THE AFTERNOON";
            else if (hour24 < 21) timeOfDay = "IN THE EVENING";
            else timeOfDay = "AT NIGHT";

            if (minute == 0) {
                return "IT IS " + currentHourWord + " O'CLOCK " + timeOfDay;
            } else if (minute == 15) {
                return "IT IS A QUARTER PAST " + currentHourWord + " " + timeOfDay;
            } else if (minute == 30) {
                return "IT IS HALF PAST " + currentHourWord + " " + timeOfDay;
            } else if (minute == 45) {
                return "IT IS A QUARTER TO " + nextHourWord + " " + timeOfDay;
            } else if (minute < 30) {
                return "IT IS " + minute + " MINUTES PAST " + currentHourWord + " " + timeOfDay;
            } else {
                return "IT IS " + (60 - minute) + " MINUTES TO " + nextHourWord + " " + timeOfDay;
            }
        }

        @Override
        public void onDestroy() {
            mVisible = false;
            mHandler.removeCallbacks(mDrawRunnable);
            if (mBgBitmap1 != null && !mBgBitmap1.isRecycled()) {
                mBgBitmap1.recycle();
                mBgBitmap1 = null;
            }
            if (mBgBitmap2 != null && !mBgBitmap2.isRecycled()) {
                mBgBitmap2.recycle();
                mBgBitmap2 = null;
            }
            super.onDestroy();
        }
    }
}
