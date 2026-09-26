import { useEffect, useState, type ComponentProps } from "react";
import { Text, View, Pressable, useWindowDimensions } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { request } from "../core/api";
import { useStore } from "../core/Store";
import type { INotice, ITicket } from "../core/types";
import { Button, Field, Choice, Loading } from "../ui/Controls";
import { ProductCard } from "../ui/ProductCard";
import { CreatorIdentity } from "../ui/CreatorIdentity";
import { statusLabel } from "../core/presentation";
import { colors, styles } from "../ui/theme";

export function SellerScreen() {
    const { selected, products, t, run, toggle, hasActivity } = useStore();
    const { width } = useWindowDimensions();
    const [bio, setBio] = useState("");
    useEffect(() => {
        if (!selected) {
            return;
        }
        const controller = new AbortController();
        setBio("");
        request<{ bio: string }>(`/public/sellers/${selected.sellerId}`, "", "GET", undefined, controller.signal)
            .then((value) => setBio(value.bio.replace(/^(PERSONAL|BUSINESS):\s*/, "")))
            .catch(() => {
                if (!controller.signal.aborted) {
                    setBio("");
                }
            });
        return () => controller.abort();
    }, [selected?.sellerId]);
    if (!selected) {
        return null;
    }
    return (
        <View style={styles.page}>
            <View style={[styles.panel, { backgroundColor: "#FAF8FF", paddingVertical: 32 }]}>
                <CreatorIdentity
                    name={selected.sellerName}
                    size={64}
                    subtitle={t("크리에이터 채널", "Creator channel")}
                />
                <Text style={styles.subtitle}>{bio}</Text>
                <View style={styles.row}>
                    <Button
                        label={
                            hasActivity("FOLLOW", selected.sellerId)
                                ? t("팔로우 중", "Following")
                                : t("팔로우", "Follow")
                        }
                        onPress={() => run(() => toggle("FOLLOW", selected.sellerId))}
                    />
                    <Button
                        secondary
                        label={
                            t("신규 영상 알림", "New video alerts") +
                            (hasActivity("NOTIFY", selected.sellerId) ? " ✓" : "")
                        }
                        onPress={() => run(() => toggle("NOTIFY", selected.sellerId))}
                    />
                    <Button
                        secondary
                        label={
                            hasActivity("BLOCK", selected.sellerId)
                                ? t("차단 해제", "Unblock")
                                : t("사용자 차단", "Block user")
                        }
                        onPress={() => run(() => toggle("BLOCK", selected.sellerId))}
                    />
                </View>
            </View>
            <Text style={styles.heading}>{t("이 크리에이터의 영상", "From this creator")}</Text>
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 24 }}>
                {products
                    .filter((product) => product.sellerId === selected.sellerId)
                    .map((product) => (
                        <ProductCard
                            key={product.id}
                            product={product}
                            width={width < 700 ? "100%" : width < 1200 ? "47%" : "31%"}
                        />
                    ))}
            </View>
        </View>
    );
}

