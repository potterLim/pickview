import { useEffect, useState, useRef } from "react";
import { Text, View, useWindowDimensions } from "react-native";
import { request } from "../core/api";
import { useStore } from "../core/Store";
import type { IOrder } from "../core/types";
import { Button, Empty, Field } from "../ui/Controls";
import { ProductCard } from "../ui/ProductCard";
import { money, styles } from "../ui/theme";

export function CartScreen() {
    const { activity, products, token, t, run, busy, refresh, navigate, toggle, notify } = useStore();
    const [channel, setChannel] = useState("CARD");
    const [outcome, setOutcome] = useState("SUCCESS");
    const requestKey = useRef("");
    const cart = products.filter(product => activity.some(item => item.kind === "CART" && item.targetId === product.id));
    const total = cart.reduce((amount, product) => amount + product.priceWon, 0);
    async function checkout() {
        if (!requestKey.current) { requestKey.current = `web-${Date.now()}-${Math.random().toString(36).slice(2)}`; }
        const order = await request<IOrder>("/checkout", token, "POST", { productIds: cart.map(product => product.id), requestKey: requestKey.current, channel, outcome });
        requestKey.current = "";
        if (order.status === "SUCCESS") {
            await Promise.all(cart.map(product => request(`/activity/CART/${product.id}`, token, "DELETE")));
            await refresh(); notify(t("구매 완료! 라이브러리에서 시청하세요.", "Purchase complete. Enjoy your library.")); navigate("library");
        } else { notify(t(`모의 결제 결과: ${order.status}. 결제되지 않았습니다.`, `Mock result: ${order.status}. No charge.`)); }
    }
    return <View style={styles.page}><Text style={styles.title}>{t("장바구니", "Your cart")}</Text>
        {!cart.length ? <Empty title={t("장바구니가 비어 있어요.", "Your cart is empty.")} /> : <>
            {cart.map(product => <View style={[styles.panel, styles.between]} key={product.id}><View style={{ flex: 1 }}><Text style={styles.heading}>{product.title}</Text>
                <Text style={styles.muted}>{product.sellerName} · {product.termDays || "∞"}{t("일", " days")}</Text></View>
                <Text style={styles.price}>{money(product.priceWon)}</Text><Button secondary label={t("삭제", "Remove")} onPress={() => run(() => toggle("CART", product.id))} /></View>)}
            <View style={styles.panel}><Text style={styles.heading}>{t("모의 결제", "Mock checkout")}</Text>
                <Text style={styles.subtitle}>{t("실제 결제되지 않습니다. 카드정보를 입력하지 마세요.", "No real payment. Do not enter card details.")}</Text>
                <View style={styles.row}>{[["CARD", "카드", "Card"], ["EASY", "간편결제", "Easy pay"]].map(([value, ko, en]) =>
                    <Button key={value} secondary={channel !== value} label={t(ko!, en!)} onPress={() => { setChannel(value!); requestKey.current = ""; }} />)}</View>
                <View style={styles.row}>{["SUCCESS", "FAILED", "CANCELED"].map(value => <Button key={value} secondary={outcome !== value} label={value} onPress={() => { setOutcome(value); requestKey.current = ""; }} />)}</View>
                <View style={styles.between}><Text style={styles.heading}>{t("합계", "Total")}</Text><Text style={styles.price}>{money(total)}</Text></View>
                <Button label={t("모의 결제하기", "Complete mock purchase")} disabled={busy} onPress={() => run(checkout)} />
            </View></>}
    </View>;
}

export function LibraryScreen() {
    const { library, t, activity } = useStore();
    const { width } = useWindowDimensions();
    const items = library.filter((item, index, list) => list.findIndex(other => other.product.id === item.product.id && other.active === item.active) === index);
    return <View style={styles.page}><Text style={styles.title}>{t("내 라이브러리", "Your library")}</Text><Text style={styles.subtitle}>{t("나의 취향으로 채운 작은 세계", "A collection made for your curiosity.")}</Text>
        {!items.length ? <Empty title={t("첫 번째 영상을 골라보세요.", "Find your first video.")} /> : null}
        <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 24 }}>{items.map(item => <View key={item.id} style={{ width: width < 700 ? "100%" : "47%", gap: 12 }}>
            <ProductCard product={item.product} width="100%" />
            <Text style={styles.badge}>{!item.active ? t("이용 기간 만료", "Expired") : item.expiresAt ? `${t("이용 기한", "Available until")}: ${new Date(item.expiresAt).toLocaleDateString()}` : t("기간 제한 없음", "Unlimited")}</Text>
            <Text style={styles.muted}>{t("이어보기", "Resume")}: {Math.round(activity.find(entry => entry.kind === "PROGRESS" && entry.targetId === item.product.id)?.numberValue ?? 0)}s</Text>
        </View>)}</View></View>;
}

export function OrdersScreen() {
    const { token, t, run, notify } = useStore();
    const [orders, setOrders] = useState<IOrder[]>([]);
    const [reason, setReason] = useState("");
    useEffect(() => { request<IOrder[]>("/orders", token).then(setOrders).catch(error => notify(error.message)); }, [token]);
    async function refund(id: string) {
        await request("/tickets", token, "POST", { kind: "REFUND", targetId: id, message: reason || t("미재생 취소 요청", "Unplayed cancellation request") });
        notify(t("환불 요청을 접수했어요. 문의함에서 상태를 확인하세요.", "Refund requested. Track it in your inbox."));
    }
    return <View style={styles.page}><Text style={styles.title}>{t("주문 내역", "Your orders")}</Text>
        <Field label={t("환불 요청 사유", "Refund reason")} value={reason} onChangeText={setReason} />
        {!orders.length ? <Empty title={t("아직 주문이 없어요.", "No orders yet.")} /> : null}
        {orders.slice().reverse().map(order => <View key={order.id} style={styles.panel}><View style={styles.between}>
            <Text style={styles.muted}>{new Date(order.createdAt).toLocaleString()} · {order.id.slice(0, 8)}</Text><Text style={styles.badge}>{order.status}</Text></View>
            {order.lines.map(line => <View key={line.id} style={styles.between}><Text style={[styles.text, { flex: 1 }]}>{line.title}</Text><Text style={styles.price}>{money(line.priceWon)}</Text>
                {line.refunded ? <Text style={styles.muted}>{t("환불 완료", "Refunded")}</Text> : <Button secondary label={t("환불 요청", "Request refund")} onPress={() => run(() => refund(line.id))} />}</View>)}
            <Text style={styles.heading}>{money(order.totalWon)}</Text></View>)}
    </View>;
}
