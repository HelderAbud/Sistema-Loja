import { lazy, Suspense, useEffect, useState, type ReactNode } from "react";
import { toast } from "sonner";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { useAuthSession, useLoginForm } from "@/features/auth";
import { RouteDocumentHead } from "./routeDocumentMeta";
import { AuthRoute } from "./routes/AuthRoute";
import { ProtectedLayout } from "./routes/ProtectedLayout";

const PilotoWorkspacePage = lazy(() =>
  import("./pages").then((m) => ({ default: m.PilotoWorkspacePage })),
);

const LandingPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.LandingPage })),
);
const HomePage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.HomePage })),
);
const CatalogPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.CatalogPage })),
);
const OrdersPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.OrdersPage })),
);
const ProductPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.ProductPage })),
);
const CartPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.CartPage })),
);
const SellerAreaPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.SellerAreaPage })),
);
const PitchPage = lazy(() =>
  import("./pages/StorefrontPages").then((m) => ({ default: m.PitchPage })),
);

function SessionBackdrop() {
  return (
    <div className="app-backdrop" aria-busy="true" aria-live="polite">
      <p style={{ margin: 0, opacity: 0.85 }}>A carregar sessão…</p>
    </div>
  );
}

function RouteFallback() {
  return (
    <div className="app-backdrop" aria-busy="true" aria-live="polite">
      <p style={{ margin: 0, opacity: 0.85 }}>A carregar página…</p>
    </div>
  );
}

function LazyRoute({ children }: { children: ReactNode }) {
  return <Suspense fallback={<RouteFallback />}>{children}</Suspense>;
}

function AppRoutes() {
  const { isAuthenticated, bootstrapSession, logout: endSession } = useAuthSession();
  const {
    email,
    password,
    authMode,
    error,
    busy,
    setEmail,
    setPassword,
    setAuthMode,
    setError,
    onSubmit,
  } = useLoginForm();
  const [sessionChecked, setSessionChecked] = useState(false);

  useEffect(() => {
    let cancelled = false;
    void bootstrapSession().finally(() => {
      if (!cancelled) setSessionChecked(true);
    });
    return () => {
      cancelled = true;
    };
  }, [bootstrapSession]);

  if (!sessionChecked) {
    return (
      <>
        <RouteDocumentHead />
        <SessionBackdrop />
      </>
    );
  }

  async function logout() {
    await endSession();
    setError(null);
    toast.message("Sessão terminada");
  }

  return (
    <>
      <RouteDocumentHead />
      <Routes>
        <Route
          path="/"
          element={
            <LazyRoute>
              <LandingPage />
            </LazyRoute>
          }
        />
        <Route
          path="/home"
          element={
            <LazyRoute>
              <HomePage />
            </LazyRoute>
          }
        />
        <Route
          path="/catalog"
          element={
            <LazyRoute>
              <CatalogPage />
            </LazyRoute>
          }
        />
        <Route
          path="/orders"
          element={
            <LazyRoute>
              <OrdersPage />
            </LazyRoute>
          }
        />
        <Route
          path="/product/:slug"
          element={
            <LazyRoute>
              <ProductPage />
            </LazyRoute>
          }
        />
        <Route
          path="/cart"
          element={
            <LazyRoute>
              <CartPage />
            </LazyRoute>
          }
        />
        <Route
          path="/seller"
          element={
            <LazyRoute>
              <SellerAreaPage />
            </LazyRoute>
          }
        />
        <Route
          path="/pitch"
          element={
            <LazyRoute>
              <PitchPage />
            </LazyRoute>
          }
        />
        <Route
          path="/login"
          element={
            <AuthRoute
              authed={isAuthenticated}
              email={email}
              password={password}
              authMode={authMode}
              error={error}
              busy={busy}
              onEmailChange={setEmail}
              onPasswordChange={setPassword}
              onAuthModeChange={setAuthMode}
              onSubmit={onSubmit}
            />
          }
        />
        <Route element={<ProtectedLayout authed={isAuthenticated} />}>
          <Route
            path="/piloto/:tab"
            element={
              <LazyRoute>
                <PilotoWorkspacePage email={email} error={error} onLogout={logout} />
              </LazyRoute>
            }
          />
          <Route path="/piloto" element={<Navigate to="/piloto/products" replace />} />
        </Route>
        <Route
          path="/app"
          element={<Navigate to={isAuthenticated ? "/piloto/products" : "/login"} replace />}
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AppRoutes />
    </BrowserRouter>
  );
}
