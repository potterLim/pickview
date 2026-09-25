import { StoreProvider, useStore } from "./src/core/Store";
import { AppShell } from "./src/ui/AppShell";
import { DiscoverScreen } from "./src/screens/DiscoverScreen";
import { AuthScreen } from "./src/screens/AuthScreen";
import { DetailScreen } from "./src/screens/DetailScreen";
import { CartScreen, LibraryScreen, OrdersScreen } from "./src/screens/PurchaseScreens";
import { InboxScreen, SellerScreen, SettingsScreen } from "./src/screens/AccountScreens";
import { StudioScreen } from "./src/screens/StudioScreen";
import { AdminScreen } from "./src/screens/AdminScreen";

function CurrentScreen() {
    const { route } = useStore();
    switch (route) {
        case "login": return <AuthScreen />;
        case "detail": return <DetailScreen />;
        case "seller": return <SellerScreen />;
        case "cart": return <CartScreen />;
        case "library": return <LibraryScreen />;
        case "orders": return <OrdersScreen />;
        case "inbox": return <InboxScreen />;
        case "settings": return <SettingsScreen />;
        case "studio": return <StudioScreen />;
        case "admin": return <AdminScreen />;
        default: return <DiscoverScreen />;
    }
}

export default function App() {
    return <StoreProvider><AppShell><CurrentScreen /></AppShell></StoreProvider>;
}
