import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { ApiError, request } from "./api";
import type { IActivity, ILibraryItem, IProduct, IUser, Language, Route } from "./types";

interface IStore {
    token: string; user: IUser | null; language: Language; route: Route; products: IProduct[];
    activity: IActivity[]; library: ILibraryItem[]; selected: IProduct | null; busy: boolean; message: string;
    t: (ko: string, en: string) => string;
    navigate: (route: Route, product?: IProduct) => void;
    setLanguage: (language: Language) => void;
    refresh: () => Promise<void>;
    signIn: (token: string) => Promise<void>;
    signOut: () => Promise<void>;
    run: (action: () => Promise<void>) => Promise<void>;
    notify: (message: string) => void;
    toggle: (kind: string, targetId: string) => Promise<void>;
    hasActivity: (kind: string, targetId: string) => boolean;
}
const Store = createContext<IStore | null>(null);

export function StoreProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState("");
    const [user, setUser] = useState<IUser | null>(null);
    const [language, setLanguageState] = useState<Language>("ko");
    const [route, setRoute] = useState<Route>("discover");
    const [products, setProducts] = useState<IProduct[]>([]);
    const [activity, setActivity] = useState<IActivity[]>([]);
    const [library, setLibrary] = useState<ILibraryItem[]>([]);
    const [selected, setSelected] = useState<IProduct | null>(null);
    const [busy, setBusy] = useState(false);
    const [message, setMessage] = useState("");
    const refreshController = useRef<AbortController | null>(null);
    const t = (ko: string, en: string) => language === "ko" ? ko : en;

    const refresh = useCallback(async () => {
        refreshController.current?.abort();
        const controller = new AbortController();
        refreshController.current = controller;
        const signal = controller.signal;
        try {
            const catalog = await request<IProduct[]>("/public/products", "", "GET", undefined, signal);
            if (signal.aborted) { return; }
            setProducts(catalog);
            if (!token) { setUser(null); setActivity([]); setLibrary([]); return; }
            const [currentUser, currentActivity, currentLibrary] = await Promise.all([
                request<IUser>("/me", token, "GET", undefined, signal),
                request<IActivity[]>("/activity", token, "GET", undefined, signal),
                request<ILibraryItem[]>("/library", token, "GET", undefined, signal),
            ]);
            if (signal.aborted) { return; }
            setUser(currentUser); setActivity(currentActivity); setLibrary(currentLibrary);
        } catch (error) {
            if (signal.aborted) { return; }
            if (error instanceof ApiError && error.status === 401) {
                await AsyncStorage.removeItem("pickview.token");
                setToken(""); setUser(null); setActivity([]); setLibrary([]); setRoute("login");
            }
            throw error;
        }
    }, [token]);

    useEffect(() => { AsyncStorage.getItem("pickview.token").then(value => setToken(value ?? "")).catch(() => setMessage("Session storage unavailable")); }, []);
    useEffect(() => {
        refresh().catch(error => setMessage(String(error.message)));
        return () => refreshController.current?.abort();
    }, [refresh]);
    useEffect(() => {
        if (!message) { return; }
        const timer = setTimeout(() => setMessage(""), 7000);
        return () => clearTimeout(timer);
    }, [message]);

    function navigate(next: Route, product?: IProduct) {
        if (product) { setSelected(product); }
        const isPublic = ["discover", "detail", "seller", "login"].includes(next);
        setRoute(!token && !isPublic ? "login" : next);
    }
    async function signIn(next: string) {
        refreshController.current?.abort();
        await AsyncStorage.setItem("pickview.token", next);
        setUser(null); setActivity([]); setLibrary([]); setToken(next); setRoute("discover");
    }
    async function signOut() {
        refreshController.current?.abort();
        try { await request("/auth/logout", token, "POST"); }
        finally {
            await AsyncStorage.removeItem("pickview.token");
            setToken(""); setUser(null); setActivity([]); setLibrary([]); setSelected(null); setRoute("discover");
        }
    }
    async function run(action: () => Promise<void>) {
        if (busy) { return; }
        setBusy(true);
        try { await action(); } catch (error) { setMessage(error instanceof Error ? error.message : String(error)); }
        finally { setBusy(false); }
    }
    function hasActivity(kind: string, targetId: string) { return activity.some(item => item.kind === kind && item.targetId === targetId); }
    async function toggle(kind: string, targetId: string) {
        if (!token) { setRoute("login"); return; }
        if (hasActivity(kind, targetId)) { await request(`/activity/${kind}/${targetId}`, token, "DELETE"); }
        else { await request("/activity", token, "PUT", { kind, targetId, content: "", numberValue: 0 }); }
        await refresh();
    }
    const value: IStore = { token, user, language, route, products, activity, library, selected, busy, message, t,
        navigate, setLanguage: setLanguageState, refresh, signIn, signOut, run, notify: setMessage, toggle, hasActivity };
    return <Store.Provider value={value}>{children}</Store.Provider>;
}

export function useStore(): IStore {
    const value = useContext(Store);
    if (!value) { throw new Error("StoreProvider is required"); }
    return value;
}
