import type { JSX } from "react";
import { decodeArray } from "../core/validation";
import { decodeOrder, decodeTicket } from "../core/contracts";
import { useEffect, useState, useRef, useCallback } from "react";
import { Text, View, useWindowDimensions } from "react-native";
import { request, read } from "../core/api";
import { useStore } from "../core/Store";
import type { IOrder, ITicket } from "../core/types";
import { durationLabel, statusLabel } from "../core/presentation";
import { Button, Empty, Field, Loading } from "../ui/Controls";
import { ProductCard, ProductArtwork } from "../ui/ProductCard";
import { colors, money, styles } from "../ui/theme";

export function CartScreen(): JSX.Element {
    const { activity, products, token, t, run, busy, refresh, navigate, toggle } = useStore();
    const { width } = useWindowDimensions();
    const [channel, setChannel] = useState("CARD");
    const [outcome, setOutcome] = useState("SUCCESS");
    const [showScenarios, setShowScenarios] = useState(false);
    const [result, setResult] = useState<IOrder | null>(null);
    const requestKey = useRef({ fingerprint: "", key: "" });
    const cart = products.filter((product) => activity.some((item) => item.kind === "CART" && item.targetId === product.id));
    const total = cart.reduce((amount, product) => amount + product.priceWon, 0);
    async function checkout() {
        const productIds = cart.map((product) => product.id).sort();
        const fingerprint = JSON.stringify({ productIds, channel, outcome });
        if (requestKey.current.fingerprint !== fingerprint) {
            requestKey.current = { fingerprint, key: `demo-${Date.now()}-${Math.random().toString(36).slice(2)}` };
        }
        const order = await read(decodeOrder, "/checkout", token, "POST", {
            productIds,
            requestKey: requestKey.current.key,
            channel,
            outcome,
        });
        setResult(order);
        requestKey.current = { fingerprint: "", key: "" };
        if (order.status === "SUCCESS") {
            await Promise.all(cart.map((product) => request(`/activity/CART/${product.id}`, token, "DELETE")));
            await refresh();
        }
    }
    if (result?.status === "SUCCESS") {
        return (
            <View style={[styles.page, { maxWidth: 800, width: "100%", alignSelf: "center", paddingTop: 24 }]}>
                <View style={{ alignSelf: "center", padding: 22, borderRadius: 48, backgroundColor: "#F4F1FE" }}>
                    <Text style={{ fontSize: 36, color: "#7256E8" }}>✓</Text>
                </View>
                <Text style={[styles.title, { textAlign: "center" }]}>
                    {t("좋은 선택이에요. 이제 만나볼까요?", "Great choice. Make yourself comfortable.")}
                </Text>
                <Text style={[styles.subtitle, { textAlign: "center" }]}>
                    {t("구매한 영상을 라이브러리에 담았어요. 실제로 청구된 금액은 없습니다.", "Your videos are in your library. No real money was charged.")}
                </Text>
                <View style={styles.panel}>
                    {result.lines.map((line) => (
                        <View key={line.id} style={styles.between}>
                            <Text style={[styles.text, { flex: 1 }]}>{line.title}</Text>
                            <Text style={styles.price}>{money(line.priceWon)}</Text>
                        </View>
                    ))}
                    <View style={styles.divider} />
                    <View style={styles.between}>
                        <Text style={styles.text}>{t("모의 결제 금액", "Demo total")}</Text>
                        <Text style={styles.price}>{money(result.totalWon)}</Text>
                    </View>
                    <Text style={styles.muted}>
                        {t("주문 번호", "Order")} · {result.id.slice(0, 8)}
                    </Text>
                </View>
                <Button
                    label={t("라이브러리로 이동", "Go to your library")}
                    icon="play-outline"
                    onPress={() => navigate("library")}
                />
                <Button
                    secondary
                    label={t("다른 영상 둘러보기", "Keep exploring")}
                    onPress={() => navigate("discover")}
                />
            </View>
        );
    }
    return (
        <View style={styles.page}>
            <Text style={styles.title}>{t("좋아하는 영상이 기다리고 있어요.", "Your next discovery is waiting.")}</Text>
            <Text style={styles.subtitle}>
                {t("구독 없이, 마음에 드는 영상만. 구매 후 라이브러리에서 만나요.", "Just the videos you want. Ready in your library after purchase.")}
            </Text>
            {!cart.length ? (
                <>
                    <Empty title={t("장바구니가 비어 있어요.", "Your cart is empty.")} />
                    <Button
                        secondary
                        label={t("영상 둘러보기", "Explore videos")}
                        onPress={() => navigate("discover")}
                    />
                </>
            ) : (
                <View style={{ flexDirection: width >= 1120 ? "row" : "column", gap: 28, alignItems: "flex-start" }}>
                    <View
                        style={{
                            flex: width >= 1120 ? 1 : undefined,
                            width: width >= 1120 ? undefined : "100%",
                            gap: 24,
                        }}
                    >
                        <View style={styles.panel}>
                            <Text style={styles.heading}>
                                {t(`선택한 영상 ${cart.length}개`, `${cart.length} selections`)}
                            </Text>
                            {cart.map((product) => (
                                <View
                                    style={[
                                        styles.row,
                                        { paddingVertical: 14, borderTopWidth: 1, borderTopColor: "#EDECF2" },
                                    ]}
                                    key={product.id}
                                >
                                    <View style={{ width: width < 500 ? 90 : 145 }}>
                                        <ProductArtwork product={product} />
                                    </View>
                                    <View style={{ flex: 1, minWidth: 110, gap: 5 }}>
                                        <Text style={[styles.text, { fontWeight: "700" }]}>{product.title}</Text>
                                        <Text style={styles.muted}>
                                            {product.sellerName} ·{" "}
                                            {product.termDays
                                                ? t(`${product.termDays}일 이용`, `${product.termDays} days`)
                                                : t("기간 제한 없음", "Unlimited")}
                                        </Text>
                                        <Text style={styles.price}>{money(product.priceWon)}</Text>
                                    </View>
                                    <Button
                                        secondary
                                        compact
                                        disabled={busy}
                                        label={t("삭제", "Remove")}
                                        onPress={() => run(() => toggle("CART", product.id))}
                                    />
                                </View>
                            ))}
                        </View>
                        <View style={styles.panel}>
                            <Text style={styles.heading}>{t("결제 수단", "Payment method")}</Text>
                            <View style={styles.row}>
                                {[
                                    ["CARD", "카드", "Card"],
                                    ["EASY", "간편결제", "Easy pay"],
                                ].map(([value, ko, en]) => (
                                    <Button
                                        key={value}
                                        secondary={channel !== value}
                                        disabled={busy}
                                        label={`${channel === value ? "✓ " : ""}${t(ko!, en!)}`}
                                        onPress={() => {
                                            setChannel(value!);
                                            setResult(null);
                                        }}
                                    />
                                ))}
                            </View>
                            <View style={{ padding: 16, backgroundColor: "#F4F1FE", borderRadius: 12, gap: 6 }}>
                                <Text style={[styles.text, { fontWeight: "600" }]}>
                                    {t("안심하고 체험하는 데모 결제", "A checkout you can safely try")}
                                </Text>
                                <Text style={styles.muted}>
                                    {t("실제 결제나 카드 정보 입력 없이 구매 과정을 체험합니다.", "Try the purchase flow without card details or real charges.")}
                                </Text>
                            </View>
                            <Button
                                secondary
                                compact
                                label={t(
                                    showScenarios ? "결제 시나리오 닫기" : "다른 결제 결과 체험",
                                    showScenarios ? "Hide scenarios" : "Try other payment outcomes",
                                )}
                                onPress={() => setShowScenarios(!showScenarios)}
                            />
                            {showScenarios ? (
                                <View style={styles.row}>
                                    {[
                                        ["SUCCESS", "정상 결제", "Success"],
                                        ["FAILED", "실패", "Failure"],
                                        ["CANCELED", "취소", "Cancel"],
                                    ].map(([value, ko, en]) => (
                                        <Button
                                            key={value}
                                            compact
                                            secondary={outcome !== value}
                                            disabled={busy}
                                            label={t(ko!, en!)}
                                            onPress={() => {
                                                setOutcome(value!);
                                                setResult(null);
                                            }}
                                        />
                                    ))}
                                </View>
                            ) : null}
                        </View>
                    </View>
                    <View style={[styles.panel, { width: width >= 1120 ? 320 : "100%" }]}>
                        <Text style={styles.heading}>{t("결제 요약", "Order summary")}</Text>
                        <View style={styles.between}>
                            <Text style={styles.muted}>{t("선택한 상품", "Selected items")}</Text>
                            <Text style={styles.text}>{cart.length}</Text>
                        </View>
                        <View style={styles.divider} />
                        <View style={styles.between}>
                            <Text style={styles.text}>{t("총 결제 금액", "Total")}</Text>
                            <Text style={[styles.price, { fontSize: 28 }]}>{money(total)}</Text>
                        </View>
                        {result ? (
                            <View accessibilityRole="alert" style={{ gap: 5 }}>
                                <Text style={{ color: "#BA3856" }}>
                                    {t(
                                        result.status === "FAILED" ? "결제 실패" : "결제 취소",
                                        result.status === "FAILED" ? "Payment failed" : "Payment canceled",
                                    )}
                                </Text>
                                <Text style={styles.muted}>
                                    {t("결제되지 않았어요. 장바구니는 그대로 유지됩니다. 정상 결제로 바꾸고 다시 시도해보세요.", "No charge was made. Your cart is saved. Select Success to try again.")}
                                </Text>
                            </View>
                        ) : null}
                        <Button
                            fullWidth
                            label={t(`${money(total)} 모의 결제하기`, `${money(total)} · Complete demo purchase`)}
                            disabled={busy}
                            onPress={() => run(checkout)}
                        />
                        <Text style={styles.muted}>
                            {t("구매 즉시 시청 · 자동 갱신 없음", "Watch immediately · No auto-renewal")}
                        </Text>
                    </View>
                </View>
            )}
        </View>
    );
}

