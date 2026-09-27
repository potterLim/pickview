import { useEffect, useRef, useState } from "react";
import { ActivityIndicator, Text, View } from "react-native";
import { useVideoPlayer, VideoView } from "expo-video";
import { useStore } from "../core/Store";
import { savePlaybackProgress } from "../core/progress";
import type { IProduct } from "../core/types";
import { Button } from "./Controls";
import { colors, styles } from "./theme";

export function Playback({
    uri,
    product,
    full,
    onPurchase,
}: {
    uri: string;
    product: IProduct;
    full: boolean;
    onPurchase: () => void;
}) {
    const { token, activity, t, rememberProgress } = useStore();
    const [speed, setSpeed] = useState(1);
    const [muted, setMuted] = useState(true);
    const [ended, setEnded] = useState(false);
    const [status, setStatus] = useState("loading");
    const [saveFailed, setSaveFailed] = useState(false);
    const resumeAt = useRef(
        activity.find((item) => item.kind === "PROGRESS" && item.targetId === product.id)?.numberValue ?? 0,
    );
    const player = useVideoPlayer(uri, (instance) => {
        instance.timeUpdateEventInterval = 2;
        instance.muted = true;
    });

    useEffect(() => {
        let alive = true;
        let started = false;
        function savePosition(seconds: number) {
            if (!full || !token) {
                return;
            }
            const position = Math.max(0, Math.min(seconds, product.durationSeconds));
            rememberProgress(product.id, position);
            savePlaybackProgress(token, product.id, position)
                .then(() => {
                    if (alive) {
                        setSaveFailed(false);
                    }
                })
                .catch(() => {
                    if (alive) {
                        setSaveFailed(true);
                    }
                });
        }
        function startWhenReady() {
            if (started) {
                return;
            }
            started = true;
            if (full && resumeAt.current < product.durationSeconds - 1) {
                player.currentTime = resumeAt.current;
            }
            player.play();
        }
        // Web reports readiness through statusChange rather than sourceLoad.
        const statusChange = player.addListener("statusChange", (event) => {
            setStatus(event.status);
            if (event.status === "readyToPlay") {
                startWhenReady();
            }
        });
        const update = player.addListener("timeUpdate", (event) => savePosition(event.currentTime));
        const complete = player.addListener("playToEnd", () => {
            setEnded(true);
            savePosition(product.durationSeconds);
        });
        const playing = player.addListener("playingChange", (event) => {
            if (event.isPlaying) {
                setEnded(false);
            }
        });
        setStatus(player.status);
        if (player.status === "readyToPlay") {
            startWhenReady();
        }
        return () => {
            alive = false;
            statusChange.remove();
            update.remove();
            complete.remove();
            playing.remove();
        };
    }, [player, token, product.id, product.durationSeconds, full]);

    return (
        <View style={{ gap: 12 }}>
            <View style={{ borderRadius: 14, overflow: "hidden", backgroundColor: "#17151E" }}>
                <VideoView
                    player={player}
                    nativeControls
                    fullscreenOptions={{ enable: true }}
                    style={{ width: "100%", aspectRatio: 16 / 9 }}
                />
                {status === "loading" ? (
                    <View pointerEvents="none" style={{ position: "absolute", top: "45%", alignSelf: "center" }}>
                        <ActivityIndicator color="white" />
                    </View>
                ) : null}
            </View>
            {status === "error" ? (
                <View style={styles.panel}>
                    <Text style={styles.text}>
                        {t(
                            "영상을 불러오지 못했어요. 연결을 확인한 뒤 다시 시도해 주세요.",
                            "The video couldn't load. Check your connection and try again.",
                        )}
                    </Text>
                    <Button
                        secondary
                        label={t("다시 시도", "Try again")}
                        onPress={() => {
                            void player
                                .replaceAsync(uri)
                                .then(() => player.play())
                                .catch(() => setStatus("error"));
                        }}
                    />
                </View>
            ) : null}
            <View style={styles.between}>
                <View style={[styles.row, { gap: 6 }]}>
                    <Button compact secondary label="−10s" onPress={() => player.seekBy(-10)} />
                    <Button compact secondary label="+10s" onPress={() => player.seekBy(10)} />
                    <Button
                        compact
                        secondary
                        label={`${speed}×`}
                        onPress={() => {
                            const next = speed >= 2 ? 0.5 : speed + 0.25;
                            setSpeed(next);
                            player.playbackRate = next;
                        }}
                    />
                    <Button
                        compact
                        secondary
                        icon={muted ? "volume-mute-outline" : "volume-high-outline"}
                        label={muted ? t("소리 켜기", "Sound on") : t("소리 끄기", "Mute")}
                        onPress={() => {
                            player.muted = !muted;
                            setMuted(!muted);
                        }}
                    />
                </View>
                <Text style={styles.muted}>
                    {saveFailed
                        ? t("이어보기 저장을 재시도하고 있어요.", "Retrying progress sync.")
                        : full
                          ? t("이어보기 자동 저장", "Progress saved automatically")
                          : t("무료 미리보기", "Free preview")}
                </Text>
            </View>
            {ended ? (
                <View style={{ padding: 22, backgroundColor: colors.pale, borderRadius: 14, gap: 14 }}>
                    <Text style={styles.heading}>
                        {full
                            ? t("작은 발견, 어떠셨나요?", "A little discovery. How was it?")
                            : t("이야기는 계속됩니다.", "There's more to discover.")}
                    </Text>
                    <Text style={styles.subtitle}>
                        {full
                            ? t(
                                  "아래에서 감상을 남기거나 다시 시청해 보세요.",
                                  "Leave your thoughts below, or watch it again.",
                              )
                            : t(
                                  "마음에 들었다면, 전체 영상을 나의 라이브러리에 담아보세요.",
                                  "Enjoy the full video in your own library.",
                              )}
                    </Text>
                    <View style={styles.row}>
                        <Button
                            label={full ? t("다시 시청", "Watch again") : t("이 영상 소장하기", "Get the full video")}
                            onPress={
                                full
                                    ? () => {
                                          player.currentTime = 0;
                                          player.play();
                                          setEnded(false);
                                      }
                                    : onPurchase
                            }
                        />
                    </View>
                </View>
            ) : null}
        </View>
    );
}
