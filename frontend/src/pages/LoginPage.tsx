import { FormEvent } from "react";
import { BRAND_LOGIN_LEAD, BRAND_NAME, BRAND_TAGLINE } from "../brand";

type AuthMode = "login" | "register";

type Props = {
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

export function LoginPage({
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
  return (
    <div className="login-shell">
      <div className="login-split">
        <div className="login-visual">
          <h2>{BRAND_TAGLINE}</h2>
          <p className="login-visual-lead">{BRAND_LOGIN_LEAD}</p>
          <ul className="login-feature-list">
            <li>Automação fiscal com importação de NFe (XML) e rastreio de entradas.</li>
            <li>Stock e vendas ligados: cada venda atualiza saldos e histórico automaticamente.</li>
            <li>Indicadores por marca, curva ABC e Pareto para decisão comercial.</li>
          </ul>
        </div>
        <div className="login-form-panel">
          <main className="card card-elevated">
            <header className="header">
              <div className="login-brand">
                <span className="login-mark" aria-hidden>
                  L
                </span>
                <div>
                  <h1 className="login-title">{BRAND_NAME}</h1>
                  <p className="login-sub muted small">{BRAND_TAGLINE}</p>
                </div>
              </div>
              <p className="muted small" style={{ marginTop: "0.65rem" }}>
                Aceda ao painel para operar stock, vendas e documentos fiscais.
              </p>
            </header>
            <div className="tabs small" style={{ marginTop: "1rem" }}>
              <button
                type="button"
                className={authMode === "login" ? "active" : ""}
                onClick={() => onAuthModeChange("login")}
              >
                Entrar
              </button>
              <button
                type="button"
                className={authMode === "register" ? "active" : ""}
                onClick={() => onAuthModeChange("register")}
              >
                Criar conta
              </button>
            </div>
            <form onSubmit={onSubmit} className="form" style={{ marginTop: "0.75rem" }}>
              <label>
                Email
                <input
                  type="email"
                  autoComplete="email"
                  value={email}
                  onChange={(ev) => onEmailChange(ev.target.value)}
                  required
                />
              </label>
              <label>
                Palavra-passe
                <input
                  type="password"
                  autoComplete={authMode === "login" ? "current-password" : "new-password"}
                  value={password}
                  onChange={(ev) => onPasswordChange(ev.target.value)}
                  required
                  minLength={8}
                />
              </label>
              {error ? <p className="error">{error}</p> : null}
              <button
                type="submit"
                className="primary"
                disabled={busy}
                aria-label={authMode === "login" ? "Entrar na conta" : "Registar nova conta"}
              >
                {busy ? (
                  <span className="btn-inline-loading">
                    <span
                      className="ui-spinner"
                      style={{ width: "1rem", height: "1rem", color: "#fff" }}
                    />
                    A processar…
                  </span>
                ) : authMode === "login" ? (
                  "Entrar"
                ) : (
                  "Registar"
                )}
              </button>
            </form>
            <p className="hint muted">
              API em <code>http://localhost:8080</code> (proxy Vite). Em produção defina{" "}
              <code>VITE_API_BASE</code>.
            </p>
          </main>
        </div>
      </div>
    </div>
  );
}
