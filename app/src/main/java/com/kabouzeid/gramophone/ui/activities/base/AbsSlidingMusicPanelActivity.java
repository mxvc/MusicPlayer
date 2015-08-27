package com.kabouzeid.gramophone.ui.activities.base;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.ColorInt;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.util.Pair;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Property;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.ThemeSingleton;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.dialogs.AddToPlaylistDialog;
import com.kabouzeid.gramophone.dialogs.SleepTimerDialog;
import com.kabouzeid.gramophone.dialogs.SongDetailDialog;
import com.kabouzeid.gramophone.dialogs.SongShareDialog;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.imageloader.BlurProcessor;
import com.kabouzeid.gramophone.loader.SongLoader;
import com.kabouzeid.gramophone.misc.SimpleAnimatorListener;
import com.kabouzeid.gramophone.misc.SimpleOnSeekbarChangeListener;
import com.kabouzeid.gramophone.model.Song;
import com.kabouzeid.gramophone.service.MusicService;
import com.kabouzeid.gramophone.ui.activities.tageditor.AbsTagEditorActivity;
import com.kabouzeid.gramophone.ui.activities.tageditor.SongTagEditorActivity;
import com.kabouzeid.gramophone.util.ColorUtil;
import com.kabouzeid.gramophone.util.MusicUtil;
import com.kabouzeid.gramophone.util.NavigationUtil;
import com.kabouzeid.gramophone.util.PreferenceUtil;
import com.kabouzeid.gramophone.util.Util;
import com.kabouzeid.gramophone.util.ViewUtil;
import com.kabouzeid.gramophone.views.PlayPauseDrawable;
import com.kabouzeid.gramophone.views.SquareIfPlaceImageView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.lang.ref.WeakReference;

import butterknife.Bind;
import butterknife.ButterKnife;

/**
 * @author Karim Abou Zeid (kabouzeid)
 *         <p/>
 *         Do not use {@link #setContentView(int)} but wrap your layout with
 *         {@link #wrapSlidingMusicPanelAndFab(int)} first and then return it in {@link #createContentView()}
 */
public abstract class AbsSlidingMusicPanelActivity extends AbsMusicServiceActivity implements SlidingUpPanelLayout.PanelSlideListener, SharedPreferences.OnSharedPreferenceChangeListener, Toolbar.OnMenuItemClickListener {
    public static final String TAG = AbsSlidingMusicPanelActivity.class.getSimpleName();

    private static final int FAB_CIRCULAR_REVEAL_ANIMATION_TIME = 1000;
    private static final long DEFAULT_PROGRESS_VIEW_REFRESH_INTERVAL = 500;
    private static final int CMD_REFRESH_PROGRESS_VIEWS = 1;
    private static final int SLIDING_PANEL_ANIMATION_STEPS = 1000;

    @Bind(R.id.play_pause_fab)
    FloatingActionButton playPauseButton;
    @Bind(R.id.sliding_layout)
    SlidingUpPanelLayout slidingUpPanelLayout;

    @Bind(R.id.mini_player)
    FrameLayout miniPlayer;
    @Bind(R.id.mini_player_title)
    TextView miniPlayerTitle;
    @Bind(R.id.mini_player_image)
    ImageView miniPlayerImage;

    @Bind(R.id.player_dummy_fab)
    View dummyFab;

    @Bind(R.id.player_title)
    TextView songTitle;
    @Bind(R.id.player_text)
    TextView songText;
    @Bind(R.id.player_footer)
    LinearLayout footer;
    @Bind(R.id.player_playback_controller_card)
    CardView playbackControllerCard;
    @Bind(R.id.player_prev_button)
    ImageButton prevButton;
    @Bind(R.id.player_next_button)
    ImageButton nextButton;
    @Bind(R.id.player_repeat_button)
    ImageButton repeatButton;
    @Bind(R.id.player_shuffle_button)
    ImageButton shuffleButton;
    @Bind(R.id.player_media_controller_container)
    RelativeLayout mediaControllerContainer;
    @Bind(R.id.player_media_controller_container_background)
    View mediaControllerContainerBackground;
    @Bind(R.id.player_footer_frame)
    LinearLayout footerFrame;
    @Bind(R.id.player_album_art_background)
    ImageView albumArtBackground;
    @Bind(R.id.player_image)
    SquareIfPlaceImageView albumArt;
    @Bind(R.id.player_status_bar)
    View playerStatusbar;
    @Bind(R.id.player_toolbar)
    Toolbar playerToolbar;
    @Bind(R.id.player_favorite_icon)
    ImageView favoriteIcon;

    TextView songCurrentProgress;
    TextView songTotalTime;
    SeekBar progressSlider;

    private int lastFooterColor;
    private int lastPlaybackControlsColor;
    private int lastTitleTextColor;
    private int lastCaptionTextColor;

    private int navigationBarColor;
    private int taskColor;

    private Handler progressViewsUpdateHandler;

    private boolean opaqueStatusBar;
    private boolean opaqueToolBar;
    private boolean forceSquareAlbumArt;
    private boolean largerTitleBox;
    private boolean alternativeProgressSlider;
    private boolean showPlaybackControllerCard;
    private boolean colorPlaybackControls;

