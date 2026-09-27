import type { JSX } from "react";
import { getErrorMessage } from "../core/validation";
import { decodeDashboard } from "../core/contracts";
import { useEffect, useState, useCallback } from "react";
import { Text, View } from "react-native";
import { useStore } from "../core/Store";
import { request, read } from "../core/api";
import type { IDashboard } from "../core/types";
import { Button, Field, Loading } from "../ui/Controls";
import { money, styles } from "../ui/theme";
import { statusLabel } from "../core/presentation";
import { InspectionPlayer } from "../ui/InspectionPlayer";

export function AdminScreen(): JSX.Element {
    const { token, user, t, language, run, refresh } = useStore();
    const [dashboard, setDashboard] = useState<IDashboard | null>(null);
    const [reply, setReply] = useState("");
    const [sellerId, setSellerId] = useState("seller");
    const [roleId, setRoleId] = useState("");
    const [role, setRole] = useState("CONTENT");
    const [loadError, setLoadError] = useState("");
    const reload = useCallback(
        async (signal?: AbortSignal) => {
            const result = await read(decodeDashboard, "/admin/dashboard", token, "GET", undefined, signal);
            if (!signal?.aborted) {
                setDashboard(result);
                setLoadError("");
            }
        },
        [token],
    );
    useEffect(() => {
        const controller = new AbortController();
        setDashboard(null);
        setLoadError("");
        reload(controller.signal).catch((error: unknown) => {
            if (!controller.signal.aborted) {
                setLoadError(getErrorMessage(error));
            }
        });
        return () => controller.abort();
    }, [reload]);
    async function decide(path: string, body: unknown) {
        await request(path, token, "POST", body);
        await reload();
        await refresh();
    }
    if (loadError) {
        return (
            <View style={styles.page}>
                <Text style={styles.text}>{loadError}</Text>
                <Button label={t("다시 시도", "Retry")} onPress={() => run(() => reload())} />
            </View>
        );
    }
    if (!dashboard) {
        return <Loading />;
    }
    return (
        <View style={styles.page}>
            <Text style={styles.title}>{t("운영 관리", "Operations")}</Text>
            <Text style={styles.subtitle}>
                {t("검토하고, 보호하고, 신뢰를 쌓는 공간", "A workspace for a trusted creator community.")} ·{" "}
                {user?.role}
            </Text>
            {dashboard.accounts
                .filter((account) => account.sellerStatus === "PENDING")
                .map((account) => (
                    <View key={account.id} style={styles.panel}>
                        <Text style={styles.heading}>{account.displayName}</Text>
                        <Text style={styles.text}>{account.bio}</Text>
                        <View style={styles.row}>
                            <Button
                                label={t("판매자 승인", "Approve seller")}
                                onPress={() => run(() => decide(`/admin/sellers/${account.id}`, { approve: true }))}
                            />
                            <Button
                                secondary
                                label={t("반려", "Reject")}
                                onPress={() => run(() => decide(`/admin/sellers/${account.id}`, { approve: false }))}
                            />
                        </View>
                    </View>
                ))}
            {dashboard.products.length ? (
                <Text style={styles.heading}>{t("콘텐츠 검수", "Content review")}</Text>
            ) : null}
            {dashboard.products.map((product) => (
                <View style={[styles.panel, styles.between]} key={product.id}>
                    <View style={{ flex: 1 }}>
                        <Text style={styles.text}>{product.title}</Text>
                        <Text style={styles.muted}>
                            {product.sellerName} · {statusLabel(product.status, language)}
                        </Text>
                    </View>
                    <Button
                        label={t("승인", "Approve")}
                        onPress={() => run(() => decide(`/admin/products/${product.id}`, { decision: "APPROVE" }))}
                    />
                    <Button
                        secondary
                        label={t("반려", "Reject")}
                        onPress={() => run(() => decide(`/admin/products/${product.id}`, { decision: "REJECT" }))}
                    />
                    <Button
                        secondary
                        label={t("제공 중단", "Block access")}
                        onPress={() => run(() => decide(`/admin/products/${product.id}`, { decision: "BLOCK" }))}
                    />
                    {product.kind === "VIDEO" && product.durationSeconds > 0 ? (
                        <InspectionPlayer productId={product.id} />
                    ) : null}
                </View>
            ))}
            <Text style={styles.heading}>{t("신고 · 고객지원 · 환불", "Reports · Support · Refunds")}</Text>
            <Field label={t("처리 답변", "Resolution message")} value={reply} onChangeText={setReply} multiline />
            {dashboard.tickets.map((ticket) => (
                <View style={styles.panel} key={ticket.id}>
                    <View style={styles.between}>
                        <Text style={styles.badge}>
                            {statusLabel(ticket.kind, language)} · {statusLabel(ticket.status, language)}
                        </Text>
                        <Text style={styles.muted}>{ticket.id.slice(0, 8)}</Text>
                    </View>
                    <Text style={styles.text}>{ticket.message}</Text>
                    {ticket.reply ? <Text style={styles.subtitle}>{ticket.reply}</Text> : null}
                    {ticket.status === "OPEN" ? (
                        <View style={styles.row}>
                            <Button
                                label={t("승인 / 처리", "Approve / Resolve")}
                                onPress={() =>
                                    run(() => decide(`/admin/tickets/${ticket.id}`, { approve: true, reply }))
                                }
                            />
                            <Button
                                secondary
                                label={t("거절", "Reject")}
                                onPress={() =>
                                    run(() => decide(`/admin/tickets/${ticket.id}`, { approve: false, reply }))
                                }
                            />
                        </View>
                    ) : null}
                </View>
            ))}
            {["ADMIN", "FINANCE"].includes(user?.role ?? "") ? (
                <View style={styles.panel}>
                    <Text style={styles.heading}>{t("모의 정산", "Mock settlements")}</Text>
                    <Text style={styles.subtitle}>
                        {t("전월 실적 · 매월 15일부터 · 1만원 미만 이월", "Prior month · From the 15th · KRW 10,000 minimum")}
                    </Text>
                    <Text style={styles.text}>
                        {t("미환불 판매자 몫", "Unrefunded seller share")}:{" "}
                        {money(
                            dashboard.lines
                                .filter((line) => !line.refunded)
                                .reduce((sum, line) => sum + line.sellerAmountWon, 0),
                        )}
                    </Text>
                    {dashboard.adjustments
                        .filter((item) => !item.settlementId)
                        .map((item) => (
                            <Text key={item.lineId} style={styles.muted}>
                                {t("환불 차감 이월", "Refund deduction carried forward")}: {item.sellerId} · −
                                {money(item.amountWon)}
                            </Text>
                        ))}
                    <Field label={t("판매자 ID", "Seller ID")} value={sellerId} onChangeText={setSellerId} />
                    <Button
                        label={t("모의 정산 실행", "Run mock settlement")}
                        onPress={() => run(() => decide(`/admin/settlements/${sellerId}`, {}))}
                    />
                </View>
            ) : null}
            {user?.role === "ADMIN" ? (
                <>
                    <View style={styles.panel}>
                        <Text style={styles.heading}>{t("운영자 권한 관리", "Operator roles")}</Text>
                        <Field label={t("계정 ID", "Account ID")} value={roleId} onChangeText={setRoleId} />
                        <View style={styles.row}>
                            {["BUYER", "CONTENT", "SUPPORT", "FINANCE", "ADMIN"].map((value) => (
                                <Button
                                    key={value}
                                    label={value}
                                    secondary={role !== value}
                                    onPress={() => setRole(value)}
                                />
                            ))}
                        </View>
                        <Button
                            label={t("권한 변경", "Update role")}
                            onPress={() => run(() => decide(`/admin/roles/${roleId}`, { decision: role }))}
                        />
                        {dashboard.accounts.map((account) => (
                            <Text key={account.id} style={styles.muted}>
                                {account.id} · {account.email} · {account.role}
                            </Text>
                        ))}
                    </View>
                    <Text style={styles.heading}>{t("운영 기록", "Audit log")}</Text>
                    {dashboard.audits.slice().reverse()
                        .map((audit) => (
                            <Text key={audit.id} style={styles.muted}>
                                {new Date(audit.createdAt).toLocaleString()} · {audit.actorId} · {audit.action} ·{" "}
                                {audit.detail}
                            </Text>
                        ))}
                </>
            ) : null}
        </View>
    );
}
