export enum ERole {
    BUYER = "BUYER",
    ADMIN = "ADMIN",
    CONTENT = "CONTENT",
    SUPPORT = "SUPPORT",
    FINANCE = "FINANCE",
}

export enum ESellerStatus {
    NONE = "NONE",
    PENDING = "PENDING",
    APPROVED = "APPROVED",
    REJECTED = "REJECTED",
}

export enum EProductKind {
    VIDEO = "VIDEO",
    BUNDLE = "BUNDLE",
}

export enum ECategory {
    EDUCATION = "EDUCATION",
    FINANCE = "FINANCE",
    COMEDY = "COMEDY",
}

export enum EProductStatus {
    DRAFT = "DRAFT",
    PENDING = "PENDING",
    APPROVED = "APPROVED",
    REJECTED = "REJECTED",
    WITHDRAWN = "WITHDRAWN",
}

export enum EActivityKind {
    WISHLIST = "WISHLIST",
    FOLLOW = "FOLLOW",
    BLOCK = "BLOCK",
    PROGRESS = "PROGRESS",
    REVIEW = "REVIEW",
    CART = "CART",
    NOTIFY = "NOTIFY",
}

export enum ETicketKind {
    INQUIRY = "INQUIRY",
    SUPPORT = "SUPPORT",
    REPORT = "REPORT",
    REFUND = "REFUND",
}

export enum ETicketStatus {
    OPEN = "OPEN",
    ANSWERED = "ANSWERED",
    APPROVED = "APPROVED",
    REJECTED = "REJECTED",
}

export enum EPaymentChannel {
    CARD = "CARD",
    EASY = "EASY",
}

export enum EOrderStatus {
    SUCCESS = "SUCCESS",
    FAILED = "FAILED",
    CANCELED = "CANCELED",
    REFUNDED = "REFUNDED",
}
