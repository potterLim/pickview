export interface IUser {
    id: string; email: string; displayName: string; role: string; sellerStatus: string;
    bio: string; language: string; interests: string;
}
export interface IProduct {
    id: string; sellerId: string; sellerName: string; title: string; description: string;
    category: string; priceWon: number; termDays: number; status: string; thumbnail: string;
    durationSeconds: number; kind: string; videoIds: string[]; rating: number; reviewCount: number;
    sales: number; createdAt: number; blocked: boolean; tags: string; isDemo: boolean;
}
export interface IActivity { id: string; targetId: string; kind: string; content: string; numberValue: number; author: string }
export interface ILibraryItem { id: string; product: IProduct; expiresAt: number; active: boolean }
export interface IOrderLine {
    id: string; productId: string; title: string; priceWon: number; channelFeeWon: number;
    platformFeeWon: number; sellerAmountWon: number; termDays: number; refunded: boolean; settlementId: string;
}
export interface IOrder { id: string; status: string; channel: string; createdAt: number; lines: IOrderLine[]; totalWon: number }
export interface ITicket { id: string; userId: string; targetId: string; recipientId: string; kind: string; message: string; status: string; reply: string; createdAt: number }
export interface INotice { id: string; message: string; read: boolean; createdAt: number }
export interface IAudit { id: string; actorId: string; action: string; targetId: string; detail: string; createdAt: number }
export interface IRefundAdjustment { lineId: string; sellerId: string; amountWon: number; settlementId: string }
export interface ISettlementSummary { pendingWon: number; adjustmentWon: number; paidWon: number; settlements: { id: string; amountWon: number; createdAt: number }[] }
export interface IDashboard { accounts: IUser[]; products: IProduct[]; tickets: ITicket[]; lines: IOrderLine[]; audits: IAudit[]; adjustments: IRefundAdjustment[] }
export type Language = "ko" | "en";
export type Route = "discover" | "detail" | "seller" | "library" | "wishlist" | "following" | "cart" | "orders" | "inbox" | "settings" | "studio" | "admin" | "login";
