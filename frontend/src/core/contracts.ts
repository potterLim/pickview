import { decodeWonAmount, decodeAccessTerm } from "./commerceValues";
import { decodeProductId, decodeAccountId } from "./identifiers";
import {
    ERole,
    ESellerStatus,
    EProductKind,
    ECategory,
    EProductStatus,
    EActivityKind,
    ETicketKind,
    ETicketStatus,
    EPaymentChannel,
    EOrderStatus,
} from "./domain";
import { decodeChoice } from "./validation";
import { decodeObject, decodeString, decodeNumber, decodeBoolean, decodeArray } from "./validation";
import type {
    IUser,
    IProduct,
    IActivity,
    ILibraryItem,
    IOrderLine,
    IOrder,
    ITicket,
    INotice,
    IAudit,
    IRefundAdjustment,
    ISettlementSummary,
    IDashboard,
} from "./types";

export function decodeUser(value: unknown): IUser {
    const item = decodeObject(value);
    return {
        id: decodeAccountId(item.id),
        email: decodeString(item.email),
        displayName: decodeString(item.displayName),
        role: decodeChoice(Object.values(ERole), item.role),
        sellerStatus: decodeChoice(Object.values(ESellerStatus), item.sellerStatus),
        bio: decodeString(item.bio),
        language: decodeString(item.language),
        interests: decodeString(item.interests),
    };
}

export function decodeProduct(value: unknown): IProduct {
    const item = decodeObject(value);
    return {
        id: decodeProductId(item.id),
        sellerId: decodeAccountId(item.sellerId),
        sellerName: decodeString(item.sellerName),
        title: decodeString(item.title),
        description: decodeString(item.description),
        category: decodeChoice(Object.values(ECategory), item.category),
        priceWon: decodeWonAmount(item.priceWon),
        termDays: decodeAccessTerm(item.termDays),
        status: decodeChoice(Object.values(EProductStatus), item.status),
        thumbnail: decodeString(item.thumbnail),
        durationSeconds: decodeNumber(item.durationSeconds),
        kind: decodeChoice(Object.values(EProductKind), item.kind),
        videoIds: decodeArray(decodeProductId)(item.videoIds),
        rating: decodeNumber(item.rating),
        reviewCount: decodeNumber(item.reviewCount),
        sales: decodeNumber(item.sales),
        createdAt: decodeNumber(item.createdAt),
        blocked: decodeBoolean(item.blocked),
        tags: decodeString(item.tags),
        isDemo: decodeBoolean(item.isDemo),
    };
}

export function decodeActivity(value: unknown): IActivity {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        targetId: decodeString(item.targetId),
        kind: decodeChoice(Object.values(EActivityKind), item.kind),
        content: decodeString(item.content),
        numberValue: decodeNumber(item.numberValue),
        author: decodeString(item.author),
    };
}

export function decodeLibraryItem(value: unknown): ILibraryItem {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        product: decodeProduct(item.product),
        expiresAt: decodeNumber(item.expiresAt),
        active: decodeBoolean(item.active),
    };
}

export function decodeOrderLine(value: unknown): IOrderLine {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        productId: decodeProductId(item.productId),
        title: decodeString(item.title),
        priceWon: decodeWonAmount(item.priceWon),
        channelFeeWon: decodeWonAmount(item.channelFeeWon),
        platformFeeWon: decodeWonAmount(item.platformFeeWon),
        sellerAmountWon: decodeWonAmount(item.sellerAmountWon),
        termDays: decodeAccessTerm(item.termDays),
        refunded: decodeBoolean(item.refunded),
        settlementId: decodeString(item.settlementId),
    };
}

export function decodeOrder(value: unknown): IOrder {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        status: decodeChoice(Object.values(EOrderStatus), item.status),
        channel: decodeChoice(Object.values(EPaymentChannel), item.channel),
        createdAt: decodeNumber(item.createdAt),
        lines: decodeArray(decodeOrderLine)(item.lines),
        totalWon: decodeWonAmount(item.totalWon),
    };
}

export function decodeTicket(value: unknown): ITicket {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        userId: decodeString(item.userId),
        targetId: decodeString(item.targetId),
        recipientId: decodeString(item.recipientId),
        kind: decodeChoice(Object.values(ETicketKind), item.kind),
        message: decodeString(item.message),
        status: decodeChoice(Object.values(ETicketStatus), item.status),
        reply: decodeString(item.reply),
        createdAt: decodeNumber(item.createdAt),
    };
}

export function decodeNotice(value: unknown): INotice {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        message: decodeString(item.message),
        read: decodeBoolean(item.read),
        createdAt: decodeNumber(item.createdAt),
    };
}

export function decodeAudit(value: unknown): IAudit {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        actorId: decodeString(item.actorId),
        action: decodeString(item.action),
        targetId: decodeString(item.targetId),
        detail: decodeString(item.detail),
        createdAt: decodeNumber(item.createdAt),
    };
}

export function decodeRefundAdjustment(value: unknown): IRefundAdjustment {
    const item = decodeObject(value);
    return {
        lineId: decodeString(item.lineId),
        sellerId: decodeString(item.sellerId),
        amountWon: decodeNumber(item.amountWon),
        settlementId: decodeString(item.settlementId),
    };
}

export function decodeSettlementSummary(value: unknown): ISettlementSummary {
    const item = decodeObject(value);
    return {
        pendingWon: decodeNumber(item.pendingWon),
        adjustmentWon: decodeNumber(item.adjustmentWon),
        paidWon: decodeNumber(item.paidWon),
        settlements: decodeArray(decodeSettlement)(item.settlements),
    };
}

export function decodeDashboard(value: unknown): IDashboard {
    const item = decodeObject(value);
    return {
        accounts: decodeArray(decodeUser)(item.accounts),
        products: decodeArray(decodeProduct)(item.products),
        tickets: decodeArray(decodeTicket)(item.tickets),
        lines: decodeArray(decodeOrderLine)(item.lines),
        audits: decodeArray(decodeAudit)(item.audits),
        adjustments: decodeArray(decodeRefundAdjustment)(item.adjustments),
    };
}

export function decodeToken(value: unknown): { token: string } {
    return { token: decodeString(decodeObject(value).token) };
}

export function decodePath(value: unknown): { path: string } {
    return { path: decodeString(decodeObject(value).path) };
}

export function decodeSeller(value: unknown): { bio: string } {
    return { bio: decodeString(decodeObject(value).bio) };
}

function decodeSettlement(value: unknown): ISettlementSummary["settlements"][number] {
    const item = decodeObject(value);
    return {
        id: decodeString(item.id),
        amountWon: decodeNumber(item.amountWon),
        createdAt: decodeNumber(item.createdAt),
    };
}
