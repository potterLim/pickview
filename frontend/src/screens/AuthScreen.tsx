import type { JSX } from "react";
import { decodeToken } from "../core/contracts";
import { useState } from "react";
import { Platform, Text, View } from "react-native";
import { useStore } from "../core/Store";
import { read } from "../core/api";
import { Button, Field } from "../ui/Controls";
import { styles } from "../ui/theme";

export function AuthScreen(): JSX.Element {
    const { t, run, signIn, busy, notify } = useStore();
    const [email, setEmail] = useState("buyer@pickview.demo");
    const [password, setPassword] = useState("PickView-demo-2026!");
    const [name, setName] = useState("");
    const [register, setRegister] = useState(false);
    const [adult, setAdult] = useState(false);
    async function submit() {
        const result = await read(decodeToken, register ? "/auth/register" : "/auth/login", "", "POST", {
            email,
            password,
            name,
            isAdult: adult,
        });
        await signIn(result.token);
    }
    return (
        <View style={[styles.panel, { maxWidth: 500, width: "100%", alignSelf: "center", marginVertical: 30 }]}>
            <Text style={styles.title}>{t("반가워요, PickView", "Welcome to PickView")}</Text>
            <Text style={styles.subtitle}>
                {t("나에게 필요한 영상과 만나는 곳", "A little curiosity starts here.")}
            </Text>
            <Field
                label={t("이메일", "Email")}
                value={email}
                onChangeText={setEmail}
                autoCapitalize="none"
                keyboardType="email-address"
            />
            <Field label={t("비밀번호", "Password")} value={password} onChangeText={setPassword} secureTextEntry />
            {register ? (
                <>
                    <Field label={t("이름", "Name")} value={name} onChangeText={setName} />
                    <Button
                        secondary
                        label={`${adult ? "✓ " : ""}${t("성인임을 확인합니다 (데모)", "I confirm I am an adult (demo)")}`}
                        onPress={() => setAdult(!adult)}
                    />
                </>
            ) : null}
            <Button
                fullWidth
                label={register ? t("회원가입", "Create account") : t("로그인", "Sign in")}
                disabled={busy || (register && !adult)}
                onPress={() => run(submit)}
            />
            <Button
                secondary
                label={
                    register ? t("로그인으로 돌아가기", "Back to sign in") : t("새 계정 만들기", "Create an account")
                }
                onPress={() => setRegister(!register)}
            />
            <Text style={styles.label}>{t("데모 계정 선택", "Choose a demo account")}</Text>
            <View style={styles.row}>
                {["buyer", "seller", "admin", "content", "support", "finance"].map((role) => (
                    <Button
                        secondary
                        key={role}
                        label={role}
                        onPress={() => {
                            setEmail(`${role}@pickview.demo`);
                            setPassword("PickView-demo-2026!");
                        }}
                    />
                ))}
            </View>
            <Text style={styles.muted}>
                {t("소셜 로그인은 연결 전 모의 화면입니다.", "Social sign-in is a simulation, not connected.")}
            </Text>
            <View style={styles.row}>
                {["Kakao", "Google", ...(Platform.OS === "ios" ? ["Apple"] : [])].map((provider) => (
                    <Button
                        key={provider}
                        secondary
                        label={`${provider} (${t("모의", "mock")})`}
                        onPress={() => notify(t("소셜 인증 연결 전입니다. 위 데모 계정으로 로그인하세요.", "Use a demo account above; social authentication is not connected."))}
                    />
                ))}
            </View>
        </View>
    );
}
