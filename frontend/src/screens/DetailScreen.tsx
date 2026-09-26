import { useEffect, useState } from "react";
import { Text, View, Share } from "react-native";
import { useVideoPlayer, VideoView } from "expo-video";
import { useStore } from "../core/Store";
import { API_URL, request } from "../core/api";
import type { IActivity, IProduct } from "../core/types";
import { Button, Field } from "../ui/Controls";
import { money, styles } from "../ui/theme";

function VideoPlayer({ uri, product, full }: { uri: string; product: IProduct; full: boolean }) {
    const { token, activity, t, notify } = useStore();
    const [speed, setSpeed] = useState(1);
    const player = useVideoPlayer(uri, instance => {
        instance.timeUpdateEventInterval = 5;
    });
    useEffect(() => {
        const progress = activity.find(item => item.kind === "PROGRESS" && item.targetId === product.id)?.numberValue ?? 0;
        const ready = player.addListener("sourceLoad", () => { if (full && progress < product.durationSeconds - 1) { player.currentTime = progress; } });
        const error = player.addListener("statusChange", event => { if (event.status === "error") { notify(t("영상을 불러오지 못했습니다.", "Video could not be loaded.")); } });
        const update = player.addListener("timeUpdate", event => {
            if (!full || !token) { return; }
            request("/activity", token, "PUT", { targetId: product.id, kind: "PROGRESS", content: "", numberValue: Math.min(event.currentTime, product.durationSeconds) })
                .catch(() => notify(t("이어보기 저장 실패", "Could not save progress")));
        });
        return () => { ready.remove(); error.remove(); update.remove(); };
    }, [player, token, product.id, product.durationSeconds, full]);
    return <View style={{ gap: 12 }}><VideoView player={player} nativeControls fullscreenOptions={{ enable: true }} style={{ width: "100%", aspectRatio: 16 / 9, backgroundColor: "#15141B", borderRadius: 14 }} />
        <View style={styles.row}>
            <Button secondary label="−10s" onPress={() => player.seekBy(-10)} />
            <Button secondary label="+10s" onPress={() => player.seekBy(10)} />
            <Button secondary label={`${speed}×`} onPress={() => { const next = speed >= 2 ? .5 : speed + .25; setSpeed(next); player.playbackRate = next; }} />
            <Text style={styles.muted}>{full ? t("구매한 영상 · 이어보기 자동 저장", "Purchased · Progress saved") : t("무료 미리보기", "Free preview")}</Text>
        </View></View>;
}