export function InboxScreen() {
    const { token, user, t, language, run, busy } = useStore();
    const [tickets, setTickets] = useState<ITicket[]>([]);
    const [notices, setNotices] = useState<INotice[]>([]);
    const [message, setMessage] = useState("");
    const [tab, setTab] = useState<"tickets" | "notices">("tickets");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const [sent, setSent] = useState(false);
    const unread = notices.filter((notice) => !notice.read).length;
    async function reload(signal?: AbortSignal) {
        const [newTickets, newNotices] = await Promise.all([
            request<ITicket[]>("/tickets", token, "GET", undefined, signal),
            request<INotice[]>("/notices", token, "GET", undefined, signal),
        ]);
        if (signal?.aborted) {
            return;
        }
        setTickets(newTickets);
        setNotices(newNotices);
        setError("");
        setLoading(false);
    }
    useEffect(() => {
        const controller = new AbortController();
        reload(controller.signal).catch(() => {
            if (!controller.signal.aborted) {
                setError(t("문의함을 불러오지 못했어요.", "Couldn't load your inbox."));
                setLoading(false);
            }
        });
        return () => controller.abort();
    }, [token]);
    async function sendMessage() {
        await request("/tickets", token, "POST", { kind: "SUPPORT", targetId: "", message: message.trim() });
        setMessage("");
        setSent(true);
        await reload();
    }
    return (
        <View style={[styles.page, { maxWidth: 920, width: "100%" }]}>
            <View style={{ gap: 8 }}>
                <Text style={styles.title}>{t("문의와 알림", "Inbox & notifications")}</Text>
                <Text style={styles.subtitle}>
                    {t(
                        "궁금한 점과 새로운 소식, 한곳에서 확인하세요.",
                        "Your conversations and updates, all in one place.",
                    )}
                </Text>
            </View>
            <View
                accessibilityRole="tablist"
                style={{ flexDirection: "row", gap: 24, borderBottomWidth: 1, borderColor: colors.line }}
            >
                {(["tickets", "notices"] as const).map((value) => (
                    <Pressable
                        key={value}
                        accessibilityRole="tab"
                        aria-selected={tab === value}
                        onPress={() => setTab(value)}
                        style={{
                            paddingVertical: 14,
                            borderBottomWidth: 2,
                            borderColor: tab === value ? colors.violet : "transparent",
                        }}
                    >
                        <Text
                            style={{
                                fontSize: 15,
                                color: tab === value ? colors.violet : colors.muted,
                                fontWeight: "600",
                            }}
                        >
                            {value === "tickets" ? t("문의 내역", "Conversations") : t("알림", "Notifications")}{" "}
                            {value === "tickets" ? tickets.length : unread}
                        </Text>
                    </Pressable>
                ))}
            </View>
            {tab === "tickets" ? (
                <View style={[styles.panel, { gap: 20 }]}>
                    <View style={{ gap: 5 }}>
                        <Text style={styles.sectionTitle}>{t("무엇을 도와드릴까요?", "How can we help?")}</Text>
                        <Text style={styles.muted}>
                            {t(
                                "답변은 이 문의함에 남겨드려요. 비밀번호나 결제 정보는 적지 마세요.",
                                "We'll reply here. Please leave out passwords and payment details.",
                            )}
                        </Text>
                    </View>
                    <Field
                        label={t("고객지원 문의", "Contact support")}
                        placeholder={t("궁금한 점을 자세히 알려주세요.", "Tell us what you need help with.")}
                        value={message}
                        onChangeText={(value) => {
                            setMessage(value);
                            setSent(false);
                        }}
                        maxLength={2000}
                        multiline
                    />
                    <View style={styles.actionRow}>
                        <Text accessibilityLiveRegion="polite" style={[styles.muted, { flex: 1 }]}>
                            {sent
                                ? t(
                                      "문의를 보냈어요. 아래에서 진행 상황을 확인하세요.",
                                      "Message sent. Track your conversation below.",
                                  )
                                : `${message.length.toLocaleString()} / 2,000`}
                        </Text>
                        <Button
                            loading={busy}
                            label={t("문의 보내기", "Send message")}
                            icon="arrow-up-outline"
                            disabled={!message.trim() || busy}
                            onPress={() => run(sendMessage)}
                        />
                    </View>
                </View>
            ) : null}
            {error ? (
                <View style={styles.panel}>
                    <Text style={styles.text}>{error}</Text>
                    <Button
                        secondary
                        label={t("다시 시도", "Try again")}
                        disabled={busy}
                        onPress={() => run(() => reload())}
                    />
                </View>
            ) : loading ? (
                <Loading />
            ) : tab === "tickets" ? (
                tickets.length ? (
                    tickets
                        .slice()
                        .sort((a, b) => b.createdAt - a.createdAt)
                        .map((ticket) => (
                            <TicketConversation
                                key={ticket.id}
                                ticket={ticket}
                                canReply={ticket.recipientId === user?.id && ticket.kind === "INQUIRY"}
                                onReply={reload}
                            />
                        ))
                ) : (
                    <InboxEmpty
                        icon="chatbubble-ellipses-outline"
                        title={t("아직 나눈 이야기가 없어요", "No conversations yet")}
                        description={t(
                            "문의가 생기면 편하게 남겨주세요. 이곳에 차곡차곡 모아둘게요.",
                            "Send us a question whenever you need. Your conversations will appear here.",
                        )}
                    />
                )
            ) : notices.length ? (
                <View style={styles.panel}>
                    {notices
                        .slice()
                        .sort((a, b) => b.createdAt - a.createdAt)
                        .map((notice, index) => (
                            <View
                                key={notice.id}
                                style={{
                                    gap: 12,
                                    paddingVertical: 12,
                                    borderTopWidth: index ? 1 : 0,
                                    borderColor: colors.line,
                                }}
                            >
                                <View style={[styles.row, { alignItems: "flex-start", flexWrap: "nowrap" }]}>
                                    <View
                                        style={{
                                            width: 7,
                                            height: 7,
                                            borderRadius: 4,
                                            backgroundColor: notice.read ? colors.line : colors.violet,
                                            marginTop: 8,
                                        }}
                                    />
                                    <View style={{ flex: 1, gap: 6 }}>
                                        <Text style={styles.text}>{notice.message}</Text>
                                        <Text style={styles.muted}>
                                            {new Date(notice.createdAt).toLocaleDateString(
                                                language === "ko" ? "ko-KR" : "en-US",
                                            )}
                                        </Text>
                                    </View>
                                    {!notice.read ? (
                                        <Button
                                            quiet
                                            compact
                                            label={t("읽음", "Mark read")}
                                            disabled={busy}
                                            onPress={() =>
                                                run(async () => {
                                                    await request(`/notices/${notice.id}/read`, token, "POST");
                                                    await reload();
                                                })
                                            }
                                        />
                                    ) : null}
                                </View>
                            </View>
                        ))}
                </View>
            ) : (
                <InboxEmpty
                    icon="notifications-outline"
                    title={t("새로운 소식이 오면 알려드릴게요", "You're all caught up")}
                    description={t(
                        "구매와 팔로우한 크리에이터의 소식을 여기서 확인할 수 있어요.",
                        "Purchase updates and news from creators you follow will appear here.",
                    )}
                />
            )}
        </View>
    );
}

