import type { ProductId } from "./identifiers";
import { request } from "./api";

const pendingBySessionAndProduct = new Map<string, Promise<void>>();

export async function savePlaybackProgress(token: string, productId: ProductId, seconds: number): Promise<void> {
    if (!Number.isFinite(seconds) || seconds < 0) {
        throw new Error("Invalid playback position");
    }
    const key = `${token}:${productId}`;
    const previous = pendingBySessionAndProduct.get(key) ?? Promise.resolve();
    // Share the queue across player remounts, so an old write cannot overtake a new one.
    const pending = previous
        .catch(() => undefined)
        .then(() =>
            request("/activity", token, "PUT", {
                targetId: productId,
                kind: "PROGRESS",
                content: "",
                numberValue: seconds,
            }),
        );
    pendingBySessionAndProduct.set(key, pending);
    try {
        await pending;
    } finally {
        if (pendingBySessionAndProduct.get(key) === pending) {
            pendingBySessionAndProduct.delete(key);
        }
    }
}
