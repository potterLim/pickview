import type { JSX } from "react";
import { Pressable, Text, View } from "react-native";
import { colors, styles } from "./theme";

export function CreatorIdentity({
    name,
    subtitle,
    size = 42,
    onPress,
}: {
    name: string;
    subtitle?: string;
    size?: number;
    onPress?: () => void;
}): JSX.Element {
    const initial = name.replace(/^스튜디오\s*/, "").slice(0, 1);
    const content = (
        <View style={{ flexDirection: "row", alignItems: "center", gap: 12, minWidth: 0 }}>
            <View
                style={{
                    width: size,
                    height: size,
                    borderRadius: size / 2,
                    backgroundColor: "#EAD6CB",
                    alignItems: "center",
                    justifyContent: "center",
                }}
            >
                <Text style={{ fontSize: size * 0.36, fontWeight: "700", color: "#6A4D41" }}>{initial}</Text>
            </View>
            <View style={{ gap: 4, flexShrink: 1 }}>
                <Text style={{ fontSize: size > 60 ? 25 : 15, color: colors.ink, fontWeight: "700" }}>{name}</Text>
                {subtitle ? (
                    <Text numberOfLines={1} ellipsizeMode="middle" accessibilityLabel={subtitle} style={styles.muted}>
                        {subtitle}
                    </Text>
                ) : null}
            </View>
        </View>
    );
    return onPress ? (
        <Pressable accessibilityRole="button" accessibilityLabel={name} onPress={onPress}>
            {content}
        </Pressable>
    ) : content;
}
