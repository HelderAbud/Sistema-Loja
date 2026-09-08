import loginShot from "../../../../docs/screenshots/01-login.png";
import dashboardShot from "../../../../docs/screenshots/02-dashboard.png";
import salesShot from "../../../../docs/screenshots/03-vendas.png";
import stockShot from "../../../../docs/screenshots/04-estoque.png";
import nfeShot from "../../../../docs/screenshots/05-importacao-xml.png";
import reportsShot from "../../../../docs/screenshots/06-relatorios.png";

export const LANDING_SHOTS = [
  { src: loginShot, alt: "Ecrã de login do LojApp", caption: "Login" },
  { src: dashboardShot, alt: "Dashboard com KPIs e curva ABC", caption: "Dashboard" },
  { src: salesShot, alt: "Histórico de vendas no painel", caption: "Vendas" },
  { src: stockShot, alt: "Stock e movimentos de inventário", caption: "Stock" },
  { src: nfeShot, alt: "Importação de NFe por XML", caption: "Importação XML" },
  { src: reportsShot, alt: "Relatórios e indicadores por marca", caption: "Relatórios" },
] as const;
