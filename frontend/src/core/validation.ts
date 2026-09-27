export type Decoder<T> = (value: unknown) => T;

export function decodeChoice<T extends string>(choices: readonly T[], value: unknown): T {
    for (const choice of choices) {
        if (value === choice) {
            return choice;
        }
    }
    throw new Error("Invalid API choice");
}

export function decodeObject(value: unknown): Record<string, unknown> {
    if (typeof value !== "object" || value === null || Array.isArray(value)) {
        throw new Error("Invalid API object");
    }
    return Object.fromEntries(Object.entries(value));
}

export function decodeString(value: unknown): string {
    if (typeof value !== "string") {
        throw new Error("Invalid API text");
    }
    return value;
}

export function decodeNumber(value: unknown): number {
    if (typeof value !== "number" || !Number.isFinite(value)) {
        throw new Error("Invalid API number");
    }
    return value;
}

export function decodeBoolean(value: unknown): boolean {
    if (typeof value !== "boolean") {
        throw new Error("Invalid API boolean");
    }
    return value;
}

export function decodeArray<T>(decoder: Decoder<T>): Decoder<T[]> {
    return (value: unknown): T[] => {
        if (!Array.isArray(value)) {
            throw new Error("Invalid API array");
        }
        return value.map((item: unknown) => decoder(item));
    };
}

export function getErrorMessage(error: unknown): string {
    return error instanceof Error ? error.message : "요청을 처리하지 못했습니다. / Request failed.";
}
