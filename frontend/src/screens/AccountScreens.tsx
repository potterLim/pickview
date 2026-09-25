import { useEffect, useState } from "react";
import { Text, View } from "react-native";
import { request } from "../core/api";
import { useStore } from "../core/Store";
import type { INotice, ITicket } from "../core/types";
import { Button, Field } from "../ui/Controls";
import { ProductCard } from "../ui/ProductCard";
import { styles } from "../ui/theme";

export function SellerScreen() {
    const { selected, products, t, run, toggle, hasActivity } = useStore();
    const [bio, setBio] = useState("");
    useEffect(() => {
        if (!selected) { return; }
        request<{ bio: string }>(`/public/sellers/${selected.sellerId}`, "").then(value => setBio(value.bio)).catch(() => setBio(""));
    }, [selected?.sellerId]);
    if (!selected) { return null; }
    return <View style={styles.page}><View style={styles.panel}><Text style={styles.title}>{selected.sellerName}</Text><Text style={styles.subtitle}>{bio}</Text>
        <View style={styles.row}><Button label={hasActivity("FOLLOW", selected.sellerId) ? t("팔로우 중", "Following") : t("팔로우", "Follow")}
            onPress={() => run(() => toggle("FOLLOW", selected.sellerId))} />
            <Button secondary label={t("신규 영상 알림", "New video alerts") + (hasActivity("NOTIFY", selected.sellerId) ? " ✓" : "")} onPress={() => run(() => toggle("NOTIFY", selected.sellerId))} />
            <Button secondary label={hasActivity("BLOCK", selected.sellerId) ? t("차단 해제", "Unblock") : t("사용자 차단", "Block user")} onPress={() => run(() => toggle("BLOCK", selected.sellerId))} /></View></View>
        <Text style={styles.heading}>{t("이 크리에이터의 영상", "From this creator")}</Text>
        <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 24 }}>{products.filter(product => product.sellerId === selected.sellerId).map(product => <ProductCard key={product.id} product={product} width="100%" />)}</View>
    </View>;
}

export function InboxScreen() {
    const { token, user, t, run, notify } = useStore();
    const [tickets, setTickets] = useState<ITicket[]>([]);
    const [notices, setNotices] = useState<INotice[]>([]);
    const [message, setMessage] = useState("");
    const [reply, setReply] = useState("");
    async function reload() {
        const [newTickets, newNotices] = await Promise.all([request<ITicket[]>("/tickets", token), request<INotice[]>("/notices", token)]);
        setTickets(newTickets); setNotices(newNotices);
    }
    useEffect(() => { reload().catch(error => notify(error.message)); }, [token]);
    return <View style={styles.page}><Text style={styles.title}>{t("문의와 알림", "Inbox & notifications")}</Text>
        {notices.slice().reverse().map(notice => <View key={notice.id} style={[styles.panel, styles.between]}><Text style={styles.text}>{notice.message}</Text>
            {!notice.read ? <Button secondary label={t("읽음", "Mark read")} onPress={() => run(async () => { await request(`/notices/${notice.id}/read`, token, "POST"); await reload(); })} /> : null}</View>)}
        <View style={styles.panel}><Field label={t("고객지원 문의", "Contact support")} value={message} onChangeText={setMessage} multiline />
            <Button label={t("문의 보내기", "Send message")} onPress={() => run(async () => { await request("/tickets", token, "POST", { kind: "SUPPORT", targetId: "", message }); setMessage(""); await reload(); })} /></View>
        {tickets.slice().reverse().map(ticket => <View key={ticket.id} style={styles.panel}><View style={styles.between}><Text style={styles.label}>{ticket.kind}</Text><Text style={styles.badge}>{ticket.status}</Text></View>
            <Text style={styles.text}>{ticket.message}</Text>{ticket.reply ? <Text style={styles.subtitle}>{ticket.reply}</Text> : null}
            {ticket.recipientId === user?.id && ticket.kind === "INQUIRY" ? <><Field label={t("답변", "Reply")} value={reply} onChangeText={setReply} />
                <Button label={t("답변 보내기", "Send reply")} onPress={() => run(async () => { await request(`/tickets/${ticket.id}/reply`, token, "POST", { reply }); setReply(""); await reload(); })} /></> : null}</View>)}
    </View>;
}

export function SettingsScreen() {
    const { user, token, language, setLanguage, t, run, refresh, signOut, notify, navigate } = useStore();
    const [interests, setInterests] = useState(user?.interests ?? "");
    return <View style={styles.page}><Text style={styles.title}>{t("설정", "Settings")}</Text>
        <View style={styles.row}><Button secondary label={t("주문 내역", "Orders")} onPress={() => navigate("orders")} />
            <Button secondary label={t("찜한 영상", "Wishlist")} onPress={() => navigate("wishlist")} />
            <Button secondary label={t("팔로우", "Following")} onPress={() => navigate("following")} />
            {user && ["ADMIN", "CONTENT", "SUPPORT", "FINANCE"].includes(user.role) ? <Button secondary label={t("운영 관리", "Operations")} onPress={() => navigate("admin")} /> : null}</View>
        <View style={styles.panel}><Text style={styles.heading}>{user?.displayName}</Text><Text style={styles.muted}>{user?.email}</Text>
            <Text style={styles.label}>{t("언어", "Language")}</Text><View style={styles.row}><Button label="한국어" secondary={language !== "ko"} onPress={() => setLanguage("ko")} /><Button label="English" secondary={language !== "en"} onPress={() => setLanguage("en")} /></View>
            <Text style={styles.label}>{t("관심 분야", "Interests")}</Text><View style={styles.row}>{[["EDUCATION", "교육·강의", "Learning"], ["FINANCE", "투자·경제", "Finance"], ["COMEDY", "코미디", "Comedy"]].map(([value, ko, en]) =>
                <Button key={value} label={t(ko!, en!)} secondary={!interests.includes(value!)} onPress={() => setInterests(interests.includes(value!) ? interests.split(",").filter(item => item !== value).join(",") : [interests, value].filter(Boolean).join(","))} />)}</View>
            <Button label={t("설정 저장", "Save settings")} onPress={() => run(async () => { await request("/settings", token, "PUT", { language, interests }); await refresh(); notify(t("저장했어요.", "Saved.")); })} />
            <Button secondary label={t("로그아웃", "Sign out")} onPress={() => run(signOut)} /></View>
    </View>;
}
