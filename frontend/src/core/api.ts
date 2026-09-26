import { Platform } from "react-native";

export const API_URL = process.env.EXPO_PUBLIC_API_URL ?? (Platform.OS === "android" ? "http://10.0.2.2:8080" : "http://localhost:8080");

export class ApiError extends Error {
    private readonly mStatus: number;

    constructor(message: string, status: number) {
        super(message);
        this.mStatus = status;
    }

    get status(): number { return this.mStatus; }
}

export async function request<T>(path: string, token: string, method = "GET", body?: unknown, signal?: AbortSignal): Promise<T> {
    const response = await fetch(`${API_URL}/api${path}`, {
        method, signal,
        headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: `HTTP ${response.status}` })) as { message?: string };
        throw new ApiError(error.message ?? `HTTP ${response.status}`, response.status);
    }
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
}
