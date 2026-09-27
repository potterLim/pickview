import { Platform } from "react-native";
import type { Decoder } from "./validation";

export const API_URL =
    process.env.EXPO_PUBLIC_API_URL ?? (Platform.OS === "android" ? "http://10.0.2.2:8080" : "http://localhost:8080");

export class ApiError extends Error {
    private readonly mStatus: number;

    constructor(message: string, status: number) {
        super(message);
        this.mStatus = status;
    }

    get status(): number {
        return this.mStatus;
    }
}

export async function request(
    path: string,
    token: string,
    method = "GET",
    body?: unknown,
    signal?: AbortSignal,
): Promise<void> {
    await send(path, token, method, body, signal);
}

export async function read<T>(
    decode: Decoder<T>,
    path: string,
    token: string,
    method = "GET",
    body?: unknown,
    signal?: AbortSignal,
): Promise<T> {
    const response = await send(path, token, method, body, signal);
    const value: unknown = await response.json();
    return decode(value);
}

export async function requireSuccessfulResponse(response: Response): Promise<void> {
    if (response.ok) {
        return;
    }
    const error: unknown = await response.json().catch(() => null);
    const message = typeof error === "object" && error !== null && "message" in error
        && typeof error.message === "string" ? error.message : `HTTP ${response.status}`;
    throw new ApiError(message, response.status);
}

async function send(
    path: string,
    token: string,
    method: string,
    body: unknown,
    signal: AbortSignal | undefined,
): Promise<Response> {
    const response = await fetch(`${API_URL}/api${path}`, {
        method,
        signal,
        headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    await requireSuccessfulResponse(response);
    return response;
}
