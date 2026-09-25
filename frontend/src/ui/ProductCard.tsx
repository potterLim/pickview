import { Image, Pressable, Text, View, type ImageSourcePropType } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { IProduct } from "../core/types";
import { useStore } from "../core/Store";
import { colors, money, styles } from "./theme";

export const thumbnails: Record<string, ImageSourcePropType> = {
    studio: require("../../assets/studio.png"),
    pottery: require("../../assets/pottery.png"),
};

export function ProductCard({ product, width }: { product: IProduct; width: number | `${number}%` }) {
    const { navigate, t, language } = useStore();
    const title = language === "en" ? product.description.split("\n")[0] : product.title;
    return <Pressable accessibilityRole="button" accessibilityLabel={product.title} onPress={() => navigate("detail", product)}
        style={({ pressed }) => ({ width, gap: 10, opacity: pressed ? .85 : 1 })}>
        <View style={{ aspectRatio: 16 / 9, borderRadius: 12, overflow: "hidden", backgroundColor: colors.pale }}>
            {thumbnails[product.thumbnail] ? <Image source={thumbnails[product.thumbnail]} style={{ width: "100%", height: "100%" }} resizeMode="cover" />
                : <View style={{ flex: 1, backgroundColor: product.category === "FINANCE" ? "#E4EBDE" : "#2A2335", padding: 25, justifyContent: "center" }}>
                    <Text style={{ fontSize: 29, lineHeight: 38, fontWeight: "800", color: product.category === "FINANCE" ? "#314A2B" : "#F1CF9D" }}>
                        {product.category === "FINANCE" ? "Small steps.\nBig changes." : "A little laugh.\nA better day."}</Text>
                </View>}
            <View style={{ position: "absolute", alignSelf: "center", top: "38%", backgroundColor: "#20202A99", borderRadius: 24, padding: 11 }}>
                <Ionicons name={product.kind === "BUNDLE" ? "layers" : "play"} size={22} color="white" />
            </View>
            <Text style={{ position: "absolute", right: 8, bottom: 8, backgroundColor: "#20202ACC", color: "white", padding: 5, fontSize: 11, borderRadius: 5 }}>
                {product.kind === "BUNDLE" ? `${product.videoIds.length} ${t("편", "videos")}` : t("데모 00:30", "Demo 00:30")}
            </Text>
        </View>
        <Text style={styles.muted}>{product.sellerName}</Text>
        <Text numberOfLines={2} style={{ fontSize: 16, fontWeight: "600", color: colors.ink, lineHeight: 24 }}>{title}</Text>
        <View style={styles.between}>
            <Text style={styles.muted}>{product.reviewCount ? `★ ${product.rating.toFixed(1)} (${product.reviewCount})` : t("새로운 영상", "New arrival")}</Text>
            <Text style={styles.muted}>{product.termDays ? `${product.termDays}${t("일 시청", " days")}` : t("기간 제한 없음", "Unlimited")}</Text>
        </View>
        <Text style={styles.price}>{product.priceWon ? money(product.priceWon) : t("무료", "Free")}</Text>
    </Pressable>;
}
