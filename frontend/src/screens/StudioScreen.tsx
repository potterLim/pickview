import { createProductPrice } from "../core/commerceValues";
import { ProductDraft, type IProductInput } from "../core/ProductDraft";
import type { ProductId } from "../core/identifiers";
import { ECategory, EProductKind } from "../core/domain";
import { getErrorMessage } from "../core/validation";
import { decodeArray } from "../core/validation";
import { decodeProduct, decodeOrderLine, decodeSettlementSummary } from "../core/contracts";
import { useEffect, useState, useCallback } from "react";
import { Text, View, Platform } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import { useStore } from "../core/Store";
import { API_URL, request, read, requireSuccessfulResponse } from "../core/api";
import type { IOrderLine, IProduct, ISettlementSummary } from "../core/types";
import { Button, Field, Choice } from "../ui/Controls";
import { colors, money, styles } from "../ui/theme";
import { categoryLabel, statusLabel } from "../core/presentation";
import { CreatorIdentity } from "../ui/CreatorIdentity";
import { InspectionPlayer } from "../ui/InspectionPlayer";

export function StudioScreen() {
    const { user, token, t, language, run, busy, refresh, notify } = useStore();
    const [products, setProducts] = useState<IProduct[]>([]);
    const [sales, setSales] = useState<IOrderLine[]>([]);
    const [settlement, setSettlement] = useState<ISettlementSummary | null>(null);
    const [editing, setEditing] = useState<IProduct | null>(null);
    const [showEditor, setShowEditor] = useState(false);
    const [showProfile, setShowProfile] = useState(false);
    const [displayName, setDisplayName] = useState(user?.displayName ?? "");
    const [bio, setBio] = useState((user?.bio ?? "").replace(/^(PERSONAL|BUSINESS):\s*/, ""));
    const [type, setType] = useState(user?.bio.startsWith("BUSINESS:") ? "BUSINESS" : "PERSONAL");
    const reload = useCallback(
        async (signal?: AbortSignal) => {
            const [catalog, lines, summary] = await Promise.all([
                read(decodeArray(decodeProduct), "/seller/products", token, "GET", undefined, signal),
                read(decodeArray(decodeOrderLine), "/seller/sales", token, "GET", undefined, signal),
                read(decodeSettlementSummary, "/seller/settlements", token, "GET", undefined, signal),
            ]);
            if (signal?.aborted) {
                return;
            }
            setProducts(catalog);
            setSales(lines);
            setSettlement(summary);
        },
        [token],
    );
    useEffect(() => {
        if (user?.sellerStatus !== "APPROVED") {
            return;
        }
        const controller = new AbortController();
        reload(controller.signal).catch((error: unknown) => {
            if (!controller.signal.aborted) {
                notify(getErrorMessage(error));
            }
        });
        return () => controller.abort();
    }, [reload, notify, user?.sellerStatus]);
    useEffect(() => {
        setDisplayName(user?.displayName ?? "");
        setBio((user?.bio ?? "").replace(/^(PERSONAL|BUSINESS):\s*/, ""));
        setType(user?.bio.startsWith("BUSINESS:") ? "BUSINESS" : "PERSONAL");
    }, [user?.id, user?.bio, user?.displayName]);
    if (user?.sellerStatus !== "APPROVED") {
        const pending = user?.sellerStatus === "PENDING";
        return (
            <View style={[styles.page, { maxWidth: 840, width: "100%" }]}>
                <View style={{ gap: 8 }}>
                    <Text style={styles.title}>{t("나의 영상을 세상에", "Share what you know")}</Text>
                    <Text style={styles.subtitle}>
                        {t(
                            "작은 경험도 누군가에게는 새로운 발견이 됩니다.",
                            "Your everyday experience could be someone's next discovery.",
                        )}
                    </Text>
                </View>
                <View style={[styles.row, { gap: 20, paddingVertical: 6 }]}>
                    {[
                        t("01  채널 소개", "01  Your channel"),
                        t("02  신청 검토", "02  Review"),
                        t("03  영상 등록", "03  Publish"),
                    ].map((label, index) => (
                        <Text
                            key={label}
                            style={[
                                styles.muted,
                                {
                                    color: index === (pending ? 1 : 0) ? colors.violet : colors.muted,
                                    fontWeight: index === (pending ? 1 : 0) ? "700" : "400",
                                },
                            ]}
                        >
                            {label}
                        </Text>
                    ))}
                </View>
                <View style={[styles.panel, { gap: 24 }]}>
                    <View style={styles.between}>
                        <View style={{ gap: 5, flex: 1 }}>
                            <Text style={styles.sectionTitle}>
                                {t("채널을 소개해주세요", "Introduce your channel")}
                            </Text>
                            <Text style={styles.muted}>
                                {t("개인과 기업 모두 신청할 수 있어요.", "Individuals and businesses are welcome.")}
                            </Text>
                        </View>
                        <Text style={styles.badge}>{statusLabel(user?.sellerStatus ?? "NONE", language)}</Text>
                    </View>
                    <Field
                        label={t("채널 이름", "Channel name")}
                        placeholder={t("시청자에게 보여줄 이름", "The name your viewers will see")}
                        value={displayName}
                        onChangeText={setDisplayName}
                        editable={!pending}
                        maxLength={80}
                    />
                    <Field
                        label={t("채널 소개", "About your channel")}
                        placeholder={t("어떤 이야기를 전하고 싶으신가요?", "What would you like to share?")}
                        value={bio}
                        onChangeText={setBio}
                        editable={!pending}
                        maxLength={1000}
                        multiline
                    />
                    <View style={{ gap: 8 }}>
                        <Text style={styles.label}>{t("판매자 유형", "Creator type")}</Text>
                        <View
                            style={styles.row}
                            accessibilityRole="radiogroup"
                            accessibilityLabel={t("판매자 유형", "Creator type")}
                        >
                            {[
                                ["PERSONAL", "개인", "Individual"],
                                ["BUSINESS", "기업", "Business"],
                            ].map(([value, ko, en]) => (
                                <Choice
                                    key={value}
                                    label={t(ko!, en!)}
                                    selected={type === value}
                                    disabled={pending}
                                    onPress={() => setType(value!)}
                                />
                            ))}
                        </View>
                    </View>
                    <View style={[styles.actionRow, { paddingTop: 20, borderTopWidth: 1, borderColor: colors.line }]}>
                        <Text style={[styles.muted, { flex: 1, minWidth: 160 }]}>
                            {pending
                                ? t(
                                      "신청을 검토하고 있어요. 승인 후 영상을 등록할 수 있습니다.",
                                      "Your application is in review. You can publish once approved.",
                                  )
                                : t(
                                      "신청이 승인되면 영상 판매를 시작할 수 있어요.",
                                      "Start selling videos once your application is approved.",
                                  )}
                        </Text>
                        <Button
                            loading={busy}
                            label={pending ? t("검토 중", "In review") : t("판매자 신청", "Apply to sell")}
                            icon={pending ? "time-outline" : "arrow-forward-outline"}
                            disabled={pending || busy || !displayName.trim() || !bio.trim()}
                            onPress={() =>
                                run(async () => {
                                    await request("/seller/apply", token, "POST", {
                                        displayName: displayName.trim(),
                                        bio: bio.trim(),
                                        type,
                                    });
                                    await refresh();
                                })
                            }
                        />
                    </View>
                </View>
            </View>
        );
    }
    const validSales = sales.filter((line) => !line.refunded);
    return (
        <View style={styles.page}>
            <View style={styles.between}>
                <View>
                    <Text style={styles.title}>{t("크리에이터 스튜디오", "Creator studio")}</Text>
                    <Text style={styles.subtitle}>
                        {t("당신의 지식과 이야기가 새로운 가치를 만듭니다.", "Your ideas deserve an audience.")}
                    </Text>
                </View>
                <Button
                    label={t("새 영상 / 패키지", "New video / bundle")}
                    onPress={() => {
                        setEditing(null);
                        setShowEditor(true);
                    }}
                />
            </View>
            <View style={styles.row}>
                {[
                    [t("판매 건수", "Sales"), String(validSales.length)],
                    [t("판매금액", "Revenue"), money(validSales.reduce((sum, line) => sum + line.priceWon, 0))],
                    [t("정산 예정", "Pending payout"), money(settlement?.pendingWon ?? 0)],
                ].map(([label, value]) => (
                    <View style={[styles.panel, { flex: 1, minWidth: 170 }]} key={label}>
                        <Text style={styles.muted}>{label}</Text>
                        <Text style={styles.title}>{value}</Text>
                    </View>
                ))}
            </View>
            <Text style={styles.muted}>
                {t(
                    "모의 결제 경로 비용 3% · 잔액에서 플랫폼 15% / 판매자 85%. 실제 계약 요율이 아닙니다.",
                    "Simulated 3% channel fee, then 15% platform / 85% seller. Not contractual rates.",
                )}
            </Text>
            <View style={styles.panel}>
                <View style={styles.between}>
                    <CreatorIdentity name={user.displayName} subtitle={t("채널 프로필", "Channel profile")} />
                    <Button
                        secondary
                        compact
                        label={showProfile ? t("닫기", "Close") : t("프로필 편집", "Edit profile")}
                        onPress={() => setShowProfile(!showProfile)}
                    />
                </View>
                {showProfile ? (
                    <View style={{ gap: 16, maxWidth: 720, width: "100%" }}>
                        <Field
                            label={t("채널 이름", "Channel name")}
                            value={displayName}
                            onChangeText={setDisplayName}
                        />
                        <Field label={t("소개", "About")} value={bio} onChangeText={setBio} multiline />
                        <Button
                            secondary
                            label={t("프로필 저장", "Save profile")}
                            onPress={() =>
                                run(async () => {
                                    await request("/seller/profile", token, "PUT", { displayName, bio, type });
                                    await refresh();
                                    notify(t("프로필을 저장했어요.", "Profile saved."));
                                })
                            }
                        />
                    </View>
                ) : null}
            </View>
            {showEditor ? (
                <ProductEditor
                    key={editing?.id ?? "new"}
                    product={editing}
                    products={products}
                    onCancel={() => setShowEditor(false)}
                    onDone={async () => {
                        setShowEditor(false);
                        await reload();
                        await refresh();
                    }}
                />
            ) : null}
            <Text style={styles.heading}>{t("내 콘텐츠", "Your content")}</Text>
            {products.map((product) => (
                <View key={product.id} style={[styles.panel, styles.between]}>
                    <View style={{ flex: 1, gap: 6 }}>
                        <Text style={styles.heading}>{product.title}</Text>
                        <Text style={styles.muted}>
                            {statusLabel(product.kind, language)} · {money(product.priceWon)} ·{" "}
                            {product.termDays || "∞"}
                            {t("일", " days")}
                        </Text>
                    </View>
                    <Text style={styles.badge}>{statusLabel(product.status, language)}</Text>
                    <Button
                        secondary
                        label={t("수정", "Edit")}
                        onPress={() => {
                            setEditing(product);
                            setShowEditor(true);
                        }}
                    />
                    <Button
                        secondary
                        label={t("검수 요청", "Submit review")}
                        onPress={() =>
                            run(async () => {
                                await request(`/seller/products/${product.id}/SUBMIT`, token, "POST");
                                await reload();
                            })
                        }
                    />
                    <Button
                        secondary
                        label={t("판매 중단", "Withdraw")}
                        onPress={() =>
                            run(async () => {
                                await request(`/seller/products/${product.id}/WITHDRAW`, token, "POST");
                                await reload();
                                await refresh();
                            })
                        }
                    />
                    {product.kind === "VIDEO" && product.durationSeconds > 0 ? (
                        <InspectionPlayer productId={product.id} />
                    ) : null}
                </View>
            ))}
            <Text style={styles.heading}>{t("판매 상세", "Sales ledger")}</Text>
            <Text style={styles.muted}>
                {t("정산 후 환불 차감 예정", "Pending refund deduction")}: {money(settlement?.adjustmentWon ?? 0)} ·{" "}
                {t("모의 지급 완료", "Mock paid")}: {money(settlement?.paidWon ?? 0)}
            </Text>
            {settlement?.settlements.map((item) => (
                <Text key={item.id} style={styles.muted}>
                    {new Date(item.createdAt).toLocaleDateString()} · {money(item.amountWon)} · {item.id.slice(0, 8)}
                </Text>
            ))}
            {sales.map((line) => (
                <View key={line.id} style={[styles.panel, styles.between]}>
                    <Text style={styles.text}>{line.title}</Text>
                    <Text style={styles.muted}>
                        {money(line.priceWon)} − {money(line.channelFeeWon)} − {money(line.platformFeeWon)} ={" "}
                        {money(line.sellerAmountWon)} {line.refunded ? "(REFUNDED)" : ""}
                    </Text>
                </View>
            ))}
        </View>
    );
}

