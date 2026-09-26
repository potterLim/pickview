import { useState } from "react";
import { ImageBackground, Pressable, Text, TextInput, View, useWindowDimensions } from "react-native";
import { useStore } from "../core/Store";
import { colors, styles } from "../ui/theme";
import { ProductCard } from "../ui/ProductCard";
import { Button, Empty } from "../ui/Controls";

export function DiscoverScreen() {
    const { products, t, user, activity, route, navigate } = useStore();
    const { width } = useWindowDimensions();
    const [query, setQuery] = useState("");
    const [category, setCategory] = useState("");
    const [sort, setSort] = useState("popular");
    const [rating, setRating] = useState(false);
    const [sellerOnly, setSellerOnly] = useState(false);
    const selectedIds = activity
        .filter((item) => item.kind === (route === "wishlist" ? "WISHLIST" : "FOLLOW"))
        .map((item) => item.targetId);
    const blocked = activity.filter((item) => item.kind === "BLOCK").map((item) => item.targetId);
    const filtered = products
        .filter(
            (product) =>
                (!category || product.category === category) &&
                !blocked.includes(product.sellerId) &&
                (!rating || product.rating >= 4) &&
                (route !== "wishlist" || selectedIds.includes(product.id)) &&
                (route !== "following" || selectedIds.includes(product.sellerId)) &&
                (sellerOnly
                    ? product.sellerName
                    : `${product.title} ${product.description} ${product.sellerName} ${product.tags}`
                )
                    .toLowerCase()
                    .includes(query.toLowerCase()),
        )
        .sort((left, right) =>
            sort === "rating"
                ? right.rating - left.rating
                : sort === "new"
                  ? right.createdAt - left.createdAt
                  : right.sales - left.sales ||
                    Number(user?.interests.includes(right.category)) - Number(user?.interests.includes(left.category)),
        );
    const hasFilters = Boolean(query || category || rating || sellerOnly);
    function clearFilters() {
        setQuery("");
        setCategory("");
        setRating(false);
        setSellerOnly(false);
    }
    const title =
        route === "wishlist"
            ? t("찜한 영상", "Your wishlist")
            : route === "following"
              ? t("팔로우한 크리에이터", "Following creators")
              : t("구독 없이, 보고 싶은 영상만.", "Your next discovery. No subscription.");
    return (
        <View style={styles.page}>
            <View style={{ gap: 10 }}>
                <Text style={[styles.title, { fontSize: width < 600 ? 27 : 36 }]}>{title}</Text>
                <Text style={styles.subtitle}>
                    {t(
                        "좋은 크리에이터의 영상을 필요한 순간에, 지금 바로.",
                        "Thoughtful videos from creators you love. Yours when you need them.",
                    )}
                </Text>
            </View>
            <TextInput
                accessibilityLabel={t("영상 검색", "Search videos")}
                placeholder={t("원하는 크리에이터나 영상을 검색해보세요.", "Search videos or creators")}
                value={query}
                onChangeText={setQuery}
                style={styles.input}
            />
            <View style={styles.row}>
                {[
                    ["", "전체", "All"],
                    ["EDUCATION", "교육·강의", "Learning"],
                    ["FINANCE", "투자·경제", "Finance"],
                    ["COMEDY", "코미디", "Comedy"],
                ].map(([value, ko, en]) => (
                    <Pressable
                        key={value}
                        accessibilityRole="button"
                        accessibilityState={{ selected: category === value }}
                        onPress={() => setCategory(value ?? "")}
                        style={{
                            paddingVertical: 12,
                            paddingHorizontal: width < 600 ? 14 : 22,
                            minHeight: 44,
                            borderRadius: 24,
                            backgroundColor: category === value ? colors.violet : "#F4F4F8",
                        }}
                    >
                        <Text style={{ color: category === value ? "white" : colors.ink, fontWeight: "600" }}>
                            {t(ko ?? "", en ?? "")}
                        </Text>
                    </Pressable>
                ))}
            </View>
            {route === "discover" && !query && !category ? (
                <ImageBackground
                    source={require("../../assets/studio.png")}
                    imageStyle={{ borderRadius: 18 }}
                    style={{
                        minHeight: width < 600 ? 215 : 270,
                        padding: width < 600 ? 24 : 38,
                        justifyContent: "center",
                        overflow: "hidden",
                        borderRadius: 18,
                    }}
                >
                    <Text
                        style={{
                            fontSize: width < 600 ? 26 : 34,
                            fontWeight: "800",
                            lineHeight: width < 600 ? 38 : 47,
                            color: colors.ink,
                            maxWidth: width < 600 ? "100%" : "60%",
                        }}
                    >
                        {t("오늘의 발견,\n내일의 새로운 나.", "A discovery today.\nA new you tomorrow.")}
                    </Text>
                    <Text
                        style={{
                            marginTop: 14,
                            color: "#555367",
                            lineHeight: 24,
                            maxWidth: width < 600 ? "100%" : "55%",
                        }}
                    >
                        {t(
                            "좋은 영상을 만나는 것이\n더 나은 하루를 만듭니다.",
                            "A little curiosity.\nA brighter everyday.",
                        )}
                    </Text>
                </ImageBackground>
            ) : null}
            <View style={styles.between}>
                <Text style={styles.heading}>
                    {hasFilters
                        ? t("검색 결과", "Search results")
                        : route === "wishlist"
                          ? t("찜한 영상", "Saved videos")
                          : route === "following"
                            ? t("팔로우한 채널의 영상", "From channels you follow")
                            : t("지금 인기 있는 영상", "Worth a watch")}
                </Text>
                <View style={styles.row}>
                    <Button
                        secondary
                        label={
                            sort === "popular"
                                ? t("인기순", "Popular")
                                : sort === "new"
                                  ? t("최신순", "Newest")
                                  : t("평점순", "Top rated")
                        }
                        onPress={() => setSort(sort === "popular" ? "new" : sort === "new" ? "rating" : "popular")}
                    />
                    <Button secondary label={rating ? "★ 4+ ✓" : "★ 4+"} onPress={() => setRating(!rating)} />
                    <Button
                        secondary
                        label={t("판매자 검색", "Seller search") + (sellerOnly ? " ✓" : "")}
                        onPress={() => setSellerOnly(!sellerOnly)}
                    />
                </View>
            </View>
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 24 }}>
                {filtered.map((product) => (
                    <ProductCard
                        key={product.id}
                        product={product}
                        width={width < 600 ? "100%" : width < 1150 ? "47%" : "31.5%"}
                    />
                ))}
            </View>
            {!filtered.length ? (
                <Empty
                    title={
                        hasFilters
                            ? t("조건에 맞는 영상이 없어요", "No matching videos")
                            : t("아직 표시할 영상이 없어요.", "No videos here yet.")
                    }
                    description={
                        hasFilters
                            ? t(
                                  "다른 검색어를 입력하거나 검색 조건을 초기화해보세요.",
                                  "Try another search or clear your filters.",
                              )
                            : undefined
                    }
                    actionLabel={hasFilters ? t("검색 조건 초기화", "Clear filters") : undefined}
                    onAction={hasFilters ? clearFilters : undefined}
                />
            ) : null}
            <View style={[styles.between, { paddingTop: 24, borderTopWidth: 1, borderColor: colors.line }]}>
                <Text style={styles.muted}>
                    {t(
                        "PickView 데모 · 상품과 영상은 시연용 샘플입니다.",
                        "PickView demo · Products and clips are demonstration samples.",
                    )}
                </Text>
                <Button
                    secondary
                    label={t("크리에이터로 시작하기", "Become a creator")}
                    onPress={() => navigate("studio")}
                />
            </View>
        </View>
    );
}