export function LibraryScreen(): JSX.Element {
    const { library, t, activity } = useStore();
    const { width } = useWindowDimensions();
    const ordered = library.slice().sort((first, second) => Number(second.active) - Number(first.active));
    const items = ordered.filter(
        (item, index, list) => list.findIndex((other) => other.product.id === item.product.id) === index,
    );
    return (
        <View style={styles.page}>
            <Text style={styles.title}>{t("내 라이브러리", "Your library")}</Text>
            <Text style={styles.subtitle}>
                {t("나의 취향으로 채운 작은 세계", "A collection made for your curiosity.")}
            </Text>
            {!items.length ? <Empty title={t("첫 번째 영상을 골라보세요.", "Find your first video.")} /> : null}
            <View style={{ flexDirection: "row", flexWrap: "wrap", gap: 24 }}>
                {items.map((item) => (
                    <View key={item.id} style={{ width: width < 700 ? "100%" : "47%", gap: 12 }}>
                        <ProductCard product={item.product} width="100%" />
                        <Text style={styles.badge}>
                            {!item.active
                                ? t("이용 기간 만료", "Expired")
                                : item.expiresAt
                                  ? `${t("이용 기한", "Available until")}: ${new Date(item.expiresAt).toLocaleDateString()}`
                                  : t("기간 제한 없음", "Unlimited")}
                        </Text>
                        <WatchProgress
                            seconds={activity.find((entry) => entry.kind === "PROGRESS" && entry.targetId === item.product.id)?.numberValue ?? 0}
                            duration={item.product.durationSeconds}
                        />
                    </View>
                ))}
            </View>
        </View>
    );
}

