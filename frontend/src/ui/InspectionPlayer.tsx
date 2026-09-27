import type { JSX } from "react";
import { decodePath } from "../core/contracts";
import { useState } from "react";
import { View } from "react-native";
import { useVideoPlayer, VideoView } from "expo-video";
import { API_URL, read } from "../core/api";
import { useStore } from "../core/Store";
import { Button } from "./Controls";

export function InspectionPlayer({ productId }: { productId: string }): JSX.Element {
    const { token, t, run } = useStore();
    const [uri, setUri] = useState("");
    return (
        <View style={{ width: "100%", gap: 12 }}>
            <Button
                secondary
                label={t("업로드 영상 확인", "Inspect uploaded video")}
                onPress={() =>
                    run(async () => {
                        const result = await read(decodePath, `/media/review/${productId}`, token, "POST");
                        setUri(API_URL + result.path);
                    })
                }
            />
            {uri ? <InspectionVideo key={uri} uri={uri} /> : null}
        </View>
    );
}

function InspectionVideo({ uri }: { uri: string }) {
    const player = useVideoPlayer(uri);
    return (
        <VideoView
            player={player}
            nativeControls
            fullscreenOptions={{ enable: true }}
            style={{ width: "100%", aspectRatio: 16 / 9, backgroundColor: "#15141B", borderRadius: 12 }}
        />
    );
}
