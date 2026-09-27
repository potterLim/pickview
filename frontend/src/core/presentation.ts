import catalog from "../../../content/demo-catalog.json";
import type { IProduct, Language } from "./types";

export function getDemoContentOrNull(product: IProduct): (typeof catalog)[number] | null {
    return product.isDemo ? (catalog.find((item) => item.id === product.id) ?? null) : null;
}

export function productTitle(product: IProduct, language: Language): string {
    return language === "en" ? (getDemoContentOrNull(product)?.titleEn ?? product.title) : product.title;
}

export function categoryLabel(category: string, language: Language): string {
    const labels: Record<string, [string, string]> = {
        EDUCATION: ["교육·강의", "Learning"],
        FINANCE: ["투자·경제", "Finance"],
        COMEDY: ["코미디", "Comedy"],
    };
    return labels[category]?.[language === "ko" ? 0 : 1] ?? category;
}

export function statusLabel(status: string, language: Language): string {
    const labels: Record<string, [string, string]> = {
        DRAFT: ["작성 중", "Draft"],
        PENDING: ["검토 중", "In review"],
        APPROVED: ["승인 완료", "Approved"],
        REJECTED: ["보완 필요", "Needs changes"],
        WITHDRAWN: ["판매 중단", "Withdrawn"],
        NONE: ["신청 전", "Not applied"],
        OPEN: ["답변 대기", "Awaiting reply"],
        RESOLVED: ["답변 완료", "Answered"],
        SUCCESS: ["구매 완료", "Purchased"],
        FAILED: ["결제 실패", "Payment failed"],
        CANCELED: ["결제 취소", "Canceled"],
        REFUNDED: ["환불 완료", "Refunded"],
        VIDEO: ["단품 영상", "Single video"],
        BUNDLE: ["영상 패키지", "Video bundle"],
        INQUIRY: ["판매자 문의", "Creator inquiry"],
        SUPPORT: ["고객지원", "Support"],
        REPORT: ["콘텐츠 신고", "Content report"],
        REFUND: ["환불 요청", "Refund request"],
    };
    return labels[status]?.[language === "ko" ? 0 : 1] ?? status;
}

export function durationLabel(seconds: number): string {
    return `${Math.floor(seconds / 60).toString().padStart(2, "0")}:${Math.floor(seconds % 60).toString().padStart(2, "0")}`;
}