function WatchProgress({ seconds, duration }: { seconds: number; duration: number }) {
    const { t } = useStore();
    const progress = Math.min(100, Math.max(0, (seconds / Math.max(1, duration)) * 100));
    return (
        <View style={{ gap: 8 }}>
            <View
                accessibilityRole="progressbar"
                accessibilityLabel={t("시청 진행률", "Watch progress")}
                aria-valuemin={0}
                aria-valuemax={100}
                aria-valuenow={Math.round(progress)}
                style={{ height: 4, backgroundColor: "#EDECF2", borderRadius: 4, overflow: "hidden" }}
            >
                <View style={{ height: 4, width: `${progress}%`, backgroundColor: "#7256E8" }} />
            </View>
            <Text style={styles.muted}>
                {seconds >= duration && duration > 0
                    ? t("시청 완료", "Finished")
                    : seconds > 0
                      ? t("이어보기", "Continue watching")
                      : t("아직 시청하지 않았어요", "Ready to watch")}{" "}
                · {durationLabel(seconds)} / {durationLabel(duration)}
            </Text>
        </View>
    );
}

export function OrdersScreen(): JSX.Element {
    const { token, t, language, run, busy, notify, navigate } = useStore();
    const [orders, setOrders] = useState<IOrder[]>([]);
    const [tickets, setTickets] = useState<ITicket[]>([]);
    const [selectedLine, setSelectedLine] = useState("");
    const [reason, setReason] = useState("");
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");
    const loadOrders = useCallback(
        async (signal?: AbortSignal) => {
            const [newOrders, newTickets] = await Promise.all([
                read(decodeArray(decodeOrder), "/orders", token, "GET", undefined, signal),
                read(decodeArray(decodeTicket), "/tickets", token, "GET", undefined, signal),
            ]);
            if (signal?.aborted) {
                return;
            }
            setOrders(newOrders);
            setTickets(newTickets);
            setError("");
            setLoading(false);
        },
        [token],
    );
    useEffect(() => {
        const controller = new AbortController();
        loadOrders(controller.signal).catch(() => {
            if (!controller.signal.aborted) {
                setError(t("주문 내역을 불러오지 못했어요.", "Couldn't load your orders."));
                setLoading(false);
            }
        });
        return () => controller.abort();
    }, [loadOrders, t]);
    function openRefund(id: string) {
        setReason("");
        setSelectedLine(id);
    }
    async function submitRefund() {
        await request("/tickets", token, "POST", { kind: "REFUND", targetId: selectedLine, message: reason.trim() });
        setSelectedLine("");
        setReason("");
        setTickets(await read(decodeArray(decodeTicket), "/tickets", token));
        notify(t("환불 요청을 접수했어요. 문의함에서 처리 상태를 확인할 수 있습니다.", "Refund request received. Track its status in your inbox."));
    }
    return (
        <View style={[styles.page, { maxWidth: 980, width: "100%" }]}>
            <View style={{ gap: 8 }}>
                <Text style={styles.title}>{t("주문 내역", "Your orders")}</Text>
                <Text style={styles.subtitle}>
                    {t("구매한 영상과 결제 내역을 확인하세요.", "Review your purchases and payment history.")}
                </Text>
            </View>
            {loading ? (
                <Loading />
            ) : error ? (
                <View style={styles.panel}>
                    <Text style={styles.text}>{error}</Text>
                    <Button
                        secondary
                        label={t("다시 시도", "Try again")}
                        disabled={busy}
                        onPress={() => run(() => loadOrders())}
                    />
                </View>
            ) : !orders.length ? (
                <Empty
                    title={t("아직 주문이 없어요.", "No orders yet.")}
                    description={t("마음에 드는 영상을 구매하면 이곳에 모아드릴게요.", "Your purchases will appear here.")}
                />
            ) : null}
            {orders.slice()
                .sort((a, b) => b.createdAt - a.createdAt)
                .map((order) => (
                    <View key={order.id} style={[styles.panel, { gap: 20 }]}>
                        <View style={styles.between}>
                            <View style={{ gap: 4 }}>
                                <Text style={styles.sectionTitle}>
                                    {new Date(order.createdAt).toLocaleDateString(
                                        language === "ko" ? "ko-KR" : "en-US",
                                    )}
                                </Text>
                                <Text style={styles.muted}>
                                    {t("주문", "Order")} {order.id.slice(0, 8)} ·{" "}
                                    {order.channel === "CARD" ? t("카드", "Card") : t("간편결제", "Easy pay")} ·{" "}
                                    {t("데모 결제", "Demo payment")}
                                </Text>
                            </View>
                            <Text style={styles.badge}>
                                {order.status === "SUCCESS" && order.lines.some((line) => line.refunded)
                                    ? t("일부 환불", "Partially refunded")
                                    : statusLabel(order.status, language)}
                            </Text>
                        </View>
                        {order.lines.map((line) => {
                            const pending = tickets.some(
                                (ticket) =>
                                    ticket.kind === "REFUND" && ticket.targetId === line.id && ticket.status === "OPEN",
                            );
                            return (
                                <View
                                    key={line.id}
                                    style={{ gap: 16, paddingTop: 16, borderTopWidth: 1, borderColor: colors.line }}
                                >
                                    <View style={styles.between}>
                                        <View style={{ flex: 1, minWidth: 150, gap: 6 }}>
                                            <Text style={[styles.text, { fontWeight: "600" }]}>{line.title}</Text>
                                            <Text style={styles.muted}>
                                                {line.termDays
                                                    ? t(`${line.termDays}일 이용`, `${line.termDays} days of access`)
                                                    : t("기간 제한 없음", "Unlimited access")}
                                            </Text>
                                        </View>
                                        <Text style={[styles.text, { fontWeight: "600" }]}>{money(line.priceWon)}</Text>
                                    </View>
                                    <View style={styles.actionRow}>
                                        {line.refunded ? (
                                            <Text style={styles.badge}>{t("환불 완료", "Refunded")}</Text>
                                        ) : pending ? (
                                            <>
                                                <Text style={styles.muted}>
                                                    {t("환불 요청 검토 중", "Refund request in review")}
                                                </Text>
                                                <Button
                                                    quiet
                                                    compact
                                                    label={t("문의함에서 확인", "View in inbox")}
                                                    onPress={() => navigate("inbox")}
                                                />
                                            </>
                                        ) : order.status === "SUCCESS" && selectedLine !== line.id ? (
                                            <Button
                                                quiet
                                                compact
                                                label={t("환불 요청", "Request refund")}
                                                disabled={busy}
                                                onPress={() => openRefund(line.id)}
                                            />
                                        ) : null}
                                    </View>
                                    {selectedLine === line.id ? (
                                        <View
                                            style={{
                                                backgroundColor: "#F8F7FC",
                                                borderRadius: 12,
                                                padding: 20,
                                                gap: 16,
                                            }}
                                        >
                                            <View style={{ gap: 6 }}>
                                                <Text style={styles.sectionTitle}>
                                                    {t("환불을 요청하시겠어요?", "Request a refund?")}
                                                </Text>
                                                <Text style={styles.muted}>
                                                    {t("이 상품의 환불 사유를 알려주세요. 요청을 제출하면 운영자가 검토하며, 즉시 환불되지는 않습니다.", "Tell us why you'd like a refund for this item. Your request will be reviewed; submitting it does not issue an immediate refund.")}
                                                </Text>
                                            </View>
                                            <Field
                                                label={t("환불 요청 사유", "Refund reason")}
                                                placeholder={t("환불을 요청하는 이유를 적어주세요.", "Tell us why you'd like a refund.")}
                                                value={reason}
                                                onChangeText={setReason}
                                                multiline
                                                maxLength={2000}
                                                autoFocus
                                            />
                                            <View style={styles.actionRow}>
                                                <Button
                                                    secondary
                                                    label={t("취소", "Cancel")}
                                                    disabled={busy}
                                                    onPress={() => {
                                                        setSelectedLine("");
                                                        setReason("");
                                                    }}
                                                />
                                                <Button
                                                    loading={busy}
                                                    label={t("환불 요청 제출", "Submit refund request")}
                                                    disabled={busy || !reason.trim()}
                                                    onPress={() => run(submitRefund)}
                                                />
                                            </View>
                                        </View>
                                    ) : null}
                                </View>
                            );
                        })}
                        <OrderPaymentSummary order={order} />
                    </View>
                ))}
        </View>
    );
}