function InboxEmpty({
    icon,
    title,
    description,
}: {
    icon: ComponentProps<typeof Ionicons>["name"];
    title: string;
    description: string;
}) {
    return (
        <View style={{ alignItems: "center", paddingVertical: 36, paddingHorizontal: 20, gap: 10 }}>
            <Ionicons name={icon} size={28} color={colors.muted} />
            <Text style={[styles.sectionTitle, { textAlign: "center", marginTop: 6 }]}>{title}</Text>
            <Text style={[styles.muted, { textAlign: "center", maxWidth: 430 }]}>{description}</Text>
        </View>
    );
}

function TicketConversation({
    ticket,
    canReply,
    onReply,
}: {
    ticket: ITicket;
    canReply: boolean;
    onReply: () => Promise<void>;
}) {
    const { token, t, language, run, busy } = useStore();
    const [reply, setReply] = useState("");
    return (
        <View style={styles.panel}>
            <View style={styles.between}>
                <View style={styles.row}>
                    <Text style={styles.sectionTitle}>{statusLabel(ticket.kind, language)}</Text>
                    <Text style={styles.badge}>{statusLabel(ticket.status, language)}</Text>
                </View>
                <Text style={styles.muted}>
                    {new Date(ticket.createdAt).toLocaleDateString(language === "ko" ? "ko-KR" : "en-US")}
                </Text>
            </View>
            <Text style={styles.text}>{ticket.message}</Text>
            {ticket.reply ? (
                <View style={{ backgroundColor: "#F8F7FC", borderRadius: 10, padding: 16, gap: 5 }}>
                    <Text style={styles.label}>{t("답변", "Reply")}</Text>
                    <Text style={styles.text}>{ticket.reply}</Text>
                </View>
            ) : null}
            {canReply ? (
                <>
                    <Field
                        label={t("답변 작성", "Write a reply")}
                        value={reply}
                        onChangeText={setReply}
                        maxLength={2000}
                        multiline
                    />
                    <View style={styles.actionRow}>
                        <Button
                            label={t("답변 보내기", "Send reply")}
                            disabled={busy || !reply.trim()}
                            onPress={() =>
                                run(async () => {
                                    await request(`/tickets/${ticket.id}/reply`, token, "POST", {
                                        reply: reply.trim(),
                                    });
                                    setReply("");
                                    await onReply();
                                })
                            }
                        />
                    </View>
                </>
            ) : null}
        </View>
    );
}

