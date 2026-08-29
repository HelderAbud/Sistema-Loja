import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useCurrentUser } from "@/hooks";
import {
  canFinalizePosSale,
  canManageBackofficeCatalog,
  canViewCommissionReport,
} from "@/features/auth";
import { PilotoCommissionTab } from "../features/commissions";
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
  const showBackofficeTabs = canManageBackofficeCatalog(appRole);
  const showPosTab = canFinalizePosSale(appRole);
  const showCommissionTab = canViewCommissionReport(appRole);

  useEffect(() => {
    if (!isPilotoTab(segment)) {
      navigate(`/piloto/${DEFAULT_PILOTO_TAB}`, { replace: true });
      return;
    }
    if (segment === "nfe" && meQ.isSuccess && !showBackofficeTabs) {
      navigate("/piloto/products", { replace: true });
    }
    if (segment === "sale" && meQ.isSuccess && !showPosTab) {
      navigate("/piloto/products", { replace: true });
    }
    if (segment === "commissions" && meQ.isSuccess && !showCommissionTab) {
      navigate("/piloto/products", { replace: true });
    }
  }, [segment, navigate, meQ.isSuccess, showBackofficeTabs, showPosTab, showCommissionTab]);

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
        <nav className="tabs tabs-wrap tabs-rail" role="tablist" aria-label="Secções do painel">
          <button
            type="button"
            role="tab"
            id="piloto-tab-products"
            aria-selected={tab === "products"}
            aria-controls="piloto-panel-products"
            tabIndex={tab === "products" ? 0 : -1}
            className={tab === "products" ? "active" : ""}
            onClick={() => navigate("/piloto/products")}
          >
            Produtos
          </button>
          <button
            type="button"
            role="tab"
            id="piloto-tab-sales"
            aria-selected={tab === "sales"}
            aria-controls="piloto-panel-sales"
            tabIndex={tab === "sales" ? 0 : -1}
            className={tab === "sales" ? "active" : ""}
            onClick={() => navigate("/piloto/sales")}
          >
            Vendas
          </button>
          <button
            type="button"
            role="tab"
            id="piloto-tab-brands"
            aria-selected={tab === "brands"}
            aria-controls="piloto-panel-brands"
            tabIndex={tab === "brands" ? 0 : -1}
            className={tab === "brands" ? "active" : ""}
            onClick={() => navigate("/piloto/brands")}
          >
            Marcas
          </button>
          {showBackofficeTabs ? (
            <button
              type="button"
              role="tab"
              id="piloto-tab-nfe"
              aria-selected={tab === "nfe"}
              aria-controls="piloto-panel-nfe"
              tabIndex={tab === "nfe" ? 0 : -1}
              className={tab === "nfe" ? "active" : ""}
              onClick={() => navigate("/piloto/nfe")}
            >
              NFe
            </button>
          ) : null}
          <button
            type="button"
            role="tab"
            id="piloto-tab-inventory"
            aria-selected={tab === "inventory"}
            aria-controls="piloto-panel-inventory"
            tabIndex={tab === "inventory" ? 0 : -1}
            className={tab === "inventory" ? "active" : ""}
            onClick={() => navigate("/piloto/inventory")}
          >
            Stock
          </button>
          {showPosTab ? (
            <button
              type="button"
              role="tab"
              id="piloto-tab-sale"
              aria-selected={tab === "sale"}
              aria-controls="piloto-panel-sale"
              tabIndex={tab === "sale" ? 0 : -1}
              className={tab === "sale" ? "active" : ""}
              onClick={() => navigate("/piloto/sale")}
            >
              Nova venda
            </button>
          ) : null}
          {showCommissionTab ? (
            <button
              type="button"
              role="tab"
              id="piloto-tab-commissions"
              aria-selected={tab === "commissions"}
              aria-controls="piloto-panel-commissions"
              tabIndex={tab === "commissions" ? 0 : -1}
              className={tab === "commissions" ? "active" : ""}
              onClick={() => navigate("/piloto/commissions")}
            >
              Comissões
            </button>
          ) : null}
          <button
            type="button"
            role="tab"
            id="piloto-tab-dashboard"
            aria-selected={tab === "dashboard"}
            aria-controls="piloto-panel-dashboard"
            tabIndex={tab === "dashboard" ? 0 : -1}
            className={tab === "dashboard" ? "active" : ""}
            onClick={() => navigate("/piloto/dashboard")}
          >
            Dashboard
          </button>
        </nav>
      </div>

      {error ? <p className="error banner">{error}</p> : null}

      {tab === "products" ? (
        <div role="tabpanel" id="piloto-panel-products" aria-labelledby="piloto-tab-products">
          <ProductsBrowseTab />
        </div>
      ) : null}
      {tab === "sales" ? (
        <div role="tabpanel" id="piloto-panel-sales" aria-labelledby="piloto-tab-sales">
          <SalesHistoryTab />
        </div>
      ) : null}
      {tab === "nfe" && showBackofficeTabs ? (
        <div role="tabpanel" id="piloto-panel-nfe" aria-labelledby="piloto-tab-nfe">
          <PilotoNfeTab />
        </div>
      ) : null}
      {tab === "inventory" ? (
        <div role="tabpanel" id="piloto-panel-inventory" aria-labelledby="piloto-tab-inventory">
          <PilotoInventoryTab />
        </div>
      ) : null}
      {tab === "sale" && showPosTab ? (
        <div role="tabpanel" id="piloto-panel-sale" aria-labelledby="piloto-tab-sale">
          <PilotoSaleTab />
        </div>
      ) : null}
      {tab === "commissions" && showCommissionTab ? (
        <div role="tabpanel" id="piloto-panel-commissions" aria-labelledby="piloto-tab-commissions">
          <PilotoCommissionTab />
        </div>
      ) : null}
      {tab === "dashboard" ? (
        <div role="tabpanel" id="piloto-panel-dashboard" aria-labelledby="piloto-tab-dashboard">
          <PilotoDashboardTab />
        </div>
      ) : null}

      {tab === "brands" ? (
        <div role="tabpanel" id="piloto-panel-brands" aria-labelledby="piloto-tab-brands">
          <BrandsTab />
        </div>
      ) : null}
    </div>
  );
}
