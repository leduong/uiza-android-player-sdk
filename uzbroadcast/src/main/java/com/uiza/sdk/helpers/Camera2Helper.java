package com.uiza.sdk.helpers;

import android.media.MediaCodecInfo;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.util.Size;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.pedro.encoder.input.gl.render.filters.BaseFilterRender;
import com.pedro.encoder.input.video.CameraHelper;
import com.pedro.encoder.input.video.CameraOpenException;
import com.pedro.rtplibrary.rtmp.RtmpCamera2;
import com.uiza.sdk.enums.ProfileVideoEncoder;
import com.uiza.sdk.enums.RecordStatus;
import com.uiza.sdk.interfaces.UZCameraChangeListener;
import com.uiza.sdk.interfaces.UZRecordListener;
import com.uiza.sdk.interfaces.UZCameraOpenException;
import com.uiza.sdk.view.UZSize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class Camera2Helper implements ICameraHelper {

    private RtmpCamera2 rtmpCamera2;

    private UZCameraChangeListener cameraChangeListener;

    private UZRecordListener recordListener;


    public Camera2Helper(@NonNull RtmpCamera2 camera) {
        this.rtmpCamera2 = camera;
    }

    @Override
    public void setConnectReTries(int reTries) {
        rtmpCamera2.setReTries(reTries);
    }

    @Override
    public boolean reTry(long delay, @NonNull String reason) {
        return rtmpCamera2.reTry(delay, reason);
    }

    @Override
    public void setCameraChangeListener(@NonNull UZCameraChangeListener cameraChangeListener) {
        this.cameraChangeListener = cameraChangeListener;
    }

    @Override
    public void setRecordListener(UZRecordListener recordListener) {
        this.recordListener = recordListener;
    }

    @Override
    public void setFilter(@NonNull BaseFilterRender filterReader) {
        rtmpCamera2.getGlInterface().setFilter(filterReader);
    }

    @Override
    public void setFilter(int filterPosition, @NonNull BaseFilterRender filterReader) {
        rtmpCamera2.getGlInterface().setFilter(filterPosition, filterReader);
    }

    @Override
    public void enableAA(boolean aAEnabled) {
        rtmpCamera2.getGlInterface().enableAA(aAEnabled);
    }

    @Override
    public boolean isAAEnabled() {
        return rtmpCamera2.getGlInterface().isAAEnabled();
    }

    @Override
    public void setVideoBitrateOnFly(int bitrate) {
        rtmpCamera2.setVideoBitrateOnFly(bitrate);
    }

    @Override
    public int getBitrate() {
        return rtmpCamera2.getBitrate();
    }

    @Override
    public int getStreamWidth() {
        return rtmpCamera2.getStreamWidth();
    }

    @Override
    public int getStreamHeight() {
        return rtmpCamera2.getStreamHeight();
    }

    public boolean prepareAudio() {
        return rtmpCamera2.prepareAudio();
    }

    @Override
    public void enableAudio() {
        rtmpCamera2.enableAudio();
    }

    @Override
    public void disableAudio() {
        rtmpCamera2.disableAudio();
    }

    @Override
    public boolean isAudioMuted() {
        return rtmpCamera2.isAudioMuted();
    }

    @Override
    public boolean prepareAudio(int bitrate, int sampleRate, boolean isStereo) {
        return rtmpCamera2.prepareAudio(bitrate, sampleRate, isStereo, AcousticEchoCanceler.isAvailable(), NoiseSuppressor.isAvailable());
    }

    @Override
    public boolean isVideoEnabled() {
        return rtmpCamera2.isVideoEnabled();
    }

    @Override
    public boolean prepareVideo(@NonNull ProfileVideoEncoder profile) {
        return prepareVideo(profile, 24, 2, 90);
    }

    @Override
    public boolean prepareVideo(@NonNull ProfileVideoEncoder profile, int fps, int iFrameInterval, int rotation) {
        return rtmpCamera2.prepareVideo(
                profile.getWidth(),
                profile.getHeight(),
                fps,
                profile.getBitrate(),
                false,
                iFrameInterval,
                rotation,
                MediaCodecInfo.CodecProfileLevel.AVCProfileHigh,
                MediaCodecInfo.CodecProfileLevel.AVCLevel4);

    }

    @Override
    public void startStream(@NonNull String liveEndpoint) {
        rtmpCamera2.startStream(liveEndpoint);
    }

    @Override
    public void stopStream() {
        rtmpCamera2.stopStream();
    }

    @Override
    public boolean isStreaming() {
        return rtmpCamera2.isStreaming();
    }

    @Override
    public boolean isFrontCamera() {
        return rtmpCamera2.isFrontCamera();
    }

    @Override
    public void switchCamera() throws UZCameraOpenException {
        try {
            rtmpCamera2.switchCamera();
            if (cameraChangeListener != null)
                cameraChangeListener.onCameraChange(rtmpCamera2.isFrontCamera());

        } catch (CameraOpenException e) {
            throw new UZCameraOpenException(e.getMessage());
        }
    }

    @Override
    public List<UZSize> getSupportedResolutions() {
        List<Size> sizes;
        if (rtmpCamera2.isFrontCamera()) {
            sizes = rtmpCamera2.getResolutionsFront();
        } else {
            sizes = rtmpCamera2.getResolutionsBack();
        }
        List<UZSize> usizes = new ArrayList<>();
        for (Size s : sizes) {
            usizes.add(UZSize.fromSize(s));
        }
        return usizes;
    }

    @Override
    public void startPreview(@NonNull CameraHelper.Facing cameraFacing) {
        rtmpCamera2.startPreview(cameraFacing);
    }

    @Override
    public void startPreview(@NonNull CameraHelper.Facing cameraFacing, int w, int h) {
        // because portrait
        rtmpCamera2.startPreview(cameraFacing, h, w);
    }

    @Override
    public boolean isOnPreview() {
        return rtmpCamera2.isOnPreview();
    }

    @Override
    public void stopPreview() {
        rtmpCamera2.stopPreview();
    }

    @Override
    public boolean isRecording() {
        return rtmpCamera2.isRecording();
    }


    @Override
    public void startRecord(@NonNull String savePath) throws IOException {
        if (recordListener != null) {
            rtmpCamera2.startRecord(savePath, status -> recordListener.onStatusChange(RecordStatus.lookup(status)));
        } else {
            rtmpCamera2.startRecord(savePath);
        }

    }

    @Override
    public void stopRecord() {
        rtmpCamera2.stopRecord();
    }


    @Override
    public boolean isLanternSupported() {
        return rtmpCamera2.isLanternSupported();
    }

    @Override
    public void enableLantern() throws Exception {
        rtmpCamera2.enableLantern();
    }

    @Override
    public void disableLantern() {
        rtmpCamera2.disableLantern();
    }

    @Override
    public boolean isLanternEnabled() {
        return rtmpCamera2.isLanternEnabled();
    }

    @Override
    public float getMaxZoom() {
        return rtmpCamera2.getMaxZoom();
    }

    @Override
    public float getZoom() {
        return rtmpCamera2.getZoom();
    }

    @Override
    public void setZoom(float level) {
        rtmpCamera2.setZoom(level);
    }

    @Override
    public void setZoom(@NonNull MotionEvent event) {
        rtmpCamera2.setZoom(event);
    }
}
