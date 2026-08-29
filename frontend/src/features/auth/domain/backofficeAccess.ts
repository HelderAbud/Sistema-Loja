const BACKOFFICE_ROLES = new Set(["USER", "ADMIN", "MANAGER", "REPRESENTATIVE"]);
const POS_ROLES = new Set(["USER", "ADMIN", "MANAGER", "CASHIER", "SELLER"]);
const COMMISSION_REPORT_ROLES = new Set(["USER", "ADMIN", "MANAGER"]);

/** NFe, ajuste de stock e mutações de catálogo (marcas/produtos/fornecedores). */
export function canManageBackofficeCatalog(appRole: string | undefined): boolean {
  return appRole !== undefined && BACKOFFICE_ROLES.has(appRole);
}

/** Finalizar PDV / ecrã Nova venda. */
export function canFinalizePosSale(appRole: string | undefined): boolean {
  return appRole !== undefined && POS_ROLES.has(appRole);
}

/** Relatório e CSV de comissões. */
export function canViewCommissionReport(appRole: string | undefined): boolean {
  return appRole !== undefined && COMMISSION_REPORT_ROLES.has(appRole);
}
