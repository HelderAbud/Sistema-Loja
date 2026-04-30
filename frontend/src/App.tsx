import { useEffect, useState } from "react";
import { toast } from "sonner";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { useAuthSession, useLoginForm } from "@/features/auth";
import { PilotoWorkspacePage } from "./pages";
import {
  CartPage,
  CatalogPage,
  HomePage,
  LandingPage,
  OrdersPage,
  PitchPage,
  ProductPage,
  SellerAreaPage,
} from "./pages/StorefrontPages";
import { RouteDocumentHead } from "./routeDocumentMeta";
import { AuthRoute } from "./routes/AuthRoute";
import { ProtectedLayout } from "./routes/ProtectedLayout";

function SessionBackdrop() {
  return (
    <div className="app-backdrop" aria-busy="true" aria-live="polite">
      <p style={{ margin: 0, opacity: 0.85 }}>A carregar sessão…</p>
    </div>
  );
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
        <Route path="/" element={<LandingPage />} />
        <Route path="/home" element={<HomePage />} />
        <Route path="/catalog" element={<CatalogPage />} />
        <Route path="/orders" element={<OrdersPage />} />
        <Route path="/product/:slug" element={<ProductPage />} />
        <Route path="/cart" element={<CartPage />} />
        <Route path="/seller" element={<SellerAreaPage />} />
        <Route path="/pitch" element={<PitchPage />} />
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
            element={<PilotoWorkspacePage email={email} error={error} onLogout={logout} />}
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