function OrderPaymentSummary({ order }: { order: IOrder }): JSX.Element {
    const { t } = useStore();
    const wasPaid = order.status === "SUCCESS" || order.status === "REFUNDED";
    const refundedLines = order.lines.filter((line) => line.refunded);
    const refundedWon = refundedLines.reduce((total, line) => total + line.priceWon, 0);
    return (
        <View style={{ gap: 12, paddingTop: 16, borderTopWidth: 1, borderColor: colors.line }}>
            <View style={styles.between}>
                <Text style={styles.muted}>
                    {wasPaid ? t("주문 당시 결제 금액", "Original payment amount") : t("결제된 금액", "Amount paid")}
                </Text>
                <Text style={styles.sectionTitle}>{money(wasPaid ? order.totalWon : 0)}</Text>
            </View>
            {wasPaid && refundedLines.length > 0 ? (
                <View style={styles.between}>
                    <Text style={styles.muted}>{t("환불 완료 금액", "Refunded amount")}</Text>
                    <Text style={[styles.sectionTitle, { color: colors.violet }]}>{money(refundedWon)}</Text>
                </View>
            ) : null}
            {!wasPaid ? (
                <Text style={styles.muted}>
                    {order.status === "FAILED"
                        ? t("결제에 실패하여 결제된 금액이 없습니다.", "Payment failed. No payment was made.")
                        : t("결제가 취소되어 결제된 금액이 없습니다.", "Payment was canceled. No payment was made.")}
                </Text>
            ) : null}
        </View>
    );
}
