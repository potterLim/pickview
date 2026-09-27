import type { JSX } from "react";
import { useEffect, type ReactNode, type ComponentProps } from "react";
import { Pressable, ScrollView, Text, View, useWindowDimensions, Platform } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useStore } from "../core/Store";
import type { Route } from "../core/types";
import { Button } from "./Controls";
import { colors, styles } from "./theme";

const navigation: { route: Route; ko: string; en: string; icon: ComponentProps<typeof Ionicons>["name"] }[] = [
    { route: "discover", ko: "탐색", en: "Discover", icon: "home-outline" },
    { route: "library", ko: "내 라이브러리", en: "My library", icon: "folder-open-outline" },
    { route: "wishlist", ko: "찜한 영상", en: "Wishlist", icon: "heart-outline" },
    { route: "following", ko: "팔로우", en: "Following", icon: "people-outline" },
    { route: "studio", ko: "판매자 스튜디오", en: "Creator studio", icon: "bar-chart-outline" },
    { route: "orders", ko: "주문 내역", en: "Orders", icon: "receipt-outline" },
    { route: "inbox", ko: "문의와 알림", en: "Inbox", icon: "chatbubbles-outline" },
    { route: "settings", ko: "설정", en: "Settings", icon: "settings-outline" },
];

export function AppShell({ children }: { children: ReactNode }): JSX.Element {
    const { width } = useWindowDimensions();
    const { t, route, navigate, language, setLanguage, user, activity, message, busy, products, selected, notify } = useStore();
    const mobile = width < 850;
    const cartCount = activity.filter((item) => item.kind === "CART").length;
    useEffect(() => {
        if (Platform.OS !== "web" || !products.length) {
            return;
        }
        const id = new URLSearchParams(window.location.search).get("video");
        const product = products.find((item) => item.id === id);
        if (product) {
            navigate("detail", product);
            window.history.replaceState({}, "", window.location.pathname);
        }
    }, [products, navigate]);
    return (
        <View style={{ flex: 1, backgroundColor: "white", flexDirection: "row" }}>
            {!mobile ? (
                <View
                    style={{
                        width: 215,
                        borderRightWidth: 1,
                        borderColor: colors.line,
                        paddingHorizontal: 16,
                        paddingTop: 28,
                        gap: 30,
                    }}
                >
                    <Pressable accessibilityRole="button" onPress={() => navigate("discover")}>
                        <Text
                            style={{
                                fontSize: 30,
                                fontWeight: "800",
                                color: colors.violet,
                                paddingLeft: 10,
                                letterSpacing: -1,
                            }}
                        >
                            PickView
                        </Text>
                    </Pressable>
                    <View style={{ gap: 8 }}>
                        {navigation.map((item) => (
                            <NavItem key={item.route} item={item} />
                        ))}
                    </View>
                    {user && ["ADMIN", "CONTENT", "SUPPORT", "FINANCE"].includes(user.role) ? (
                        <Button secondary label={t("운영 관리", "Operations")} onPress={() => navigate("admin")} />
                    ) : null}
                    <View style={{ flex: 1 }} />
                    <Text style={[styles.muted, { marginBottom: 25, paddingLeft: 10 }]}>
                        PickView · Demo{"\n"}
                        {t("좋아하는 순간을 골라보세요.", "Pick a moment that matters.")}
                    </Text>
                </View>
            ) : null}
            <View style={{ flex: 1 }}>
                <View
                    style={{
                        paddingHorizontal: mobile ? 18 : 38,
                        minHeight: 84,
                        flexDirection: "row",
                        justifyContent: "space-between",
                        alignItems: "center",
                        borderBottomWidth: 1,
                        borderColor: colors.line,
                        gap: 10,
                    }}
                >
                    <Text
                        style={{
                            fontSize: mobile ? 24 : 14,
                            fontWeight: mobile ? "800" : "500",
                            color: mobile ? colors.violet : colors.muted,
                        }}
                    >
                        {mobile ? "PickView" : t("당신의 다음 발견은 무엇인가요?", "What will you discover next?")}
                    </Text>
                    <View style={styles.row}>
                        <Pressable
                            accessibilityRole="button"
                            accessibilityLabel="Change language"
                            style={{ minWidth: 44, minHeight: 44, alignItems: "center", justifyContent: "center" }}
                            onPress={() => setLanguage(language === "ko" ? "en" : "ko")}
                        >
                            <Text style={{ color: colors.violet, fontWeight: "700" }}>
                                {language === "ko" ? "KO | EN" : "EN | KO"}
                            </Text>
                        </Pressable>
                        <Pressable
                            accessibilityRole="button"
                            accessibilityLabel={t("장바구니", "Cart")}
                            onPress={() => navigate("cart")}
                            style={{
                                padding: 7,
                                minWidth: 44,
                                minHeight: 44,
                                alignItems: "center",
                                justifyContent: "center",
                            }}
                        >
                            <Ionicons name="cart-outline" size={26} color={colors.ink} />
                            {cartCount ? (
                                <Text
                                    style={{
                                        position: "absolute",
                                        right: 0,
                                        top: -3,
                                        color: colors.violet,
                                        fontWeight: "800",
                                    }}
                                >
                                    {cartCount}
                                </Text>
                            ) : null}
                        </Pressable>
                        {width < 400 ? (
                            <Pressable
                                accessibilityRole="button"
                                accessibilityLabel={user ? t("내 계정", "Account") : t("로그인", "Sign in")}
                                onPress={() => navigate(user ? "settings" : "login")}
                                style={{
                                    padding: 8,
                                    minWidth: 44,
                                    minHeight: 44,
                                    alignItems: "center",
                                    justifyContent: "center",
                                }}
                            >
                                <Ionicons name="person-circle-outline" size={27} color={colors.violet} />
                            </Pressable>
                        ) : (
                            <Button
                                secondary={Boolean(user)}
                                label={user ? t("내 계정", "Account") : t("로그인", "Sign in")}
                                onPress={() => navigate(user ? "settings" : "login")}
                            />
                        )}
                    </View>
                </View>
                {message ? (
                    <View
                        accessibilityRole="alert"
                        style={{
                            position: "absolute",
                            zIndex: 10,
                            bottom: mobile ? 80 : 24,
                            right: mobile ? 18 : 38,
                            width: mobile ? width - 36 : 400,
                            backgroundColor: "#F7F5FF",
                            borderWidth: 1,
                            borderColor: "#DCD5F3",
                            borderRadius: 12,
                            padding: 14,
                            flexDirection: "row",
                            alignItems: "center",
                            gap: 12,
                        }}
                    >
                        <Text style={[styles.text, { flex: 1 }]}>{message}</Text>
                        <Pressable
                            accessibilityRole="button"
                            accessibilityLabel={t("알림 닫기", "Dismiss notification")}
                            onPress={() => notify("")}
                            style={{
                                padding: 8,
                                minWidth: 44,
                                minHeight: 44,
                                alignItems: "center",
                                justifyContent: "center",
                            }}
                        >
                            <Ionicons name="close" size={22} color={colors.violet} />
                        </Pressable>
                    </View>
                ) : null}
                <View style={{ height: 3, backgroundColor: busy ? colors.violet : "transparent" }} />
                <ScrollView
                    key={`${route}:${route === "detail" || route === "seller" ? selected?.id : ""}`}
                    contentContainerStyle={{
                        padding: mobile ? 18 : 38,
                        paddingBottom: 40,
                        maxWidth: 1600,
                        width: "100%",
                        alignSelf: "center",
                    }}
                >
                    {children}
                </ScrollView>
                {mobile ? (
                    <View
                        style={{
                            borderTopWidth: 1,
                            borderColor: colors.line,
                            backgroundColor: "white",
                            flexDirection: "row",
                            justifyContent: "space-around",
                            paddingTop: 8,
                            paddingBottom: 12,
                        }}
                    >
                        {navigation
                            .filter((item) =>
                                ["discover", "library", "studio", "inbox", "settings"].includes(item.route),
                            )
                            .map((item) => (
                                <NavItem key={item.route} item={item} compact />
                            ))}
                    </View>
                ) : null}
            </View>
        </View>
    );
}