export function DetailScreen() {
    const { selected: product, token, library, t, language, run, toggle, hasActivity, navigate, notify, busy } = useStore();
    const [uri, setUri] = useState("");
    const [full, setFull] = useState(false);
    const [reviews, setReviews] = useState<IActivity[]>([]);
    const [review, setReview] = useState("");
    const [rating, setRating] = useState("5");
    const [inquiry, setInquiry] = useState("");
    const owned = library.some(item => item.product.id === product?.id && item.active);
    useEffect(() => {
        setUri(""); setFull(false);
        if (!product) { return; }
        const abort = new AbortController();
        request<IActivity[]>(`/public/products/${product.id}/reviews`, "", "GET", undefined, abort.signal).then(setReviews).catch(() => setReviews([]));
        return () => abort.abort();
    }, [product?.id]);
    if (!product) { return null; }
    async function play() {
        if (!product) { return; }
        if (owned) { const result = await request<{ path: string }>(`/media/ticket/${product.id}`, token, "POST"); setUri(API_URL + result.path); setFull(true); }
        else { setUri(`${API_URL}/api/public/preview/${product.id}`); setFull(false); }
    }
    async function submitReview() {
        if (!product) { return; }
        await request("/activity", token, "PUT", { targetId: product.id, kind: "REVIEW", content: review, numberValue: Number(rating) });
        setReviews(await request<IActivity[]>(`/public/products/${product.id}/reviews`, "")); setReview(""); notify(t("후기를 저장했어요.", "Review saved."));
    }
    async function sendTicket(kind: string) {
        if (!product) { return; }
        await request("/tickets", token, "POST", { targetId: product.id, kind, message: inquiry }); setInquiry(""); notify(t("접수했어요.", "Request sent."));
    }
    return <View style={styles.page}>
        <Button secondary label={t("탐색으로 돌아가기", "Back to explore")} onPress={() => navigate("discover")} />
        <Text style={styles.title}>{language === "en" && /^video-[1-6]$/.test(product.id) ? product.description.split("\n")[0] : product.title}</Text>
        <View style={styles.between}><Button secondary label={product.sellerName} onPress={() => navigate("seller", product)} />
            <Text style={styles.badge}>{product.kind === "BUNDLE" ? t("영상 패키지", "Video bundle") : t("단품 영상", "Single video")}</Text></View>
        {uri ? <VideoPlayer key={uri} uri={uri} product={product} full={full} /> : null}
        <View style={styles.panel}>
            <View style={styles.between}><Text style={styles.price}>{money(product.priceWon)}</Text>
                <Text style={styles.text}>{product.termDays ? t(`구매일부터 ${product.termDays}일 시청`, `${product.termDays} days from purchase`) : t("기간 제한 없이 시청", "Unlimited access")}</Text></View>
            <Text style={styles.text}>{product.description}</Text>
            <View style={styles.row}>
                {product.kind === "VIDEO" ? <Button label={owned ? t("본편 시청", "Watch video") : t("미리보기 재생", "Play preview")} disabled={busy} onPress={() => run(play)} /> : null}
                {!owned ? <Button label={hasActivity("CART", product.id) ? t("장바구니에서 빼기", "Remove from cart") : t("장바구니 담기", "Add to cart")} onPress={() => run(() => toggle("CART", product.id))} /> : null}
                <Button secondary label={hasActivity("WISHLIST", product.id) ? t("찜 해제", "Unsave") : t("찜하기", "Save")} onPress={() => run(() => toggle("WISHLIST", product.id))} />
                <Button secondary label={t("공유", "Share")} onPress={() => run(async () => { await Share.share({ message: `${product.title}\n${process.env.EXPO_PUBLIC_WEB_URL ?? "http://localhost:8081"}/?video=${product.id}` }); })} />
            </View>
        </View>
        {product.kind === "BUNDLE" ? <BundleContents ids={product.videoIds} /> : null}
        <Text style={styles.heading}>{t("구매자 후기", "Buyer reviews")} ({reviews.length})</Text>
        {!reviews.length ? <Text style={styles.muted}>{t("아직 후기가 없어요. 첫 후기를 남겨 주세요.", "No reviews yet. Share your first impression.")}</Text> : null}
        {reviews.map(item => <View style={styles.panel} key={item.id}><Text style={styles.label}>{item.author} · ★ {item.numberValue}</Text><Text style={styles.text}>{item.content}</Text></View>)}
        {token ? <><View style={styles.panel}><Field label={t("후기", "Your review")} value={review} onChangeText={setReview} multiline />
            <Field label={t("평점 (1~5)", "Rating (1–5)")} value={rating} onChangeText={setRating} keyboardType="numeric" />
            <Button label={t("후기 저장", "Save review")} disabled={busy} onPress={() => run(submitReview)} /></View>
            <View style={styles.panel}><Field label={t("비공개 문의 / 신고 내용", "Private inquiry / report")} value={inquiry} onChangeText={setInquiry} multiline />
                <View style={styles.row}><Button label={t("판매자에게 문의", "Ask seller")} onPress={() => run(() => sendTicket("INQUIRY"))} />
                    <Button secondary label={t("콘텐츠 신고", "Report content")} onPress={() => run(() => sendTicket("REPORT"))} /></View></View></> : null}
    </View>;
}

function BundleContents({ ids }: { ids: string[] }) {
    const { products, t, navigate } = useStore();
    return <View style={styles.panel}><Text style={styles.heading}>{t("패키지 구성", "Included videos")}</Text>
        {ids.map(id => { const product = products.find(item => item.id === id); return product ? <Button secondary key={id} label={product.title} onPress={() => navigate("detail", product)} /> : null; })}</View>;
}
