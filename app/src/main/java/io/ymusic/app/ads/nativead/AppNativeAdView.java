package io.ymusic.app.ads.nativead;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;

import io.ymusic.app.R;

/**
 * Base class for a template view. *
 */
public class AppNativeAdView extends FrameLayout {

    private int templateType;
    private NativeAdStyle styles;
    private NativeAd nativeAd;
    private NativeAdView nativeAdView;

    private ImageView iconView;
    private TextView primaryView;
    private TextView bodyView;
    private MediaView mediaView;
    private Button callToActionView;
    private ConstraintLayout background;

    private static final String SMALL_TEMPLATE = "small_template";

    public AppNativeAdView(Context context) {
        super(context);
    }

    public AppNativeAdView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public AppNativeAdView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    public AppNativeAdView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    public void setStyles(NativeAdStyle styles) {
        this.styles = styles;
        this.applyStyles();
    }

    public NativeAdView getNativeAdView() {
        return nativeAdView;
    }

    private void applyStyles() {
        Drawable mainBackground = styles.getMainBackgroundColor();
        if (mainBackground != null) {
            background.setBackground(mainBackground);
            if (primaryView != null) {
                primaryView.setBackground(mainBackground);
            }
            if (bodyView != null) {
                bodyView.setBackground(mainBackground);
            }
        }

        Typeface primary = styles.getPrimaryTextTypeface();
        if (primary != null && primaryView != null) {
            primaryView.setTypeface(primary);
        }

        Typeface tertiary = styles.getTertiaryTextTypeface();
        if (tertiary != null && bodyView != null) {
            bodyView.setTypeface(tertiary);
        }

        Typeface ctaTypeface = styles.getCallToActionTextTypeface();
        if (ctaTypeface != null && callToActionView != null) {
            callToActionView.setTypeface(ctaTypeface);
        }

        int primaryTypefaceColor = styles.getPrimaryTextTypefaceColor();
        if (primaryTypefaceColor > 0 && primaryView != null) {
            primaryView.setTextColor(primaryTypefaceColor);
        }

        int tertiaryTypefaceColor = styles.getTertiaryTextTypefaceColor();
        if (tertiaryTypefaceColor > 0 && bodyView != null) {
            bodyView.setTextColor(tertiaryTypefaceColor);
        }

        int ctaTypefaceColor = styles.getCallToActionTypefaceColor();
        if (ctaTypefaceColor > 0 && callToActionView != null) {
            callToActionView.setTextColor(ctaTypefaceColor);
        }

        float ctaTextSize = styles.getCallToActionTextSize();
        if (ctaTextSize > 0 && callToActionView != null) {
            callToActionView.setTextSize(ctaTextSize);
        }

        float primaryTextSize = styles.getPrimaryTextSize();
        if (primaryTextSize > 0 && primaryView != null) {
            primaryView.setTextSize(primaryTextSize);
        }

        float tertiaryTextSize = styles.getTertiaryTextSize();
        if (tertiaryTextSize > 0 && bodyView != null) {
            bodyView.setTextSize(tertiaryTextSize);
        }

        Drawable ctaBackground = styles.getCallToActionBackgroundColor();
        if (ctaBackground != null && callToActionView != null) {
            callToActionView.setBackground(ctaBackground);
        }

        Drawable primaryBackground = styles.getPrimaryTextBackgroundColor();
        if (primaryBackground != null && primaryView != null) {
            primaryView.setBackground(primaryBackground);
        }

        Drawable tertiaryBackground = styles.getTertiaryTextBackgroundColor();
        if (tertiaryBackground != null && bodyView != null) {
            bodyView.setBackground(tertiaryBackground);
        }

        invalidate();
        requestLayout();
    }

    public void setNativeAd(NativeAd nativeAd) {
        this.nativeAd = nativeAd;
        String headline = nativeAd.getHeadline();
        String body = nativeAd.getBody();
        String cta = nativeAd.getCallToAction();
        NativeAd.Image icon = nativeAd.getIcon();

        nativeAdView.setCallToActionView(callToActionView);
        nativeAdView.setHeadlineView(primaryView);
        nativeAdView.setMediaView(mediaView);
        mediaView.setMediaContent(nativeAd.getMediaContent());
        mediaView.setImageScaleType(ImageView.ScaleType.CENTER_CROP);

        primaryView.setText(headline);
        callToActionView.setText(cta);

        if (bodyView != null) {
            if (!TextUtils.isEmpty(body)) {
                bodyView.setVisibility(VISIBLE);
                bodyView.setText(body);
                nativeAdView.setBodyView(bodyView);
            } else {
                bodyView.setVisibility(GONE);
            }
        }

        if (iconView != null) {
            if (icon != null) {
                iconView.setVisibility(VISIBLE);
                iconView.setImageDrawable(icon.getDrawable());
            } else {
                iconView.setVisibility(GONE);
            }
        }

        nativeAdView.setNativeAd(nativeAd);
    }

    public NativeAd getNativeAd() {
        return nativeAd;
    }

    /**
     * To prevent memory leaks, make sure to destroy your ad when you don't need it anymore. This
     * method does not destroy the template view.
     * https://developers.google.com/admob/android/native-unified#destroy_ad
     */
    public void destroyNativeAd() {
        if (nativeAd != null) {
            nativeAd.destroy();
        }
    }

    public String getTemplateTypeName() {
        if (templateType == R.layout.native_ad_small) {
            return SMALL_TEMPLATE;
        }
        return "";
    }

    private void initView(Context context, AttributeSet attributeSet) {
        TypedArray attributes = context.getTheme().obtainStyledAttributes(attributeSet, R.styleable.NativeAdView, 0, 0);

        try {
            templateType = attributes.getResourceId(R.styleable.NativeAdView_native_ad_type, R.layout.native_ad_small);
        } finally {
            attributes.recycle();
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater != null) {
            inflater.inflate(templateType, this);
        }
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();

        nativeAdView = findViewById(R.id.native_ad_view);
        primaryView = findViewById(R.id.primary);
        bodyView = findViewById(R.id.body);

        callToActionView = findViewById(R.id.cta);
        iconView = findViewById(R.id.icon);
        mediaView = findViewById(R.id.media_view);
        background = findViewById(R.id.background);
    }
}
