import { StyleSheet } from "react-native";

export const colors = { ink: "#20202A", muted: "#696978", violet: "#6B4EDB", pale: "#F4F1FE", line: "#EDECF2", white: "#FFFFFF", red: "#BA3856" };
export const styles = StyleSheet.create({
    page: { gap: 24, paddingBottom: 40 },
    title: { fontSize: 30, fontWeight: "800", color: colors.ink, letterSpacing: -1 },
    subtitle: { fontSize: 16, color: colors.muted, lineHeight: 25 },
    heading: { fontSize: 22, fontWeight: "700", color: colors.ink },
    sectionTitle: { fontSize: 17, fontWeight: "600", color: colors.ink, lineHeight: 25 },
    actionRow: { flexDirection: "row", alignItems: "center", justifyContent: "flex-end", flexWrap: "wrap", gap: 16 },
    text: { fontSize: 15, color: colors.ink, lineHeight: 23 },
    muted: { fontSize: 13, color: colors.muted, lineHeight: 20 },
    row: { flexDirection: "row", alignItems: "center", gap: 12, flexWrap: "wrap" },
    between: { flexDirection: "row", alignItems: "center", justifyContent: "space-between", gap: 12, flexWrap: "wrap" },
    panel: { borderWidth: 1, borderColor: colors.line, borderRadius: 16, padding: 24, gap: 16, backgroundColor: colors.white },
    input: { borderWidth: 1, borderColor: colors.line, borderRadius: 10, backgroundColor: "#FAFAFC", color: colors.ink, padding: 14, fontSize: 15, minHeight: 48 },
    label: { fontSize: 13, fontWeight: "600", color: colors.ink, marginBottom: 8 },
    divider: { height: 1, backgroundColor: colors.line, marginVertical: 8 },
    price: { fontSize: 20, fontWeight: "800", color: colors.violet },
    badge: { alignSelf: "flex-start", overflow: "hidden", color: colors.violet, backgroundColor: colors.pale, paddingHorizontal: 10, paddingVertical: 5, borderRadius: 6, fontSize: 12 },
});
export function money(amount: number): string { return `₩${amount.toLocaleString("ko-KR")}`; }
