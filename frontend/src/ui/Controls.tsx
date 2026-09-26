import { Pressable, Text, TextInput, View, ActivityIndicator, type TextInputProps } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useState, type ComponentProps } from "react";
import { colors, styles } from "./theme";
import { useStore } from "../core/Store";

type IconName = ComponentProps<typeof Ionicons>["name"];
interface IButtonProps {
    label: string;
    onPress: () => void;
    secondary?: boolean;
    disabled?: boolean;
    icon?: IconName;
    compact?: boolean;
    fullWidth?: boolean;
    quiet?: boolean;
    loading?: boolean;
}

export function Button({
    label,
    onPress,
    secondary = false,
    disabled = false,
    icon,
    compact = false,
    fullWidth = false,
    quiet = false,
    loading = false,
}: IButtonProps) {
    const [focused, setFocused] = useState(false);
    const [hovered, setHovered] = useState(false);
    const inactive = disabled || loading;
    const textColor = inactive ? colors.muted : secondary || quiet ? colors.violet : colors.white;
    return (
        <Pressable
            accessibilityRole="button"
            accessibilityLabel={label}
            aria-disabled={inactive}
            aria-busy={loading}
            disabled={inactive}
            onPress={onPress}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            onHoverIn={() => setHovered(true)}
            onHoverOut={() => setHovered(false)}
            style={({ pressed }) => ({
                alignSelf: fullWidth ? "stretch" : "flex-start",
                maxWidth: "100%",
                flexShrink: 0,
                backgroundColor: inactive
                    ? "#F1F1F5"
                    : quiet
                      ? hovered
                          ? colors.pale
                          : "transparent"
                      : secondary
                        ? hovered
                            ? "#EEEBFA"
                            : colors.white
                        : hovered
                          ? "#5F43D3"
                          : colors.violet,
                borderWidth: 1,
                borderColor: focused ? colors.violet : secondary && !inactive ? "#DFDBEC" : "transparent",
                borderRadius: 9,
                paddingHorizontal: compact ? 13 : 18,
                paddingVertical: 10,
                minHeight: 44,
                flexDirection: "row",
                gap: 8,
                alignItems: "center",
                justifyContent: "center",
                opacity: pressed ? 0.76 : 1,
            })}
        >
            {loading ? (
                <ActivityIndicator size="small" color={textColor} />
            ) : icon ? (
                <Ionicons name={icon} size={17} color={textColor} />
            ) : null}
            <Text
                style={{
                    fontSize: compact ? 13 : 14,
                    fontWeight: "600",
                    textAlign: "center",
                    flexShrink: 1,
                    color: textColor,
                }}
            >
                {label}
            </Text>
        </Pressable>
    );
}

export function Choice({
    label,
    selected,
    onPress,
    multiple = false,
    disabled = false,
}: {
    label: string;
    selected: boolean;
    onPress: () => void;
    multiple?: boolean;
    disabled?: boolean;
}) {
    const [focused, setFocused] = useState(false);
    const [hovered, setHovered] = useState(false);
    return (
        <Pressable
            accessibilityRole={multiple ? "checkbox" : "radio"}
            accessibilityLabel={label}
            aria-checked={selected}
            aria-disabled={disabled}
            disabled={disabled}
            onPress={onPress}
            onFocus={() => setFocused(true)}
            onBlur={() => setFocused(false)}
            onHoverIn={() => setHovered(true)}
            onHoverOut={() => setHovered(false)}
            style={({ pressed }) => ({
                minHeight: 44,
                paddingVertical: 10,
                paddingHorizontal: 15,
                borderRadius: 9,
                flexDirection: "row",
                alignItems: "center",
                gap: 8,
                borderWidth: 1,
                borderColor: selected || focused ? colors.violet : "#DFDBEC",
                backgroundColor: selected ? colors.pale : hovered ? "#F8F7FB" : colors.white,
                opacity: disabled ? 0.55 : pressed ? 0.75 : 1,
            })}
        >
            <Ionicons
                name={selected ? "checkmark-circle" : multiple ? "ellipse-outline" : "radio-button-off-outline"}
                size={17}
                color={selected ? colors.violet : colors.muted}
            />
            <Text
                style={{
                    color: selected ? colors.violet : colors.ink,
                    fontSize: 14,
                    fontWeight: selected ? "600" : "400",
                }}
            >
                {label}
            </Text>
        </Pressable>
    );
}

export function Field({ label, ...props }: TextInputProps & { label: string }) {
    const [focused, setFocused] = useState(false);
    return (
        <View>
            <Text style={styles.label}>{label}</Text>
            <TextInput
                accessibilityLabel={label}
                placeholderTextColor="#8B8997"
                {...props}
                onFocus={(event) => {
                    setFocused(true);
                    props.onFocus?.(event);
                }}
                onBlur={(event) => {
                    setFocused(false);
                    props.onBlur?.(event);
                }}
                style={[
                    styles.input,
                    props.multiline && { minHeight: 116, textAlignVertical: "top" },
                    focused && {
                        borderColor: colors.violet,
                        backgroundColor: colors.white,
                        outlineColor: "#B5A5F1",
                        outlineWidth: 2,
                        outlineOffset: 2,
                    },
                    props.style,
                ]}
            />
        </View>
    );
}
export function Empty({ title, description }: { title: string; description?: string }) {
    const { t, navigate } = useStore();
    return (
        <View style={{ paddingVertical: 44, paddingHorizontal: 20, alignItems: "center", gap: 18 }}>
            <View
                style={{
                    width: 70,
                    height: 70,
                    borderRadius: 35,
                    backgroundColor: colors.pale,
                    alignItems: "center",
                    justifyContent: "center",
                }}
            >
                <Ionicons name="play-outline" size={30} color={colors.violet} />
            </View>
            <Text style={[styles.heading, { textAlign: "center" }]}>{title}</Text>
            {description ? <Text style={styles.subtitle}>{description}</Text> : null}
            <View>
                <Button label={t("영상 둘러보기", "Explore videos")} onPress={() => navigate("discover")} secondary />
            </View>
        </View>
    );
}
export function Loading() {
    return <ActivityIndicator size="large" color={colors.violet} style={{ margin: 40 }} />;
}
