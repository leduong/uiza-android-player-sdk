package com.uiza.sdk.view;

import android.net.Uri;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.ads.interactivemedia.v3.api.player.VideoAdPlayer;
import com.google.ads.interactivemedia.v3.api.player.VideoProgressUpdate;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.ext.ima.ImaAdsLoader;
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ads.AdsMediaSource;
import com.uiza.sdk.interfaces.UZAdPlayerCallback;
import com.uiza.sdk.utils.UZAppUtils;

/**
 * Manages the {@link ExoPlayer}, the IMA plugin and all video playback.
 */
//https://medium.com/@takusemba/understands-callbacks-of-exoplayer-c05ac3c322c2
public final class UZPlayerManager extends AbstractPlayerManager implements AdsMediaSource.MediaSourceFactory {

    private ImaAdsLoader adsLoader = null;
    private boolean isOnAdEnded;
    private UZAdPlayerCallback uzAdPlayerCallback;
    private UZVideoAdPlayerListener uzVideoAdPlayerListener = new UZVideoAdPlayerListener();

    UZPlayerManager(@NonNull UZVideoView uzVideo, String linkPlay, String urlIMAAd, String thumbnailsUrl) {
        super(uzVideo, linkPlay, thumbnailsUrl);
        if (!TextUtils.isEmpty(urlIMAAd) && UZAppUtils.isAdsDependencyAvailable())
            adsLoader = new ImaAdsLoader(context, Uri.parse(urlIMAAd));
        setRunnable();
    }

    private void onAdEnded() {
        if (!isOnAdEnded && uzVideoView != null) {
            isOnAdEnded = true;
            if (progressListener != null)
                progressListener.onAdEnded();
        }
    }

    @Override
    protected boolean isPlayingAd() {
        return uzVideoAdPlayerListener != null && uzVideoAdPlayerListener.isPlayingAd();
    }

    @Override
    public void setRunnable() {
        handler = new Handler();
        runnable = () -> {
            if (uzVideoView == null || uzVideoView.getUZPlayerView() == null)
                return;
            if (uzVideoAdPlayerListener.isEnded())
                onAdEnded();
            if (isPlayingAd())
                handleAdProgress();
            else
                handleVideoProgress();
            if (handler != null && runnable != null) {
                handler.postDelayed(runnable, 1000);
            }
        };
        new Handler().postDelayed(runnable, 0);
    }

    private void handleAdProgress() {
        hideProgress();
        isOnAdEnded = false;
        uzVideoView.setUseController(false);
        if (progressListener != null) {
            VideoProgressUpdate videoProgressUpdate = adsLoader.getAdProgress();
            duration = (int) videoProgressUpdate.getDuration();
            s = (int) (videoProgressUpdate.getCurrentTime()) + 1;//add 1 second
            if (duration != 0)
                percent = (int) (s * 100 / duration);
            progressListener.onAdProgress(s, (int) duration, percent);
        }
    }

    @Override
    void initSource() {
        isOnAdEnded = false;
        DefaultDrmSessionManager<FrameworkMediaCrypto> drmSessionManager = buildDrmSessionManager(drmScheme);
        if (drmScheme != null && drmSessionManager == null) return;

        player = buildPlayer(drmSessionManager);
        playerHelper = new UZPlayerHelper(player);

        uzVideoView.getUZPlayerView().setPlayer(player);

        MediaSource mediaSourceVideo = createMediaSourceVideo();
        // merge ads to media source subtitle
        // Compose the content media source into a new AdsMediaSource with both ads and content.
        MediaSource mediaSourceWithAds = createMediaSourceWithAds(mediaSourceVideo);
        // Prepare the player with the source.
        addPlayerListener();
        if (adsLoader != null) {
            adsLoader.setPlayer(player);
            adsLoader.addCallback(uzVideoAdPlayerListener);
        }
        player.prepare(mediaSourceWithAds);
        setPlayWhenReady(uzVideoView.isAutoStart());
        if (uzVideoView.isLiveStream())
            playerHelper.seekToDefaultPosition();
        else
            seekTo(contentPosition);
        notifyUpdateButtonVisibility();
        if (UZAppUtils.hasSupportPIP(context)) {
            //Use Media Session Connector from the EXT library to enable MediaSession Controls in PIP.
            MediaSessionCompat mediaSession = new MediaSessionCompat(context, context.getPackageName());
            MediaSessionConnector mediaSessionConnector = new MediaSessionConnector(mediaSession);
            mediaSessionConnector.setPlayer(player);
            mediaSession.setActive(true);
        }
    }

    private MediaSource createMediaSourceWithAds(MediaSource mediaSource) {
        if (adsLoader == null)
            return mediaSource;
        return new AdsMediaSource(mediaSource, this, adsLoader,
                uzVideoView.getUZPlayerView());
    }

    public void reset() {
        if (player != null) {
            contentPosition = player.getContentPosition();
            player.release();
            player = null;
        }
    }

    @Override
    public void release() {
        super.release();
        if (adsLoader != null) {
            adsLoader.setPlayer(null);
            adsLoader.release();
        }
    }

    @Override
    public MediaSource createMediaSource(Uri uri) {
        return buildMediaSource(uri);
    }

    @Override
    public int[] getSupportedTypes() {
        // IMA does not support Smooth Streaming ads.
        return new int[]{C.TYPE_DASH, C.TYPE_HLS, C.TYPE_OTHER};
    }

    void addAdPlayerCallback(UZAdPlayerCallback uzAdPlayerCallback) {
        this.uzAdPlayerCallback = uzAdPlayerCallback;
    }

    private class UZVideoAdPlayerListener implements VideoAdPlayer.VideoAdPlayerCallback {
        private boolean isPlayingAd;
        private boolean isEnded;

        @Override
        public void onPlay() {
            isPlayingAd = true;
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onPlay();
        }

        @Override
        public void onVolumeChanged(int i) {
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onVolumeChanged(i);
        }

        @Override
        public void onPause() {
            isPlayingAd = false;
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onPause();
        }

        @Override
        public void onLoaded() {
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onLoaded();
        }

        @Override
        public void onResume() {
            isPlayingAd = true;
            isEnded = false;
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onResume();
        }

        @Override
        public void onEnded() {
            isPlayingAd = false;
            isEnded = true;
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onEnded();
        }

        @Override
        public void onError() {
            isPlayingAd = false;
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onError();
        }

        @Override
        public void onBuffering() {
            if (uzAdPlayerCallback != null) uzAdPlayerCallback.onBuffering();
        }

        public boolean isPlayingAd() {
            return isPlayingAd;
        }

        public boolean isEnded() {
            return isEnded;
        }
    }
}
