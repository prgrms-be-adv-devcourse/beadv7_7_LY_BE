import { createBrowserRouter } from "react-router";
import { Layout } from "./Layout";
import { HomePage } from "../pages/HomePage";
import { CatalogPage } from "../pages/CatalogPage";
import { ProductDetailPage } from "../pages/ProductDetailPage";
import { FeedPage } from "../pages/FeedPage";
import { AuctionDetailPage } from "../pages/AuctionDetailPage";
import { LoginPage } from "../pages/LoginPage";
import { SignupPage } from "../pages/SignupPage";

export const router = createBrowserRouter([
    {
        path: "/",
        element: <Layout />,
        children: [
            { index: true, element: <HomePage /> },
            { path: "catalog", element: <CatalogPage /> },
            { path: "products/:productId", element: <ProductDetailPage /> },
            { path: "feed", element: <FeedPage /> },
            { path: "auctions/:auctionId", element: <AuctionDetailPage /> },
            { path: "login", element: <LoginPage /> },
            { path: "signup", element: <SignupPage /> },
        ],
    },
]);
