import { Pressable, Text, TextInput, View, ActivityIndicator, type TextInputProps } from "react-native";
import { colors, styles } from "./theme";
import { useStore } from "../core/Store";

export function Button({ label, onPress, secondary = false, disabled = false }: { label: string; onPress: () => void; secondary?: boolean; disabled?: boolean }) {
    return <Pressable accessibilityRole="button" accessibilityLabel={label} disabled={disabled} onPress={onPress}
        style={({ pressed }) => ({ backgroundColor: secondary ? colors.pale : colors.violet, borderRadius: 10, paddingHorizontal: 19,
            paddingVertical: 13, minHeight: 46, alignItems: "center", justifyContent: "center", opacity: disabled ? .45 : pressed ? .7 : 1 })}>
        <Text style={{ fontSize: 14, fontWeight: "700", color: secondary ? colors.violet : "white" }}>{label}</Text>
    </Pressable>;
}
export function Field({ label, ...props }: TextInputProps & { label: string }) {
    return <View><Text style={styles.label}>{label}</Text><TextInput accessibilityLabel={label} placeholderTextColor="#A2A0AF" {...props} style={[styles.input, props.multiline && { minHeight: 100 }, props.style]} /></View>;
}
export function Empty({ title, description }: { title: string; description?: string }) {
    const { t, navigate } = useStore();
    return <View style={{ padding: 44, alignItems: "center", gap: 18 }}><Text style={styles.heading}>{title}</Text>
        {description ? <Text style={styles.subtitle}>{description}</Text> : null}
        <Button label={t("영상 둘러보기", "Explore videos")} onPress={() => navigate("discover")} secondary /></View>;
}
export function Loading() { return <ActivityIndicator size="large" color={colors.violet} style={{ margin: 40 }} />; }