export function SettingsScreen() {
    const { user, token, language, setLanguage, t, run, busy, refresh, signOut, navigate } = useStore();
    const { width } = useWindowDimensions();
    const [interests, setInterests] = useState(user?.interests ?? "");
    const [saved, setSaved] = useState(false);
    useEffect(() => {
        setInterests(user?.interests ?? "");
        setSaved(false);
    }, [user?.id]);
    const dirty = interests !== (user?.interests ?? "") || language !== user?.language;
    function toggleInterest(value: string) {
        const selected = interests.split(",").filter(Boolean);
        setInterests(
            selected.includes(value)
                ? selected.filter((item) => item !== value).join(",")
                : [...selected, value].join(","),
        );
        setSaved(false);
    }
    async function saveSettings() {
        await request("/settings", token, "PUT", { language, interests });
        await refresh();
        setSaved(true);
    }
    return (
        <View style={[styles.page, { maxWidth: 920, width: "100%" }]}>
            <View style={{ gap: 8 }}>
                <Text style={styles.title}>{t("설정", "Settings")}</Text>
                <Text style={styles.subtitle}>
                    {t("PickView를 나에게 맞게 조정하세요.", "Make PickView feel like you.")}
                </Text>
            </View>
            <View style={[styles.panel, styles.between]}>
                <View
                    style={{
                        flex: width < 600 ? undefined : 1,
                        width: width < 600 ? "100%" : undefined,
                        minWidth: 150,
                    }}
                >
                    <CreatorIdentity name={user?.displayName ?? ""} subtitle={user?.email} size={48} />
                </View>
                <Button
                    quiet
                    label={t("로그아웃", "Sign out")}
                    icon="log-out-outline"
                    disabled={busy}
                    onPress={() => run(signOut)}
                />
            </View>
            <View style={[styles.panel, { gap: 24 }]}>
                <View style={{ gap: 8 }}>
                    <Text style={styles.sectionTitle}>{t("언어", "Language")}</Text>
                    <Text style={styles.muted}>
                        {t("화면에 표시할 언어를 선택하세요.", "Choose the language you see in PickView.")}
                    </Text>
                    <View
                        style={[styles.row, { marginTop: 8 }]}
                        accessibilityRole="radiogroup"
                        accessibilityLabel={t("언어", "Language")}
                    >
                        <Choice
                            label="한국어"
                            selected={language === "ko"}
                            onPress={() => {
                                setLanguage("ko");
                                setSaved(false);
                            }}
                        />
                        <Choice
                            label="English"
                            selected={language === "en"}
                            onPress={() => {
                                setLanguage("en");
                                setSaved(false);
                            }}
                        />
                    </View>
                </View>
                <View style={styles.divider} />
                <View style={{ gap: 8 }}>
                    <Text style={styles.sectionTitle}>{t("관심 분야", "Interests")}</Text>
                    <Text style={styles.muted}>
                        {t(
                            "좋아하는 주제를 골라주세요. 여러 개를 선택할 수 있어요.",
                            "Choose the topics you enjoy. Pick as many as you like.",
                        )}
                    </Text>
                    <View style={[styles.row, { marginTop: 8 }]}>
                        {[
                            ["EDUCATION", "교육·강의", "Learning"],
                            ["FINANCE", "투자·경제", "Finance"],
                            ["COMEDY", "코미디", "Comedy"],
                        ].map(([value, ko, en]) => (
                            <Choice
                                key={value}
                                label={t(ko!, en!)}
                                multiple
                                selected={interests.split(",").includes(value!)}
                                onPress={() => toggleInterest(value!)}
                            />
                        ))}
                    </View>
                </View>
                <View style={[styles.actionRow, { borderTopWidth: 1, borderColor: colors.line, paddingTop: 20 }]}>
                    <Text accessibilityLiveRegion="polite" style={[styles.muted, { flex: 1, minWidth: 100 }]}>
                        {dirty
                            ? t("저장하지 않은 변경사항이 있어요.", "You have unsaved changes.")
                            : saved
                              ? t("설정을 저장했어요.", "Your preferences are saved.")
                              : t("설정이 최신 상태예요.", "Your preferences are up to date.")}
                    </Text>
                    <Button
                        loading={busy}
                        label={t("설정 저장", "Save settings")}
                        disabled={!dirty || busy}
                        icon={saved && !dirty ? "checkmark-outline" : undefined}
                        onPress={() => run(saveSettings)}
                    />
                </View>
            </View>
            <View style={styles.panel}>
                <Text style={styles.sectionTitle}>{t("내 활동", "Your activity")}</Text>
                {(
                    [
                        { route: "orders", ko: "주문 내역", en: "Orders", icon: "receipt-outline" },
                        { route: "wishlist", ko: "찜한 영상", en: "Wishlist", icon: "heart-outline" },
                        { route: "following", ko: "팔로우", en: "Following", icon: "people-outline" },
                    ] as const
                ).map((item) => (
                    <Pressable
                        key={item.route}
                        accessibilityRole="button"
                        accessibilityLabel={t(item.ko, item.en)}
                        onPress={() => navigate(item.route)}
                        style={({ pressed }) => ({
                            flexDirection: "row",
                            alignItems: "center",
                            gap: 12,
                            minHeight: 48,
                            paddingHorizontal: 12,
                            borderRadius: 8,
                            backgroundColor: pressed ? "#F8F7FC" : "transparent",
                        })}
                    >
                        <Ionicons name={item.icon} size={20} color={colors.muted} />
                        <Text style={[styles.text, { flex: 1 }]}>{t(item.ko, item.en)}</Text>
                        <Ionicons name="chevron-forward" size={17} color={colors.muted} />
                    </Pressable>
                ))}
                {user && ["ADMIN", "CONTENT", "SUPPORT", "FINANCE"].includes(user.role) ? (
                    <Button
                        quiet
                        label={t("운영 관리", "Operations")}
                        icon="shield-checkmark-outline"
                        onPress={() => navigate("admin")}
                    />
                ) : null}
            </View>
        </View>
    );
}