    private Song song;

    private PlayPauseDrawable playPauseDrawable;

    private AnimatorSet colorTransitionAnimator;

    private ObjectAnimator fabXAnimator;
    private ObjectAnimator fabYAnimator;
    private ObjectAnimator fabColorAnimator;
    private int lastFabStartColor;
    private int lastFabEndColor;
    private ObjectAnimator miniPlayerAlphaAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
        ButterKnife.bind(this);

        initAppearanceVarsFromSharedPrefs();
        PreferenceUtil.getInstance(this).registerOnSharedPreferenceChangedListener(this);

        setUpPlayPauseButton();
        setUpMiniPlayer();
        setUpSlidingPanel();

        adjustTitleBoxSize();
        setUpPlaybackControllerCard();
        setUpMusicControllers();
        setUpAlbumArtViews();
        setUpPlayerToolbar();
        setUpPlayerStatusBarElevation();

        progressViewsUpdateHandler = new MusicProgressViewsUpdateHandler(this);

        // I know the nested post calls are ugly, but this is necessary for the fab to be in the right position!
        playPauseButton.post(new Runnable() {
            @Override
            public void run() {
                dummyFab.post(new Runnable() {
                    @Override
                    public void run() {
                        playPauseButton.requestLayout();
                        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
                            mediaControllerContainer.setVisibility(View.VISIBLE);
                            onPanelSlide(slidingUpPanelLayout, 1);
                            onPanelExpanded(slidingUpPanelLayout);
                        }
                    }
                });
            }
        });
    }

    protected abstract View createContentView();

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PreferenceUtil.getInstance(this).unregisterOnSharedPreferenceChangedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startUpdatingProgressViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopUpdatingProgressViews();
    }

    @Override
    public void onPlayingMetaChanged() {
        super.onPlayingMetaChanged();
        updateCurrentSong();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        hideBottomBarIfQueueIsEmpty();
        super.onServiceConnected(name, service);
    }

    @Override
    public void onRepeatModeChanged() {
        super.onRepeatModeChanged();
        updateRepeatState();
    }

    @Override
    public void onShuffleModeChanged() {
        super.onShuffleModeChanged();
        updateShuffleState();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        switch (key) {
            case PreferenceUtil.OPAQUE_STATUSBAR_NOW_PLAYING:
                opaqueStatusBar = PreferenceUtil.getInstance(this).opaqueStatusbarNowPlaying();
                // do not break here
            case PreferenceUtil.OPAQUE_TOOLBAR_NOW_PLAYING:
                opaqueToolBar = opaqueStatusBar && PreferenceUtil.getInstance(this).opaqueToolbarNowPlaying();
                setUpPlayerStatusBarElevation();
                animateColorChange(lastFooterColor);
                if (opaqueStatusBar) {
                    if (opaqueToolBar) {
                        alignAlbumArtToToolbar();
                    } else {
                        alignAlbumArtToStatusBar();
                    }
                } else {
                    alignAlbumArtToTop();
                }
                break;
            case PreferenceUtil.FORCE_SQUARE_ALBUM_ART:
                forceSquareAlbumArt = PreferenceUtil.getInstance(this).forceAlbumArtSquared();
                albumArt.forceSquare(forceSquareAlbumArt);
                break;
            case PreferenceUtil.LARGER_TITLE_BOX_NOW_PLAYING:
                largerTitleBox = PreferenceUtil.getInstance(this).largerTitleBoxNowPlaying();
                adjustTitleBoxSize();
                break;
            case PreferenceUtil.ALTERNATIVE_PROGRESS_SLIDER_NOW_PLAYING:
                alternativeProgressSlider = PreferenceUtil.getInstance(this).alternativeProgressSliderNowPlaying();
                setUpProgressSlider();
                break;
            case PreferenceUtil.PLAYBACK_CONTROLLER_CARD_NOW_PLAYING:
                showPlaybackControllerCard = PreferenceUtil.getInstance(this).playbackControllerCardNowPlaying();
                setUpPlaybackControllerCard();
                break;
            case PreferenceUtil.COLOR_PLAYBACK_CONTROLS_NOW_PLAYING:
                colorPlaybackControls = PreferenceUtil.getInstance(this).colorPlaybackControlsNowPlaying();
                updateRepeatState();
                updateShuffleState();
                setUpProgressSliderTint();
                setUpPlayPauseButtonTint();
                break;
            case PreferenceUtil.HIDE_BOTTOM_BAR:
                recreate();
                break;
        }
    }

    private void setUpPlayPauseButton() {
        updateFabState(false);

        playPauseButton.setImageDrawable(playPauseDrawable);

        setUpPlayPauseButtonTint();

        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                toggleSlidingPanel();
                return true;
            }
        });

        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MusicPlayerRemote.getPosition() != -1) {
                    if (MusicPlayerRemote.isPlaying()) {
                        MusicPlayerRemote.pauseSong();
                    } else {
                        MusicPlayerRemote.resumePlaying();
                    }
                }
            }
        });

        playPauseButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, @NonNull MotionEvent event) {
                gestureDetector.onTouchEvent(event);
                return false;
            }
        });
    }

    private void setUpPlayPauseButtonTint() {
        int fabColor = colorPlaybackControls && slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED ? lastPlaybackControlsColor : getThemeColorAccent();
        FloatingActionButtonProperty.FAB_COLOR.set(playPauseButton, fabColor);
    }

    private void setUpMiniPlayer() {
        hideBottomBar(PreferenceUtil.getInstance(this).hideBottomBar());
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (Math.abs(velocityX) > Math.abs(velocityY)) {
                    if (velocityX < 0) {
                        MusicPlayerRemote.playNextSong();
                        return true;
                    } else if (velocityX > 0) {
                        MusicPlayerRemote.back();
                        return true;
                    }
                }
                return false;
            }
        });

        miniPlayer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });

        miniPlayer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSlidingPanel();
            }
        });

        setMiniPlayerColor(ColorUtil.resolveColor(this, R.attr.cardBackgroundColor));

        miniPlayerImage.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_equalizer_white_24dp,
                ColorUtil.resolveColor(this, android.R.attr.textColorSecondary)));
    }

    public void setMiniPlayerColor(int color) {
        miniPlayer.setBackgroundColor(color);
        miniPlayerTitle.setTextColor(ColorUtil.getPrimaryTextColorForBackground(this, color));
    }

    private void setUpSlidingPanel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaControllerContainer.setVisibility(View.INVISIBLE);
        }
        slidingUpPanelLayout.setPanelSlideListener(this);
    }

    @Override
    public void onPanelSlide(View view, float slideOffset) {
        initFabXAnimatorIfNecessary();
        initFabYAnimatorIfNecessary();
        initFabColorAnimatorIfNecessary();
        initMiniPlayerAlphaAnimatorIfNecessary();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            fabXAnimator.setCurrentFraction(slideOffset);
            fabYAnimator.setCurrentFraction(slideOffset);
            fabColorAnimator.setCurrentFraction(slideOffset);
            miniPlayerAlphaAnimator.setCurrentFraction(slideOffset);
        } else {
            int durationProgress = (int) (SLIDING_PANEL_ANIMATION_STEPS * slideOffset);
            fabXAnimator.setCurrentPlayTime(durationProgress);
            fabYAnimator.setCurrentPlayTime(durationProgress);
            fabColorAnimator.setCurrentPlayTime(durationProgress);
            miniPlayerAlphaAnimator.setCurrentPlayTime(durationProgress);
        }
    }

    @Override
    public void onPanelCollapsed(View view) {
        super.notifyTaskColorChange(taskColor);
        if (shouldColorNavigationBar()) {
            super.setNavigationBarColor(navigationBarColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaControllerContainer.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onPanelExpanded(View view) {
        super.notifyTaskColorChange(lastFooterColor);
        if (shouldColorNavigationBar()) {
            super.setNavigationBarColor(lastFooterColor);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mediaControllerContainer.getVisibility() == View.INVISIBLE) {
                int cx = (dummyFab.getLeft() + dummyFab.getRight()) / 2;
                int cy = (dummyFab.getTop() + dummyFab.getBottom()) / 2;
                int finalRadius = Math.max(mediaControllerContainer.getWidth(), mediaControllerContainer.getHeight());

                final Animator animator = ViewAnimationUtils.createCircularReveal(mediaControllerContainer, cx, cy, dummyFab.getWidth() / 2, finalRadius);
                animator.setInterpolator(new DecelerateInterpolator());
                animator.setDuration(FAB_CIRCULAR_REVEAL_ANIMATION_TIME);
                animator.start();

                mediaControllerContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onPanelAnchored(View view) {

    }

    @Override
    public void onPanelHidden(View view) {

    }

    private void initFabXAnimatorIfNecessary() {
        if (fabXAnimator == null) {
            float xTranslation = dummyFab.getX() + mediaControllerContainer.getX() + footerFrame.getX() - playPauseButton.getLeft();
            fabXAnimator = ObjectAnimator.ofFloat(playPauseButton, View.TRANSLATION_X, 0, xTranslation);
            fabXAnimator.setDuration(SLIDING_PANEL_ANIMATION_STEPS);
            fabXAnimator.setInterpolator(null);
        }
    }

    private void initFabYAnimatorIfNecessary() {
        if (fabYAnimator == null) {
            float yTranslation = dummyFab.getY() + mediaControllerContainer.getY() + footerFrame.getY() - playPauseButton.getTop();
            fabYAnimator = ObjectAnimator.ofFloat(playPauseButton, View.TRANSLATION_Y, 0, yTranslation);
            fabYAnimator.setDuration(SLIDING_PANEL_ANIMATION_STEPS);
            fabYAnimator.setInterpolator(null);
        }
    }

    private void initFabColorAnimatorIfNecessary() {
        int startColor = getThemeColorAccent();
        int endColor;
        if (colorPlaybackControls) {
            endColor = lastPlaybackControlsColor;
        } else {
            endColor = startColor;
        }
        if (fabColorAnimator == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                fabColorAnimator = ObjectAnimator.ofArgb(playPauseButton, FloatingActionButtonProperty.FAB_COLOR, startColor, endColor);
            } else {
                fabColorAnimator = ObjectAnimator.ofInt(playPauseButton, FloatingActionButtonProperty.FAB_COLOR, startColor, endColor);
                fabColorAnimator.setEvaluator(new ArgbEvaluator());
            }
            fabColorAnimator.setDuration(SLIDING_PANEL_ANIMATION_STEPS);
            fabColorAnimator.setInterpolator(null);
        } else if (startColor != lastFabStartColor || endColor != lastFabEndColor) {
            fabColorAnimator.setIntValues(startColor, endColor);
        }
        lastFabStartColor = startColor;
        lastFabEndColor = endColor;
    }

    private void initMiniPlayerAlphaAnimatorIfNecessary() {
        if (miniPlayerAlphaAnimator == null) {
            miniPlayerAlphaAnimator = ObjectAnimator.ofFloat(miniPlayer, View.ALPHA, 1, 0);
            miniPlayerAlphaAnimator.setDuration(SLIDING_PANEL_ANIMATION_STEPS);
            miniPlayerAlphaAnimator.setInterpolator(null);
        }
    }

    private void toggleSlidingPanel() {
        if (slidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.EXPANDED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
        } else {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    public SlidingUpPanelLayout getSlidingUpPanelLayout() {
        return slidingUpPanelLayout;
    }

    public void hideBottomBar(boolean hide) {
        if (hide) {
            slidingUpPanelLayout.post(new Runnable() {
                @Override
                public void run() {
                    slidingUpPanelLayout.setPanelHeight(0);
                }
            });
        } else {
            slidingUpPanelLayout.post(new Runnable() {
                @Override
                public void run() {
                    slidingUpPanelLayout.setPanelHeight(getResources().getDimensionPixelSize(R.dimen.mini_player_height));
                }
            });
        }
    }

    public int getBottomOffset() {
        return getResources().getDimensionPixelSize(R.dimen.bottom_offset_fab_activity) - slidingUpPanelLayout.getPanelHeight();
    }

    protected void updateFabState(boolean animate) {
        if (playPauseDrawable == null) {
            playPauseDrawable = new PlayPauseDrawable(this);
        }
        if (MusicPlayerRemote.isPlaying()) {
            playPauseDrawable.setPause(animate);
        } else {
            playPauseDrawable.setPlay(animate);
        }
    }

    public Pair[] addPlayPauseFabToSharedViews(@Nullable Pair[] sharedViews) {
        Pair[] sharedViewsWithFab;
        if (sharedViews != null) {
            sharedViewsWithFab = new Pair[sharedViews.length + 1];
            System.arraycopy(sharedViews, 0, sharedViewsWithFab, 0, sharedViews.length);
        } else {
            sharedViewsWithFab = new Pair[1];
        }
        sharedViewsWithFab[sharedViewsWithFab.length - 1] = Pair.create((View) playPauseButton, getString(R.string.transition_fab));
        return sharedViewsWithFab;
    }

    @Override
    public void onPlayStateChanged() {
        super.onPlayStateChanged();
        updateFabState(true);
    }

    protected View wrapSlidingMusicPanelAndFab(@LayoutRes int resId) {
        @SuppressLint("InflateParams")
        View slidingMusicPanelLayout = getLayoutInflater().inflate(R.layout.sliding_music_panel_layout, null);
        ViewGroup contentContainer = ButterKnife.findById(slidingMusicPanelLayout, R.id.content_container);
        getLayoutInflater().inflate(resId, contentContainer);
        return slidingMusicPanelLayout;
    }

    @Override
    public void onBackPressed() {
        if (slidingUpPanelLayout.getPanelState() != SlidingUpPanelLayout.PanelState.COLLAPSED) {
            slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }
        super.onBackPressed();
    }

    private void initAppearanceVarsFromSharedPrefs() {
        opaqueStatusBar = PreferenceUtil.getInstance(this).opaqueStatusbarNowPlaying();
        opaqueToolBar = opaqueStatusBar && PreferenceUtil.getInstance(this).opaqueToolbarNowPlaying();
        forceSquareAlbumArt = PreferenceUtil.getInstance(this).forceAlbumArtSquared();
        largerTitleBox = PreferenceUtil.getInstance(this).largerTitleBoxNowPlaying();
        alternativeProgressSlider = PreferenceUtil.getInstance(this).alternativeProgressSliderNowPlaying();
        showPlaybackControllerCard = PreferenceUtil.getInstance(this).playbackControllerCardNowPlaying();
        colorPlaybackControls = PreferenceUtil.getInstance(this).colorPlaybackControlsNowPlaying();
    }

    private void initProgressSliderDependentViews() {
        if (alternativeProgressSlider) {
            findViewById(R.id.player_default_progress_container).setVisibility(View.GONE);
            findViewById(R.id.player_default_progress_slider).setVisibility(View.GONE);
            findViewById(R.id.player_alternative_progress_container).setVisibility(View.VISIBLE);

            songCurrentProgress = (TextView) findViewById(R.id.player_alternative_song_current_progress);
            songTotalTime = (TextView) findViewById(R.id.player_alternative_song_total_time);
            progressSlider = (SeekBar) findViewById(R.id.player_alternative_progress_slider);
        } else {
            findViewById(R.id.player_default_progress_container).setVisibility(View.VISIBLE);
            findViewById(R.id.player_default_progress_slider).setVisibility(View.VISIBLE);
            findViewById(R.id.player_alternative_progress_container).setVisibility(View.GONE);

            songCurrentProgress = (TextView) findViewById(R.id.player_default_song_current_progress);
            songTotalTime = (TextView) findViewById(R.id.player_default_song_total_time);
            progressSlider = (SeekBar) findViewById(R.id.player_default_progress_slider);
        }
    }

    private void moveProgressSliderIntoPlace() {
        if (!alternativeProgressSlider) {
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) progressSlider.getLayoutParams();
            progressSlider.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            final int seekBarMarginLeftRight = getResources().getDimensionPixelSize(R.dimen.seek_bar_margin_left_right);
            lp.setMargins(seekBarMarginLeftRight, 0, seekBarMarginLeftRight, -(progressSlider.getMeasuredHeight() / 2));
            progressSlider.setLayoutParams(lp);
        }
    }

    private void adjustTitleBoxSize() {
        int paddingTopBottom = largerTitleBox ? getResources().getDimensionPixelSize(R.dimen.title_box_padding_large) : getResources().getDimensionPixelSize(R.dimen.title_box_padding_small);
        footer.setPadding(footer.getPaddingLeft(), paddingTopBottom, footer.getPaddingRight(), paddingTopBottom);

        songTitle.setPadding(songTitle.getPaddingLeft(), songTitle.getPaddingTop(), songTitle.getPaddingRight(), largerTitleBox ? getResources().getDimensionPixelSize(R.dimen.title_box_text_spacing_large) : getResources().getDimensionPixelSize(R.dimen.title_box_text_spacing_small));
        songText.setPadding(songText.getPaddingLeft(), largerTitleBox ? getResources().getDimensionPixelSize(R.dimen.title_box_text_spacing_large) : getResources().getDimensionPixelSize(R.dimen.title_box_text_spacing_small), songText.getPaddingRight(), songText.getPaddingBottom());

        songTitle.setTextSize(TypedValue.COMPLEX_UNIT_PX, largerTitleBox ? getResources().getDimensionPixelSize(R.dimen.title_box_title_text_size_large) : getResources().getDimensionPixelSize(R.dimen.title_box_title_text_size_small));
        songText.setTextSize(TypedValue.COMPLEX_UNIT_PX, largerTitleBox ? getResources().getDimensionPixelSize(R.dimen.title_box_caption_text_size_large) : getResources().getDimensionPixelSize(R.dimen.title_box_caption_text_size_small));
    }

    private void setUpPlaybackControllerCard() {
        playbackControllerCard.setVisibility(showPlaybackControllerCard ? View.VISIBLE : View.GONE);
        mediaControllerContainerBackground.setVisibility(showPlaybackControllerCard ? View.GONE : View.VISIBLE);
    }

    private void setUpMusicControllers() {
        setUpPrevNext();
        setUpRepeatButton();
        setUpShuffleButton();
        setUpProgressSlider();
    }

    private void setUpProgressSliderTint() {
        int thumbColor;
        int progressColor;
        if (alternativeProgressSlider) {
            if (colorPlaybackControls) {
                thumbColor = lastPlaybackControlsColor;
            } else {
                thumbColor = ThemeSingleton.get().positiveColor.getDefaultColor();
            }
            progressColor = Color.TRANSPARENT;
        } else {
            if (colorPlaybackControls) {
                if (ColorUtil.useDarkTextColorOnBackground(lastPlaybackControlsColor)) {
                    thumbColor = shiftColorDown(lastPlaybackControlsColor);
                } else {
                    thumbColor = shiftColorUp(lastPlaybackControlsColor);
                }
            } else {
                thumbColor = getThemeColorAccent();
            }
            progressColor = thumbColor;
        }
        setSeekBarTint(progressSlider, thumbColor, progressColor);
    }

    @ColorInt
    private static int shiftColorUp(@ColorInt int color) {
        return ColorUtil.shiftColor(color, 1.2f);
    }

    @ColorInt
    private static int shiftColorDown(@ColorInt int color) {
        return ColorUtil.shiftColor(color, 0.8f);
    }

    private static void setSeekBarTint(SeekBar seekBar, @ColorInt int thumbColor, @ColorInt int progressColor) {
        seekBar.getThumb().mutate().setColorFilter(thumbColor, PorterDuff.Mode.SRC_IN);
        seekBar.getProgressDrawable().mutate().setColorFilter(progressColor, PorterDuff.Mode.SRC_IN);
    }

    private void setUpProgressSlider() {
        initProgressSliderDependentViews();
        moveProgressSliderIntoPlace();
        setUpProgressSliderTint();
        progressSlider.setOnSeekBarChangeListener(new SimpleOnSeekbarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MusicPlayerRemote.seekTo(progress);
                    refreshProgressViews();
                }
            }
        });
    }

    private void setUpPrevNext() {
        int themedDrawableColor = ColorUtil.resolveColor(this, android.R.attr.textColorSecondary);
        nextButton.setImageDrawable(Util.getTintedDrawable(this,
                R.drawable.ic_skip_next_white_36dp, themedDrawableColor));
        prevButton.setImageDrawable(Util.getTintedDrawable(this,
                R.drawable.ic_skip_previous_white_36dp, themedDrawableColor));
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicPlayerRemote.playNextSong();
            }
        });
        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicPlayerRemote.back();
            }
        });
    }

    private void setUpShuffleButton() {
        updateShuffleState();
        shuffleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicPlayerRemote.toggleShuffleMode();
            }
        });
    }

    private void updateShuffleState() {
        switch (MusicPlayerRemote.getShuffleMode()) {
            case MusicService.SHUFFLE_MODE_SHUFFLE:
                int activatedColor = colorPlaybackControls ? lastPlaybackControlsColor : ThemeSingleton.get().positiveColor.getDefaultColor();
                shuffleButton.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_shuffle_white_36dp,
                        activatedColor));
                break;
            default:
                int deactivatedColor = ColorUtil.resolveColor(this, android.R.attr.textColorSecondary);
                shuffleButton.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_shuffle_white_36dp,
                        deactivatedColor));
                break;
        }
    }

    private void setUpRepeatButton() {
        updateRepeatState();
        repeatButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MusicPlayerRemote.cycleRepeatMode();
            }
        });
    }

    private void updateRepeatState() {
        int activatedColor = colorPlaybackControls ? lastPlaybackControlsColor : ThemeSingleton.get().positiveColor.getDefaultColor();
        switch (MusicPlayerRemote.getRepeatMode()) {
            case MusicService.REPEAT_MODE_ALL:
                repeatButton.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_repeat_white_36dp,
                        activatedColor));
                break;
            case MusicService.REPEAT_MODE_THIS:
                repeatButton.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_repeat_one_white_36dp,
                        activatedColor));
                break;
            default:
                int deactivatedColor = ColorUtil.resolveColor(this, android.R.attr.textColorSecondary);
                repeatButton.setImageDrawable(Util.getTintedDrawable(this, R.drawable.ic_repeat_white_36dp,
                        deactivatedColor));
                break;
        }
    }

    private void setUpAlbumArtViews() {
        albumArtBackground.setAlpha(0.7f);
        albumArt.forceSquare(forceSquareAlbumArt);
        if (opaqueStatusBar) {
            if (opaqueToolBar) {
                alignAlbumArtToToolbar();
            } else {
                alignAlbumArtToStatusBar();
            }
        } else {
            alignAlbumArtToTop();
        }
    }

    private void alignAlbumArtToTop() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.player_album_art_frame).getLayoutParams();
        if (Build.VERSION.SDK_INT > 16) {
            params.removeRule(RelativeLayout.BELOW);
        } else {
            params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            params.addRule(RelativeLayout.ABOVE, R.id.player_footer_frame);
        }
    }

    private void alignAlbumArtToToolbar() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.player_album_art_frame).getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.player_toolbar);
    }

    private void alignAlbumArtToStatusBar() {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) findViewById(R.id.player_album_art_frame).getLayoutParams();
        params.addRule(RelativeLayout.BELOW, R.id.player_status_bar);
    }

    private void setUpPlayerToolbar() {
        playerToolbar.inflateMenu(R.menu.menu_player);
        playerToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        playerToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
        playerToolbar.setOnMenuItemClickListener(this);
    }

    private void setUpPlayerStatusBarElevation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (opaqueToolBar) {
                playerStatusbar.setElevation(getResources().getDimensionPixelSize(R.dimen.toolbar_elevation));
            } else {
                playerStatusbar.setElevation(0);
            }
        }
    }

    private void updatePlayerMenu() {
        boolean isFavorite = MusicUtil.isFavorite(this, song);
        playerToolbar.getMenu().findItem(R.id.action_toggle_favorite)
                .setIcon(isFavorite ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp)
                .setTitle(isFavorite ? getString(R.string.action_remove_from_favorites) : getString(R.string.action_add_to_favorites));
    }

    private void updateCurrentSong() {
        hideBottomBarIfQueueIsEmpty();
        getCurrentSong();
        updateMiniPlayerAndHeaderText();
        setUpAlbumArtAndApplyPalette();
        updatePlayerMenu();
    }

    private void hideBottomBarIfQueueIsEmpty() {
        if (MusicPlayerRemote.getPlayingQueue().isEmpty()) {
            playPauseButton.setVisibility(View.GONE);
            hideBottomBar(true);
        } else {
            playPauseButton.setVisibility(View.VISIBLE);
            hideBottomBar(PreferenceUtil.getInstance(this).hideBottomBar());
        }
    }

    private void getCurrentSong() {
        song = MusicPlayerRemote.getCurrentSong();
    }

    private void updateMiniPlayerAndHeaderText() {
        songTitle.setText(song.title);
        songText.setText(song.artistName);

        miniPlayerTitle.setText(song.title);
    }

    private void setUpAlbumArtAndApplyPalette() {
        ImageLoader.getInstance().displayImage(
                MusicUtil.getSongImageLoaderString(song),
                albumArt,
                new DisplayImageOptions.Builder()
                        .cacheInMemory(true)
                        .showImageOnFail(R.drawable.default_album_art)
                        .build(),
                new SimpleImageLoadingListener() {
                    @Override
                    public void onLoadingFailed(String imageUri, View view, @Nullable FailReason failReason) {
                        applyPalette(null);

                        ImageLoader.getInstance().displayImage(
                                "drawable://" + R.drawable.default_album_art,
                                albumArtBackground,
                                new DisplayImageOptions.Builder().postProcessor(new BlurProcessor()).build()
                        );
                    }

                    @Override
                    public void onLoadingComplete(String imageUri, View view, @Nullable Bitmap loadedImage) {
                        if (loadedImage == null) {
                            onLoadingFailed(imageUri, view, null);
                            return;
                        }

                        applyPalette(loadedImage);

                        ImageLoader.getInstance().displayImage(
                                imageUri,
                                albumArtBackground,
                                new DisplayImageOptions.Builder().postProcessor(new BlurProcessor()).build()
                        );
                    }
                }
        );
    }

    private void applyPalette(@Nullable Bitmap bitmap) {
        if (bitmap != null) {
            Palette.from(bitmap)
                    .resizeBitmapSize(ColorUtil.PALETTE_BITMAP_SIZE)
                    .generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(@NonNull Palette palette) {
                            setColors(ColorUtil.getColor(AbsSlidingMusicPanelActivity.this, palette));
                        }
                    });
        } else {
            setColors(ColorUtil.getColor(AbsSlidingMusicPanelActivity.this, null));
        }
    }

    private void setColors(int color) {
        animateColorChange(color);

        if (slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            super.notifyTaskColorChange(color);
            if (shouldColorNavigationBar()) {
                super.setNavigationBarColor(color);
            }
        }
    }

    private void animateColorChange(final int newColor) {
        if (colorTransitionAnimator != null && colorTransitionAnimator.isStarted()) {
            colorTransitionAnimator.cancel();
        }
        colorTransitionAnimator = new AnimatorSet();
        AnimatorSet.Builder animatorSetBuilder = colorTransitionAnimator.play(ViewUtil.createBackgroundColorTransition(footer, lastFooterColor, newColor));

        if (opaqueToolBar) {
            animatorSetBuilder.with(ViewUtil.createBackgroundColorTransition(playerToolbar, lastFooterColor, newColor));
        } else {
            playerToolbar.setBackgroundColor(Color.TRANSPARENT);
        }

        if (opaqueStatusBar) {
            int newStatusbarColor = newColor;
            int oldStatusbarColor = lastFooterColor;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                newStatusbarColor = ColorUtil.shiftColorDown(newStatusbarColor);
                oldStatusbarColor = ColorUtil.shiftColorDown(oldStatusbarColor);
            }
            animatorSetBuilder.with(ViewUtil.createBackgroundColorTransition(playerStatusbar, oldStatusbarColor, newStatusbarColor));
        } else {
            playerStatusbar.setBackgroundColor(Color.TRANSPARENT);
        }

        int titleTextColor = ColorUtil.getPrimaryTextColorForBackground(this, newColor);
        int captionTextColor = ColorUtil.getSecondaryTextColorForBackground(this, newColor);

        animatorSetBuilder.with(ViewUtil.createTextColorTransition(songTitle, lastTitleTextColor, titleTextColor));
        animatorSetBuilder.with(ViewUtil.createTextColorTransition(songText, lastCaptionTextColor, captionTextColor));

        colorTransitionAnimator.addListener(new SimpleAnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                if (newColor == ColorUtil.resolveColor(AbsSlidingMusicPanelActivity.this, R.attr.default_bar_color) && ThemeSingleton.get().darkTheme) {
                    lastPlaybackControlsColor = Color.WHITE;
                } else {
                    lastPlaybackControlsColor = newColor;
                }
                updateRepeatState();
                updateShuffleState();
                setUpProgressSliderTint();
                setUpPlayPauseButtonTint();
            }
        });

        colorTransitionAnimator.start();

        lastFooterColor = newColor;
        lastTitleTextColor = titleTextColor;
        lastCaptionTextColor = captionTextColor;
    }

    private void startUpdatingProgressViews() {
        queueNextRefresh(1);
    }

    private void stopUpdatingProgressViews() {
        progressViewsUpdateHandler.removeMessages(CMD_REFRESH_PROGRESS_VIEWS);
    }

    private void queueNextRefresh(final long delay) {
        final Message message = progressViewsUpdateHandler.obtainMessage(CMD_REFRESH_PROGRESS_VIEWS);
        progressViewsUpdateHandler.removeMessages(CMD_REFRESH_PROGRESS_VIEWS);
        progressViewsUpdateHandler.sendMessageDelayed(message, delay);
    }

    private long refreshProgressViews() {
        final int totalMillis = MusicPlayerRemote.getSongDurationMillis();
        final int progressMillis = MusicPlayerRemote.getSongProgressMillis();

        progressSlider.setMax(totalMillis);
        progressSlider.setProgress(progressMillis);
        songCurrentProgress.setText(MusicUtil.getReadableDurationString(progressMillis));
        songTotalTime.setText(MusicUtil.getReadableDurationString(totalMillis));

        if (!MusicPlayerRemote.isPlaying()) {
            return DEFAULT_PROGRESS_VIEW_REFRESH_INTERVAL;
        }

        // calculate the number of milliseconds until the next full second,
        // so
        // the counter can be updated at just the right time
        final long remainingMillis = 1000 - progressMillis % 1000;
        if (remainingMillis < 20) {
            return 20;
        }

        return remainingMillis;
    }

    private static class MusicProgressViewsUpdateHandler extends Handler {
        private WeakReference<AbsSlidingMusicPanelActivity> activityReference;

        public MusicProgressViewsUpdateHandler(final AbsSlidingMusicPanelActivity activity) {
            super();
            activityReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == CMD_REFRESH_PROGRESS_VIEWS) {
                AbsSlidingMusicPanelActivity activity = activityReference.get();
                if (activity != null) {
                    long nextDelay = activityReference.get().refreshProgressViews();
                    activityReference.get().queueNextRefresh(nextDelay);
                }
            }
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_sleep_timer:
                new SleepTimerDialog().show(getSupportFragmentManager(), "SET_SLEEP_TIMER");
                return true;
            case R.id.action_toggle_favorite:
                MusicUtil.toggleFavorite(this, song);
                if (MusicUtil.isFavorite(this, song)) {
                    animateSetFavorite();
                }
                updatePlayerMenu();
                return true;
            case R.id.action_share:
                SongShareDialog.create(song).show(getSupportFragmentManager(), "SHARE_SONG");
                return true;
            case R.id.action_equalizer:
                NavigationUtil.openEqualizer(this);
                return true;
            case R.id.action_shuffle_all:
                MusicPlayerRemote.openAndShuffleQueue(SongLoader.getAllSongs(this), true);
                return true;
            case R.id.action_add_to_playlist:
                AddToPlaylistDialog.create(song).show(getSupportFragmentManager(), "ADD_PLAYLIST");
                return true;
            case R.id.action_playing_queue:
                NavigationUtil.openPlayingQueueDialog(this);
                return true;
            case R.id.action_tag_editor:
                Intent intent = new Intent(this, SongTagEditorActivity.class);
                intent.putExtra(AbsTagEditorActivity.EXTRA_ID, song.id);
                startActivity(intent);
                return true;
            case R.id.action_details:
                SongDetailDialog.create(song).show(getSupportFragmentManager(), "SONG_DETAIL");
                return true;
            case R.id.action_go_to_album:
                NavigationUtil.goToAlbum(this, song.albumId, addPlayPauseFabToSharedViews(null));
                return true;
            case R.id.action_go_to_artist:
                NavigationUtil.goToArtist(this, song.artistId, addPlayPauseFabToSharedViews(null));
                return true;
        }
        return false;
    }

    private void animateSetFavorite() {
        favoriteIcon.clearAnimation();

        favoriteIcon.setAlpha(0f);
        favoriteIcon.setScaleX(0f);
        favoriteIcon.setScaleY(0f);
        favoriteIcon.setVisibility(View.VISIBLE);
        favoriteIcon.setPivotX(favoriteIcon.getWidth() / 2);
        favoriteIcon.setPivotY(favoriteIcon.getHeight() / 2);

        favoriteIcon.animate()
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator())
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setListener(new SimpleAnimatorListener() {
                    @Override
                    public void onAnimationCancel(Animator animation) {
                        favoriteIcon.setVisibility(View.INVISIBLE);
                    }
                })
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        favoriteIcon.animate()
                                .setDuration(300)
                                .setInterpolator(new DecelerateInterpolator())
                                .alpha(0f)
                                .start();
                    }
                })
                .start();
    }

    @Override
    protected void setNavigationBarColor(@ColorInt int color) {
        this.navigationBarColor = color;
        if (slidingUpPanelLayout == null || slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            super.setNavigationBarColor(color);
        }
    }

    @Override
    protected void notifyTaskColorChange(@ColorInt int color) {
        this.taskColor = color;
        if (slidingUpPanelLayout == null || slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.COLLAPSED) {
            super.notifyTaskColorChange(color);
        }
    }

    private static class FloatingActionButtonProperty {
        public static final Property<FloatingActionButton, Integer> FAB_COLOR = new Property<FloatingActionButton, Integer>(Integer.class, "fabColor") {
            @Override
            public void set(FloatingActionButton object, Integer value) {
                object.setBackgroundTintList(ColorStateList.valueOf(value));
                object.getDrawable().setColorFilter(ColorUtil.getFabDrawableColorForBackground(object.getContext(), value), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public Integer get(FloatingActionButton object) {
                return object.getBackgroundTintList() != null ? object.getBackgroundTintList().getDefaultColor() : 0;
            }
        };
    }
}
