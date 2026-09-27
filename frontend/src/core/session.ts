export async function clearLocalSession(resetMemory: () => void, removeStoredToken: () => Promise<void>): Promise<void> {
    // Storage availability must never control whether private UI state is cleared.
    resetMemory();
    await removeStoredToken();
}
