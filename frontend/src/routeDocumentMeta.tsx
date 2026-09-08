import { useEffect } from "react";
import { useLocation } from "react-router-dom";

const SITE = "LojApp";

const DEFAULT_DESCRIPTION =
  "Plataforma de Gestão Comercial com Automação Fiscal — stock, vendas, NFe e dashboards.";

function metaForPath(pathname: string): { title: string; description: string } {
  if (pathname === "/" || pathname === "") {
    return {
      title: `${SITE} · Gestão comercial e fiscal`,
      description: DEFAULT_DESCRIPTION,
    };
  }
  if (pathname.startsWith("/piloto")) {
    return {
      title: `Painel piloto · ${SITE}`,
      description: "Stock, NFe, vendas e indicadores — área operacional da loja.",
    };
  }
  if (pathname.startsWith("/login")) {
    return {
      title: `Iniciar sessão · ${SITE}`,
      description: "Aceda ao painel com a sua conta.",
    };
  }
  if (pathname.startsWith("/pitch")) {
    return {
      title: `Pitch · ${SITE}`,
      description:
        "NFe XML, stock, PDV, comissões e KPIs isolados por conta — monólito Spring Boot + React.",
    };
  }
  if (
    pathname === "/home" ||
    pathname.startsWith("/catalog") ||
    pathname.startsWith("/product/") ||
    pathname === "/cart" ||
    pathname === "/orders" ||
    pathname === "/seller" ||
    pathname === "/app"
  ) {
    return {
      title: `${SITE} · Gestão comercial e fiscal`,
      description: DEFAULT_DESCRIPTION,
    };
  }
  return {
    title: `Página não encontrada · ${SITE}`,
    description: "Este endereço não existe no LojApp.",
  };
}

function setMeta(attr: "name" | "property", key: string, content: string) {
  const el = document.querySelector(`meta[${attr}="${key}"]`);
  if (el) el.setAttribute("content", content);
}

/**
 * Atualiza document.title e meta tags relevantes para SEO e pré-visualizações (OG/Twitter)
 * em cada mudança de rota — complemento ao index.html estático.
 */
export function RouteDocumentHead() {
  const { pathname } = useLocation();

  useEffect(() => {
    const { title, description } = metaForPath(pathname);
    document.title = title;

    setMeta("name", "description", description);
    setMeta("property", "og:title", title);
    setMeta("property", "og:description", description);
    setMeta("property", "og:url", `${window.location.origin}${pathname}`);
    setMeta("name", "twitter:title", title);
    setMeta("name", "twitter:description", description);
  }, [pathname]);

  return null;
}
