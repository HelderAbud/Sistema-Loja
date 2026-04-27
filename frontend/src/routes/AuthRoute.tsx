import { FormEvent } from "react";
import { Navigate } from "react-router-dom";
import { LoginPage } from "../pages";

type AuthMode = "login" | "register";

type Props = {
  authed: boolean;
  email: string;
  password: string;
  authMode: AuthMode;
  error: string | null;
  busy: boolean;
  onEmailChange: (v: string) => void;
  onPasswordChange: (v: string) => void;
  onAuthModeChange: (m: AuthMode) => void;
  onSubmit: (e: FormEvent) => void;
};

export function AuthRoute({
  authed,
  email,
  password,
  authMode,
  error,
  busy,
  onEmailChange,
  onPasswordChange,
  onAuthModeChange,
  onSubmit,
}: Props) {
  if (authed) {
    return <Navigate to="/piloto/products" replace />;
  }
  return (
    <div className="app-backdrop">
      <LoginPage
        email={email}
        password={password}
        authMode={authMode}
        error={error}
        busy={busy}
        onEmailChange={onEmailChange}
        onPasswordChange={onPasswordChange}
        onAuthModeChange={onAuthModeChange}
        onSubmit={onSubmit}
      />
    </div>
  );
}
