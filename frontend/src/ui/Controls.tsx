import { Pressable, Text, TextInput, View, ActivityIndicator, type TextInputProps } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import type { ComponentProps } from "react";
import { colors, styles } from "./theme";
import { useStore } from "../core/Store";

export function Button({ label, onPress, secondary = false, disabled = false, icon, compact = false }: { label: string; onPress: () => void; secondary?: boolean; disabled?: boolean; icon?: ComponentProps<typeof Ionicons>["name"]; compact?: boolean }) {
    return <Pressable accessibilityRole="button" accessibilityLabel={label} disabled={disabled} onPress={onPress}
        style={({ pressed }) => ({ backgroundColor: secondary ? colors.pale : colors.violet, borderRadius: 10, paddingHorizontal: 19,
            paddingVertical: compact ? 10 : 15, minHeight: compact ? 40 : 49, flexDirection: "row", gap: 8, alignItems: "center", justifyContent: "center", opacity: disabled ? .45 : pressed ? .7 : 1 })}>
        {icon ? <Ionicons name={icon} size={18} color={secondary ? colors.violet : "white"} /> : null}
        <Text style={{ fontSize: compact ? 13 : 14, fontWeight: "700", textAlign: "center", color: secondary ? colors.violet : "white" }}>{label}</Text>
    </Pressable>;
}
export function Field({ label, ...props }: TextInputProps & { label: string }) {
    return <View><Text style={styles.label}>{label}</Text><TextInput accessibilityLabel={label} placeholderTextColor="#A2A0AF" {...props} style={[styles.input, props.multiline && { minHeight: 100 }, props.style]} /></View>;
}
export function Empty({ title, description }: { title: string; description?: string }) {
    const { t, navigate } = useStore();
    return <View style={{ paddingVertical: 44, paddingHorizontal: 20, alignItems: "center", gap: 18 }}>
        <View style={{ width: 70, height: 70, borderRadius: 35, backgroundColor: colors.pale, alignItems: "center", justifyContent: "center" }}><Ionicons name="play-outline" size={30} color={colors.violet} /></View>
        <Text style={[styles.heading, { textAlign: "center" }]}>{title}</Text>
        {description ? <Text style={styles.subtitle}>{description}</Text> : null}
        <Button label={t("영상 둘러보기", "Explore videos")} onPress={() => navigate("discover")} secondary /></View>;
}
export function Loading() { return <ActivityIndicator size="large" color={colors.violet} style={{ margin: 40 }} />; }
