import { read, request } from "./api";
import { decodeProduct } from "./contracts";
import type { ProductId } from "./identifiers";
import type { IProduct } from "./types";

export interface IProductInput {
    title: string;
    description: string;
    tags: string;
    priceWon: number;
    termDays: number;
    category: IProduct["category"];
    kind: IProduct["kind"];
    videoIds: ProductId[];
    hasRights: boolean;
    thumbnail: string;
}

export class ProductDraft {
    private mProductId: ProductId | null;

    constructor(productIdOrNull: ProductId | null) {
        this.mProductId = productIdOrNull;
    }

    async save(input: IProductInput, token: string): Promise<ProductId> {
        if (this.mProductId !== null) {
            await request(`/seller/products/${this.mProductId}`, token, "PUT", input);
            return this.mProductId;
        }
        const product = await read(decodeProduct, "/seller/products", token, "POST", input);
        // Keep the committed identity before media uploads, which may fail independently.
        this.mProductId = product.id;
        return product.id;
    }
}
