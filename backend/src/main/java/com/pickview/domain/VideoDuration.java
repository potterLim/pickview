package com.pickview.domain;

import com.pickview.api.ApiFailure;

public final class VideoDuration {

    private static final double MAX_VIDEO_SECONDS = 600;
    private static final double MAX_PREVIEW_SECONDS = 60;
    private static final double MAX_PREVIEW_FRACTION = 0.2;
    private final double mSeconds;

    public VideoDuration(double seconds) {
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > MAX_VIDEO_SECONDS) {
            throw new ApiFailure(400, "Video duration must be positive and at most 600 seconds");
        }
        mSeconds = seconds;
    }

    public double getSeconds() {
        return mSeconds;
    }

    public void requireValidPreview(VideoDuration preview) {
        if (preview.mSeconds > Math.min(MAX_PREVIEW_SECONDS, mSeconds * MAX_PREVIEW_FRACTION)) {
            throw new ApiFailure(400, "미리보기는 전체의 20% 이내, 최대 60초입니다. / Preview limit exceeded.");
        }
    }
}
