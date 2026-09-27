declare const wonUnit: unique symbol;
declare const productPricePolicy: unique symbol;

export type WonAmount = number & { readonly [wonUnit]: true };
export type ProductPrice = WonAmount & { readonly [productPricePolicy]: true };
export type AccessTerm = 0 | 7 | 30 | 90;

export function decodeWonAmount(value: unknown): WonAmount {
    if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 0) {
        throw new Error("Invalid KRW amount");
    }
    return value as WonAmount;
}

export function createProductPrice(value: unknown): ProductPrice {
    const amount = decodeWonAmount(value);
    if (amount > 1_000_000 || (amount !== 0 && (amount < 1_000 || amount % 100 !== 0))) {
        throw new Error("가격은 무료 또는 1,000~1,000,000원 사이의 100원 단위입니다. / Invalid product price.");
    }
    return amount as ProductPrice;
}

export function decodeAccessTerm(value: unknown): AccessTerm {
    if (value === 0 || value === 7 || value === 30 || value === 90) {
        return value;
    }
    throw new Error("Invalid access term");
}
