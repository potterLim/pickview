import type { JSX } from "react";
import { decodeArray } from "../core/validation";
import { decodeActivity, decodePath } from "../core/contracts";
import { useEffect, useState } from "react";
import { Text, View, Pressable, Share, Platform, useWindowDimensions } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useStore } from "../core/Store";
import { API_URL, request, read } from "../core/api";
import { categoryLabel, getDemoContentOrNull, productTitle } from "../core/presentation";
import type { IActivity } from "../core/types";
import { Button, Field } from "../ui/Controls";
import { CreatorIdentity } from "../ui/CreatorIdentity";
import { ProductArtwork } from "../ui/ProductCard";
import { Playback } from "../ui/Playback";
import { colors, money, styles } from "../ui/theme";

export function DetailScreen(): JSX.Element | null {
    const {
        selected: product,
        token,
        library,
        t,
        language,
        run,
        toggle,
        hasActivity,
        navigate,
        notify,
        busy,
        addToCart,
    } = useStore();
    const { width } = useWindowDimensions();
    const wide = width >= 1120;
    const [uri, setUri] = useState("");
    const [full, setFull] = useState(false);
    const [reviews, setReviews] = useState<IActivity[]>([]);
    const [tab, setTab] = useState("intro");
    const [review, setReview] = useState("");
    const [rating, setRating] = useState(5);
    const [inquiry, setInquiry] = useState("");
    const [contact, setContact] = useState<"" | "INQUIRY" | "REPORT">("");
    const owned =
        product?.kind === "BUNDLE"
            ? product.videoIds.every((id) => library.some((item) => item.product.id === id && item.active))
            : library.some((item) => item.product.id === product?.id && item.active);
    const productId = product?.id;
    useEffect(() => {
        setUri("");
        setFull(false);
        setTab("intro");
        setContact("");
        setReview("");
        if (!productId) {
            return;
        }
        const abort = new AbortController();
        read(decodeArray(decodeActivity), `/public/products/${productId}/reviews`, "", "GET", undefined, abort.signal)
            .then((value) => {
                if (!abort.signal.aborted) {
                    setReviews(value);
                }
            })
            .catch(() => {
                if (!abort.signal.aborted) {
                    setReviews([]);
                }
            });
        return () => abort.abort();
    }, [productId]);
    if (!product) {
        return null;
    }
    const content = getDemoContentOrNull(product);
    const title = productTitle(product, language);
    const inCart = hasActivity("CART", product.id);
    const term = product.termDays
        ? t(`구매일부터 ${product.termDays}일 동안 시청`, `Watch for ${product.termDays} days from purchase`)
        : t("기간 제한 없이 시청", "Watch without a time limit");
    async function play() {
        if (!product) {
            return;
        }
        if (owned) {
            const result = await read(decodePath, `/media/ticket/${product.id}`, token, "POST");
            setUri(API_URL + result.path);
            setFull(true);
        } else {
            setUri(`${API_URL}/api/public/preview/${product.id}`);
            setFull(false);
        }
    }
    async function submitReview() {
        if (!product) {
            return;
        }
        await request("/activity", token, "PUT", {
            targetId: product.id,
            kind: "REVIEW",
            content: review,
            numberValue: rating,
        });
        setReviews(await read(decodeArray(decodeActivity), `/public/products/${product.id}/reviews`, ""));
        setReview("");
        notify(t("소중한 감상을 남겨 주셔서 고마워요.", "Thanks for sharing your thoughts."));
    }
    async function share() {
        if (!product) {
            return;
        }
        const url = `${process.env.EXPO_PUBLIC_WEB_URL ?? (Platform.OS === "web" ? window.location.origin : "http://localhost:8081")}/?video=${product.id}`;
        if (Platform.OS === "web" && navigator.clipboard) {
            await navigator.clipboard.writeText(url);
            notify(t("영상 링크를 복사했어요.", "Video link copied."));
        } else {
            await Share.share({ message: `${title}\n${url}` });
        }
    }
    return (
        <View style={[styles.page, { gap: 28 }]}>
            <View style={styles.row}>
                <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={t("탐색으로 돌아가기", "Back to explore")}
                    onPress={() => navigate("discover")}
                >
                    <Text style={styles.muted}>{t("탐색", "Discover")}</Text>
                </Pressable>
                <Text style={styles.muted}>/</Text>
                <Text style={styles.muted}>{categoryLabel(product.category, language)}</Text>
            </View>
            <View style={{ flexDirection: wide ? "row" : "column", gap: 24, alignItems: "flex-start" }}>
                <View style={{ width: wide ? "64%" : "100%", gap: 19 }}>
                    {uri ? (
                        <Playback
                            key={uri}
                            uri={uri}
                            product={product}
                            full={full}
                            onPurchase={() => run(() => addToCart(product.id, true))}
                        />
                    ) : (
                        <Pressable
                            accessibilityRole="button"
                            accessibilityLabel={
                                owned ? t("본편 시청", "Watch video") : t("미리보기 재생", "Play preview")
                            }
                            disabled={product.kind === "BUNDLE" || busy}
                            onPress={() => run(play)}
                            style={{ width: "100%" }}
                        >
                            <ProductArtwork product={product} showPlay={product.kind !== "BUNDLE"} />
                            <View
                                style={{
                                    position: "absolute",
                                    bottom: 14,
                                    left: 14,
                                    backgroundColor: "#20202ABF",
                                    borderRadius: 8,
                                    paddingHorizontal: 11,
                                    paddingVertical: 7,
                                }}
                            >
                                <Text style={{ fontSize: 12, color: "white", fontWeight: "600" }}>
                                    {product.kind === "BUNDLE"
                                        ? t("크리에이터 패키지", "Creator bundle")
                                        : owned
                                          ? t("본편 시청", "Watch video")
                                          : t("무료 미리보기", "Free preview")}
                                </Text>
                            </View>
                        </Pressable>
                    )}
                    <Text
                        style={[styles.title, { fontSize: width < 600 ? 25 : 29, lineHeight: width < 600 ? 36 : 42 }]}
                    >
                        {title}
                    </Text>
                    <View style={styles.between}>
                        <CreatorIdentity name={product.sellerName} onPress={() => navigate("seller", product)} />
                        <Pressable
                            accessibilityRole="button"
                            onPress={() => run(() => toggle("FOLLOW", product.sellerId))}
                        >
                            <Text style={{ color: colors.violet, fontWeight: "700", padding: 10 }}>
                                {hasActivity("FOLLOW", product.sellerId)
                                    ? t("팔로우 중", "Following")
                                    : t("팔로우하기", "Follow creator")}
                            </Text>
                        </Pressable>
                    </View>
                </View>
                <View style={{ width: wide ? "34%" : "100%", gap: 14 }}>
                    <View style={[styles.panel, { padding: wide ? 24 : 22, gap: 22 }]}>
                        <Text style={[styles.heading, { fontSize: 20, lineHeight: 29 }]}>
                            {owned
                                ? t("나의 라이브러리에 담긴 영상", "A part of your library")
                                : t("작은 호기심을, 나의 취향으로.", "Make room for a little curiosity.")}
                        </Text>
                        <View style={{ gap: 10 }}>
                            <Text style={{ fontSize: 36, fontWeight: "800", color: colors.ink, letterSpacing: -1 }}>
                                {product.priceWon ? money(product.priceWon) : t("무료", "Free")}
                            </Text>
                            <Text style={styles.subtitle}>{term}</Text>
                        </View>
                        {owned ? (
                            <Button
                                fullWidth
                                icon="play"
                                label={
                                    product.kind === "BUNDLE"
                                        ? t("라이브러리에서 시청", "Watch in your library")
                                        : t("이어서 시청하기", "Continue watching")
                                }
                                disabled={busy}
                                onPress={() => (product.kind === "BUNDLE" ? navigate("library") : run(play))}
                            />
                        ) : (
                            <View style={{ gap: 10 }}>
                                <Button
                                    fullWidth
                                    label={
                                        inCart ? t("장바구니로 이동", "View cart") : t("장바구니 담기", "Add to cart")
                                    }
                                    disabled={busy}
                                    onPress={() => (inCart ? navigate("cart") : run(() => addToCart(product.id)))}
                                />
                                <Button
                                    fullWidth
                                    secondary
                                    label={
                                        product.priceWon
                                            ? t("바로 구매하기", "Buy now")
                                            : t("무료로 소장하기", "Add to my library")
                                    }
                                    disabled={busy}
                                    onPress={() => run(() => addToCart(product.id, true))}
                                />
                            </View>
                        )}
                        <View style={{ flexDirection: "row", gap: 8, justifyContent: "center", alignItems: "center" }}>
                            <Ionicons name="lock-closed-outline" size={17} color={colors.ink} />
                            <Text style={styles.text}>
                                {t("한 번만 결제 · 구독 없음", "One purchase. No subscription.")}
                            </Text>
                        </View>
                        <View style={styles.divider} />
                        <Text style={styles.muted}>
                            {t("데모 결제이며 실제 청구되지 않습니다.", "Demo checkout. You will not be charged.")}
                        </Text>
                    </View>
                    <View style={styles.row}>
                        <Button
                            secondary
                            compact
                            icon={hasActivity("WISHLIST", product.id) ? "heart" : "heart-outline"}
                            label={hasActivity("WISHLIST", product.id) ? t("찜 해제", "Unsave") : t("찜하기", "Save")}
                            onPress={() => run(() => toggle("WISHLIST", product.id))}
                        />
                        <Button
                            secondary
                            compact
                            icon="share-outline"
                            label={t("공유", "Share")}
                            onPress={() => run(share)}
                        />
                    </View>
                </View>
            </View>
            <View
                style={{ flexDirection: "row", borderBottomWidth: 1, borderColor: colors.line, gap: 26, marginTop: 10 }}
            >
                {[
                    ["intro", t("영상 소개", "About this video")],
                    ["reviews", t("구매자 후기", "Buyer reviews") + ` (${reviews.length})`],
                ].map(([value, label]) => (
                    <Pressable
                        key={value}
                        accessibilityRole="tab"
                        accessibilityState={{ selected: tab === value }}
                        onPress={() => setTab(value!)}
                        style={{
                            paddingHorizontal: 8,
                            paddingBottom: 17,
                            borderBottomWidth: 3,
                            borderColor: tab === value ? colors.violet : "transparent",
                        }}
                    >
                        <Text
                            style={{
                                color: tab === value ? colors.violet : colors.muted,
                                fontWeight: "700",
                                fontSize: 15,
                            }}
                        >
                            {label}
                        </Text>
                    </Pressable>
                ))}
            </View>
            {tab === "intro" ? (
                <View style={{ gap: 20 }}>
                    <Text style={styles.heading}>
                        {content
                            ? language === "ko"
                                ? content.headline
                                : content.headlineEn
                            : t("이 영상에서는", "Inside this video")}
                    </Text>
                    <Text style={[styles.subtitle, { maxWidth: 830, lineHeight: 29 }]}>
                        {content && language === "en" ? content.descriptionEn : product.description}
                    </Text>
                    {content ? (
                        <View style={{ flexDirection: width < 900 ? "column" : "row", gap: 24, marginVertical: 12 }}>
                            {content.outcomes.map((item, index) => (
                                <View key={item.title} style={{ flex: 1, flexDirection: "row", gap: 16 }}>
                                    <View
                                        style={{
                                            width: 45,
                                            height: 45,
                                            borderRadius: 24,
                                            backgroundColor: colors.pale,
                                            alignItems: "center",
                                            justifyContent: "center",
                                        }}
                                    >
                                        <Text style={{ color: colors.violet, fontWeight: "800", fontSize: 16 }}>
                                            0{index + 1}
                                        </Text>
                                    </View>
                                    <View style={{ flex: 1, gap: 8 }}>
                                        <Text style={[styles.text, { fontWeight: "700" }]}>
                                            {language === "ko" ? item.title : item.titleEn}
                                        </Text>
                                        <Text style={styles.muted}>
                                            {language === "ko" ? item.body.replaceAll("\n", " ") : item.bodyEn}
                                        </Text>
                                    </View>
                                </View>
                            ))}
                        </View>
                    ) : null}
                    {product.kind === "BUNDLE" ? <BundleContents ids={product.videoIds} /> : null}
                    {content ? (
                        <Text style={styles.muted}>
                            {t("스튜디오 온 · 데모를 위해 제작한 사진·모션 그래픽 영상 · 한국어 자막과 배경음", "Studio On · Original photo and motion-graphics demo · Korean text and ambient audio")}
                        </Text>
                    ) : null}
                </View>
            ) : null}
            {tab === "reviews" || reviews.length === 0 ? (
                <View style={{ gap: 18 }}>
                    <Text style={styles.heading}>{t("구매자 후기", "Buyer reviews")}</Text>
                    {reviews.length === 0 ? (
                        <View
                            style={{
                                backgroundColor: colors.pale,
                                borderRadius: 14,
                                padding: 25,
                                flexDirection: "row",
                                gap: 16,
                                alignItems: "center",
                            }}
                        >
                            <Ionicons name="chatbubble-ellipses-outline" color={colors.muted} size={30} />
                            <View style={{ flex: 1, gap: 4 }}>
                                <Text style={styles.text}>
                                    {t("첫 번째 감상을 기다리고 있어요.", "Your first impression could be the first review.")}
                                </Text>
                                <Text style={styles.muted}>
                                    {t("이 영상의 좋았던 순간을 나눠 주세요.", "Tell us about a moment you enjoyed.")}
                                </Text>
                            </View>
                        </View>
                    ) : (
                        reviews.map((item) => (
                            <View
                                key={item.id}
                                style={{ paddingVertical: 18, borderBottomWidth: 1, borderColor: colors.line, gap: 10 }}
                            >
                                <Text style={[styles.text, { fontWeight: "700" }]}>
                                    {item.author}{" "}
                                    <Text style={{ color: colors.violet }}>
                                        {" "}
                                        {"★".repeat(Math.round(item.numberValue))}
                                    </Text>
                                </Text>
                                <Text style={styles.text}>{item.content}</Text>
                            </View>
                        ))
                    )}
                    {token && owned ? (
                        <View style={styles.panel}>
                            <Text style={styles.label}>{t("나의 감상", "Your thoughts")}</Text>
                            <View style={styles.row}>
                                {[1, 2, 3, 4, 5].map((value) => (
                                    <Pressable
                                        key={value}
                                        accessibilityRole="button"
                                        accessibilityLabel={`${value}${t("점", " stars")}`}
                                        onPress={() => setRating(value)}
                                    >
                                        <Ionicons
                                            name={value <= rating ? "star" : "star-outline"}
                                            size={28}
                                            color={colors.violet}
                                        />
                                    </Pressable>
                                ))}
                            </View>
                            <Field
                                label={t("후기", "Your review")}
                                placeholder={t("어떤 순간이 가장 좋았나요?", "What did you enjoy most?")}
                                value={review}
                                onChangeText={setReview}
                                multiline
                            />
                            <Button
                                label={t("후기 저장", "Save review")}
                                disabled={busy || !review.trim()}
                                onPress={() => run(submitReview)}
                            />
                        </View>
                    ) : null}
                </View>
            ) : null}
            <View style={[styles.row, { borderTopWidth: 1, borderColor: colors.line, paddingTop: 22 }]}>
                <Button
                    compact
                    secondary
                    label={t("판매자에게 문의", "Ask the creator")}
                    onPress={() => (token ? setContact(contact === "INQUIRY" ? "" : "INQUIRY") : navigate("login"))}
                />
                <Pressable
                    accessibilityRole="button"
                    onPress={() => (token ? setContact(contact === "REPORT" ? "" : "REPORT") : navigate("login"))}
                >
                    <Text style={styles.muted}>{t("콘텐츠 신고", "Report content")}</Text>
                </Pressable>
            </View>
            {contact ? (
                <View style={styles.panel}>
                    <Field
                        label={
                            contact === "REPORT"
                                ? t("신고 내용", "Report details")
                                : t("비공개 문의", "Private inquiry")
                        }
                        value={inquiry}
                        onChangeText={setInquiry}
                        multiline
                    />
                    <Button
                        label={t("보내기", "Send")}
                        disabled={busy || !inquiry.trim()}
                        onPress={() =>
                            run(async () => {
                                await request("/tickets", token, "POST", {
                                    targetId: product.id,
                                    kind: contact,
                                    message: inquiry,
                                });
                                setInquiry("");
                                setContact("");
                                notify(t("접수했어요. 문의함에서 답변을 확인하세요.", "Sent. Look out for a reply in your inbox."));
                            })
                        }
                    />
                </View>
            ) : null}
        </View>
    );
}

function BundleContents({ ids }: { ids: string[] }) {
    const { products, library, t, language, navigate } = useStore();
    return (
        <View style={{ gap: 16 }}>
            <Text style={styles.heading}>{t("함께 담긴 영상", "Included in this collection")}</Text>
            {ids.map((id) => {
                const product = products.find((item) => item.id === id) ?? library.find((item) => item.product.id === id)?.product;
                return product ? (
                    <Pressable
                        accessibilityRole="button"
                        key={id}
                        onPress={() => navigate("detail", product)}
                        style={{ flexDirection: "row", alignItems: "center", gap: 18 }}
                    >
                        <View style={{ width: 128 }}>
                            <ProductArtwork product={product} />
                        </View>
                        <Text style={[styles.text, { fontWeight: "600", flex: 1 }]}>
                            {productTitle(product, language)}
                        </Text>
                        <Ionicons name="chevron-forward" size={20} color={colors.muted} />
                    </Pressable>
                ) : null;
            })}
        </View>
    );
}
