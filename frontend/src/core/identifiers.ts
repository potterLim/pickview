declare const identifierKind: unique symbol;

export type ProductId = string & { readonly [identifierKind]: "product" };
export type AccountId = string & { readonly [identifierKind]: "account" };

function decodeIdentifier(value: unknown): string {
    if (typeof value !== "string" || value.trim().length === 0 || value.length > 64) {
        throw new Error("Invalid identifier");
    }
    return value;
}

// Branding follows runtime validation and prevents account/product argument swaps.
export function decodeProductId(value: unknown): ProductId {
    return decodeIdentifier(value) as ProductId;
}

export function decodeAccountId(value: unknown): AccountId {
    return decodeIdentifier(value) as AccountId;
}
