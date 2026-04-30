import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useCurrentUser } from "@/hooks";
import { BRAND_NAME, BRAND_TAGLINE } from "../brand";
import { BrandsTab } from "../features/brands/presentation/BrandsTab";
import { PilotoDashboardTab } from "../features/dashboard";
import { PilotoInventoryTab } from "../features/inventory";
import { PilotoNfeTab } from "../features/nfe";
import { PilotoSaleTab } from "../features/sales";
import { ProductsBrowseTab } from "../components/ProductsBrowseTab";
import { SalesHistoryTab } from "../components/SalesHistoryTab";
import { DEFAULT_PILOTO_TAB, isPilotoTab, type PilotoTab } from "./types";

function IconUser() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8Z"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
      />
    </svg>
  );
}

type Props = {
  email: string;
  error: string | null;
  onLogout: () => void | Promise<void>;
};

export function PilotoWorkspacePage({ email, error, onLogout }: Props) {
  const meQ = useCurrentUser();
  const params = useParams<{ tab: string }>();
  const navigate = useNavigate();
  const segment = params.tab;
  const tab: PilotoTab = isPilotoTab(segment) ? segment : DEFAULT_PILOTO_TAB;
  const displayEmail = meQ.data?.email ?? email;
  const appRole = meQ.data?.appRole;

  useEffect(() => {
    if (!isPilotoTab(segment)) {
      navigate(`/piloto/${DEFAULT_PILOTO_TAB}`, { replace: true });
    }
  }, [segment, navigate]);

  return (
    <div className="shell shell-wide">
      <header className="header row workspace-header">
        <div className="workspace-brand-block">
          <span className="workspace-mark" aria-hidden>
            L
          </span>
          <div className="minw-0">
            <h1>{BRAND_NAME}</h1>
            <p className="workspace-product-tagline">{BRAND_TAGLINE}</p>
          </div>
        </div>
        <div className="workspace-actions">
          <div className="workspace-user-row">
            {displayEmail ? (
              <div className="user-chip" title={displayEmail}>
                <IconUser />
                <span>{displayEmail}</span>
              </div>
            ) : null}
            {appRole === "REPRESENTATIVE" ? (
              <span className="role-badge role-badge--representative">Representante</span>
            ) : null}
            {appRole === "ADMIN" ? (
              <span className="role-badge role-badge--admin">Administrador</span>
            ) : null}
          </div>
          <button type="button" className="ghost btn-signout" onClick={onLogout}>
            Sair
          </button>
        </div>
      </header>

      <div className="tab-nav-shell">
        <nav className="tabs tabs-wrap tabs-rail" aria-label="Secções do painel">
          <button
            type="button"
            className={tab === "products" ? "active" : ""}
            onClick={() => navigate("/piloto/products")}
          >
            Produtos
          </button>
          <button
            type="button"
            className={tab === "sales" ? "active" : ""}
            onClick={() => navigate("/piloto/sales")}
          >
            Vendas
          </button>
          <button
            type="button"
            className={tab === "brands" ? "active" : ""}
            onClick={() => navigate("/piloto/brands")}
          >
            Marcas
          </button>
          <button
            type="button"
            className={tab === "nfe" ? "active" : ""}
            onClick={() => navigate("/piloto/nfe")}
          >
            NFe
          </button>
          <button
            type="button"
            className={tab === "inventory" ? "active" : ""}
            onClick={() => navigate("/piloto/inventory")}
          >
            Stock
          </button>
          <button
            type="button"
            className={tab === "sale" ? "active" : ""}
            onClick={() => navigate("/piloto/sale")}
          >
            Nova venda
          </button>
          <button
            type="button"
            className={tab === "dashboard" ? "active" : ""}
            onClick={() => navigate("/piloto/dashboard")}
          >
            Dashboard
          </button>
        </nav>
      </div>

      {error ? <p className="error banner">{error}</p> : null}

      {tab === "products" ? <ProductsBrowseTab /> : null}
      {tab === "sales" ? <SalesHistoryTab /> : null}
      {tab === "nfe" ? <PilotoNfeTab /> : null}
      {tab === "inventory" ? <PilotoInventoryTab /> : null}
      {tab === "sale" ? <PilotoSaleTab /> : null}
      {tab === "dashboard" ? <PilotoDashboardTab /> : null}

      {tab === "brands" ? <BrandsTab /> : null}
    </div>
  );
}
