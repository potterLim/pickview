import { useState } from "react";
import { Image, Pressable, Text, View, type ImageSourcePropType } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { IProduct } from "../core/types";
import { useStore } from "../core/Store";
import { API_URL } from "../core/api";
import { categoryLabel, durationLabel, productTitle } from "../core/presentation";
import { colors, money, styles } from "./theme";

export const thumbnails: Record<string, ImageSourcePropType> = {
    studio: require("../../assets/studio.png"),
    pottery: require("../../assets/pottery.png"),
    finance: require("../../assets/finance.png"),
    comedy: require("../../assets/comedy.png"),
};

export function ProductArtwork({
    product,
    showPlay = false,
    showDuration = true,
}: {
    product: IProduct;
    showPlay?: boolean;
    showDuration?: boolean;
}) {
    const { t } = useStore();
    const source =
        thumbnails[product.thumbnail] ??
        (/^[a-f0-9-]{36}$/.test(product.thumbnail)
            ? { uri: `${API_URL}/api/public/thumbnails/${product.id}?v=${product.thumbnail}` }
            : thumbnails.studio);
    return (
        <View
            style={{
                width: "100%",
                aspectRatio: 16 / 9,
                borderRadius: 14,
                overflow: "hidden",
                backgroundColor: colors.pale,
            }}
        >
            <Image source={source} style={{ width: "100%", height: "100%" }} resizeMode="cover" />
            {showPlay ? (
                <View
                    style={{
                        position: "absolute",
                        top: 0,
                        bottom: 0,
                        left: 0,
                        right: 0,
                        alignItems: "center",
                        justifyContent: "center",
                    }}
                >
                    <View
                        style={{
                            width: 64,
                            height: 64,
                            borderRadius: 32,
                            backgroundColor: "#FFFFFFF2",
                            alignItems: "center",
                            justifyContent: "center",
                        }}
                    >
                        <Ionicons
                            name={product.kind === "BUNDLE" ? "layers" : "play"}
                            size={27}
                            color={colors.ink}
                            style={{ marginLeft: product.kind === "BUNDLE" ? 0 : 4 }}
                        />
                    </View>
                </View>
            ) : null}
            {showDuration ? (
                <Text
                    style={{
                        position: "absolute",
                        right: 10,
                        bottom: 10,
                        backgroundColor: "#20202ACC",
                        color: "white",
                        paddingHorizontal: 7,
                        paddingVertical: 4,
                        fontSize: 11,
                        fontWeight: "600",
                        borderRadius: 5,
                    }}
                >
                    {product.kind === "BUNDLE"
                        ? `${product.videoIds.length} ${t("편", "videos")}`
                        : durationLabel(product.durationSeconds)}
                </Text>
            ) : null}
        </View>
    );
}

export function ProductCard({ product, width }: { product: IProduct; width: number | `${number}%` }) {
    const { navigate, t, language, library } = useStore();
    const [highlighted, setHighlighted] = useState(false);
    const owned = library.some((item) => item.product.id === product.id && item.active);
    return (
        <Pressable
            accessibilityRole="button"
            accessibilityLabel={product.title}
            onPress={() => navigate("detail", product)}
            onHoverIn={() => setHighlighted(true)}
            onHoverOut={() => setHighlighted(false)}
            onFocus={() => setHighlighted(true)}
            onBlur={() => setHighlighted(false)}
            style={({ pressed }) => ({
                width,
                gap: 10,
                opacity: pressed ? 0.85 : 1,
                transform: [{ translateY: highlighted ? -3 : 0 }],
            })}
        >
            <ProductArtwork product={product} />
            <View style={styles.between}>
                <Text style={[styles.muted, { fontSize: 12 }]}>{categoryLabel(product.category, language)}</Text>
                {owned ? (
                    <Text style={{ fontSize: 11, color: colors.violet, fontWeight: "700" }}>
                        ✓ {t("내 라이브러리", "In your library")}
                    </Text>
                ) : null}
            </View>
            <Text
                numberOfLines={2}
                style={{
                    fontSize: 17,
                    fontWeight: "700",
                    color: highlighted ? colors.violet : colors.ink,
                    lineHeight: 26,
                    letterSpacing: -0.35,
                }}
            >
                {productTitle(product, language)}
            </Text>
            <Text style={styles.muted}>{product.sellerName}</Text>
            <View style={styles.between}>
                <Text style={[styles.price, { fontSize: 19 }]}>
                    {product.priceWon ? money(product.priceWon) : t("무료로 만나보기", "Watch for free")}
                </Text>
                <Text style={[styles.muted, { fontSize: 12 }]}>
                    {product.termDays
                        ? `${product.termDays}${t("일 시청", " days")}`
                        : t("기간 제한 없음", "Unlimited")}
                </Text>
            </View>
            {product.reviewCount > 0 ? (
                <Text style={styles.muted}>
                    ★ {product.rating.toFixed(1)} · {t("후기", "Reviews")} {product.reviewCount}
                </Text>
            ) : null}
        </Pressable>
    );
}