function NavItem({ item, compact = false }: { item: (typeof navigation)[number]; compact?: boolean }) {
    const { route, t, navigate } = useStore();
    const active = route === item.route;
    return (
        <Pressable
            accessibilityRole="button"
            accessibilityLabel={t(item.ko, item.en)}
            accessibilityState={{ selected: active }}
            onPress={() => navigate(item.route)}
            style={{
                flexDirection: compact ? "column" : "row",
                gap: compact ? 4 : 17,
                padding: compact ? 5 : 14,
                minHeight: 44,
                flex: compact ? 1 : undefined,
                borderRadius: 12,
                backgroundColor: active ? colors.pale : "white",
                alignItems: "center",
            }}
        >
            <Ionicons name={item.icon} size={compact ? 21 : 23} color={active ? colors.violet : colors.ink} />
            <Text
                style={{
                    fontSize: compact ? 10 : 14,
                    color: active ? colors.violet : colors.ink,
                    fontWeight: active ? "700" : "500",
                }}
            >
                {compact && item.route === "library"
                    ? t("라이브러리", "Library")
                    : compact && item.route === "studio"
                      ? t("스튜디오", "Studio")
                      : compact && item.route === "inbox"
                        ? t("문의·알림", "Inbox")
                        : t(item.ko, item.en)}
            </Text>
        </Pressable>
    );
}