function ProductEditor({
    product,
    products,
    onDone,
    onCancel,
}: {
    product: IProduct | null;
    products: IProduct[];
    onDone: () => Promise<void>;
    onCancel: () => void;
}) {
    const { token, t, language, run, busy, notify } = useStore();
    const [draft] = useState(() => new ProductDraft(product?.id ?? null));
    const [hasPersistedProduct, setHasPersistedProduct] = useState(product !== null);
    const [title, setTitle] = useState(product?.title ?? "");
    const [description, setDescription] = useState(product?.description ?? "");
    const [tags, setTags] = useState(product?.tags ?? "");
    const [price, setPrice] = useState(String(product?.priceWon ?? 3000));
    const [term, setTerm] = useState(product?.termDays ?? 30);
    const [category, setCategory] = useState(product?.category ?? "EDUCATION");
    const [kind, setKind] = useState(product?.kind ?? "VIDEO");
    const [ids, setIds] = useState<ProductId[]>(product?.kind === "BUNDLE" ? product.videoIds : []);
    const [rights, setRights] = useState(false);
    const [file, setFile] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
    const [thumbnail, setThumbnail] = useState<DocumentPicker.DocumentPickerAsset | null>(null);
    const [preview, setPreview] = useState("5");
    async function pickVideo() {
        const result = await DocumentPicker.getDocumentAsync({ type: "video/mp4", copyToCacheDirectory: true });
        if (!result.canceled) {
            setFile(result.assets[0] ?? null);
        }
    }
    async function pickThumbnail() {
        const result = await DocumentPicker.getDocumentAsync({
            type: ["image/jpeg", "image/png"],
            copyToCacheDirectory: true,
        });
        if (!result.canceled) {
            setThumbnail(result.assets[0] ?? null);
        }
    }
    async function save() {
        const payload: IProductInput = {
            title,
            description,
            tags,
            priceWon: createProductPrice(Number(price)),
            termDays: term,
            category,
            kind,
            videoIds: ids,
            hasRights: rights,
            thumbnail: product?.thumbnail ?? "studio",
        };
        const id = await draft.save(payload, token);
        setHasPersistedProduct(true);
        if (thumbnail) {
            const body = new FormData();
            if (Platform.OS === "web") {
                body.append("file", await (await fetch(thumbnail.uri)).blob(), thumbnail.name);
            } else {
                body.append("file", {
                    uri: thumbnail.uri,
                    name: thumbnail.name,
                    type: thumbnail.mimeType ?? "image/png",
                } as unknown as Blob);
            }
            const response = await fetch(`${API_URL}/api/seller/products/${id}/thumbnail`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
                body,
            });
            await requireSuccessfulResponse(response);
        }
        if (file && kind === "VIDEO") {
            const body = new FormData();
            if (Platform.OS === "web") {
                const blob = await (await fetch(file.uri)).blob();
                body.append("file", blob, file.name);
            } else {
                body.append("file", { uri: file.uri, name: file.name, type: "video/mp4" } as unknown as Blob);
            }
            body.append("previewSeconds", preview);
            const response = await fetch(`${API_URL}/api/seller/products/${id}/upload`, {
                method: "POST",
                headers: { Authorization: `Bearer ${token}` },
                body,
            });
            await requireSuccessfulResponse(response);
        }
        notify(t("저장했어요. 영상 업로드 시 검수 대기로 전환됩니다.", "Saved. Uploaded videos await review."));
        await onDone();
    }
    return (
        <View style={[styles.panel, { maxWidth: 840, width: "100%" }]}>
            <Text style={styles.heading}>
                {product ? t("상품 수정", "Edit product") : t("새로운 이야기 등록", "Publish a new story")}
            </Text>
            <Field label={t("제목", "Title")} value={title} onChangeText={setTitle} />
            <Field label={t("설명", "Description")} value={description} onChangeText={setDescription} multiline />
            <Button
                secondary
                label={thumbnail?.name ?? t("썸네일 선택 (JPG/PNG, 5MB 이하)", "Choose thumbnail (JPG/PNG, max 5MB)")}
                onPress={() => run(pickThumbnail)}
            />
            <Field
                label={t("태그 (선택, 쉼표로 구분)", "Tags (optional, comma separated)")}
                value={tags}
                onChangeText={setTags}
            />
            <View style={styles.row}>
                {Object.values(ECategory).map((value) => (
                    <Choice
                        key={value}
                        label={categoryLabel(value, language)}
                        selected={category === value}
                        onPress={() => setCategory(value)}
                    />
                ))}
            </View>
            <Field label={t("가격 (원)", "Price (KRW)")} value={price} onChangeText={setPrice} keyboardType="numeric" />
            <View style={styles.row}>
                {([0, 7, 30, 90] as const).map((value) => (
                    <Button
                        key={value}
                        label={value ? `${value}${t("일", " days")}` : t("기간 제한 없음", "Unlimited")}
                        secondary={term !== value}
                        onPress={() => setTerm(value)}
                    />
                ))}
            </View>
            {!hasPersistedProduct ? (
                <View style={styles.row}>
                    {Object.values(EProductKind).map((value) => (
                        <Choice
                            key={value}
                            label={statusLabel(value, language)}
                            selected={kind === value}
                            onPress={() => setKind(value)}
                        />
                    ))}
                </View>
            ) : null}
            {kind === "BUNDLE" ? (
                <View style={{ gap: 8 }}>
                    {products
                        .filter((item) => item.kind === "VIDEO" && item.status === "APPROVED")
                        .map((item) => (
                            <Button
                                key={item.id}
                                secondary={!ids.includes(item.id)}
                                disabled={Boolean(product)}
                                label={item.title}
                                onPress={() =>
                                    setIds(
                                        ids.includes(item.id) ? ids.filter((id) => id !== item.id) : [...ids, item.id],
                                    )
                                }
                            />
                        ))}
                </View>
            ) : (
                <>
                    <Text style={styles.muted}>
                        {t(
                            "MP4 H.264/AAC · 1080p · 최대 100MB / 10분",
                            "MP4 H.264/AAC · 1080p · Max 100MB / 10 minutes",
                        )}
                    </Text>
                    <Button
                        secondary
                        label={file?.name ?? t("영상 파일 선택", "Choose video")}
                        onPress={() => run(pickVideo)}
                    />
                    <Field
                        label={t("미리보기 길이 (초, 전체의 20% 이내)", "Preview seconds (up to 20%)")}
                        value={preview}
                        onChangeText={setPreview}
                        keyboardType="numeric"
                    />
                </>
            )}
            <Button
                secondary
                label={`${rights ? "✓ " : ""}${t("이 영상의 판매 권리를 보유하고 있습니다.", "I own the rights to sell this content.")}`}
                onPress={() => setRights(!rights)}
            />
            <View style={[styles.actionRow, { paddingTop: 16, borderTopWidth: 1, borderColor: colors.line }]}>
                <Button secondary label={t("취소", "Cancel")} disabled={busy} onPress={onCancel} />
                <Button
                    label={t("저장", "Save")}
                    disabled={!rights || busy || !title.trim() || !description.trim()}
                    onPress={() => run(save)}
                />
            </View>
        </View>
    );
}
