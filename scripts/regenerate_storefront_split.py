"""Regenerate storefront page modules with minimal imports from git original."""
from __future__ import annotations

import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ORIGINAL = subprocess.check_output(
    ["git", "show", "Principal:frontend/src/pages/StorefrontPages.tsx"],
    cwd=ROOT,
    text=True,
    encoding="utf-8",
)

lines = ORIGINAL.splitlines(keepends=True)

# Shared section: before first export function (helpers + StoreHeader + useStorefrontCatalog)
first_export = next(i for i, l in enumerate(lines) if l.startswith("export function LandingPage"))
shared_src = "".join(lines[:first_export])

# Fix paths for storefront/ subfolder
shared_src = shared_src.replace('from "../', 'from "../../')

# Trim shared imports to only what shared code uses
shared_header = '''import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { listProducts } from "../../api";
import { useAuthStore } from "../../authStore";
import { BRAND_NAME } from "../../brand";
import { storefrontProducts, useCartStore } from "../../features/storefront";

'''

def strip_import_lines(section_lines: list[str]) -> list[str]:
    """Remove full import statements, including multi-line blocks."""
    out: list[str] = []
    skipping = False
    for line in section_lines:
        stripped = line.lstrip()
        if not skipping and stripped.startswith("import "):
            skipping = True
            if re.search(r'from\s+["\'][^"\']+["\']\s*;?\s*$', stripped):
                skipping = False
            continue
        if skipping:
            if re.search(r'from\s+["\'][^"\']+["\']\s*;?\s*$', stripped):
                skipping = False
            continue
        out.append(line)
    return out


# Extract shared body (functions only, skip old imports)
shared_body_lines = strip_import_lines(lines[:first_export])
shared_body = shared_header + "".join(shared_body_lines)

# Shared helpers must be exported for page modules.
shared_body = shared_body.replace("function formatCurrency", "export function formatCurrency")
shared_body = shared_body.replace("function percentDelta", "export function percentDelta")
shared_body = shared_body.replace("function csvEscape", "export function csvEscape")
shared_body = shared_body.replace("function formatPercent", "export function formatPercent")
shared_body = shared_body.replace("type SummaryTemplate", "export type SummaryTemplate")
shared_body = shared_body.replace(
    "const SUMMARY_TEMPLATE_STORAGE_KEY",
    "export const SUMMARY_TEMPLATE_STORAGE_KEY",
)
shared_body = shared_body.replace(
    "const SUMMARY_CUSTOM_TEXT_STORAGE_KEY",
    "export const SUMMARY_CUSTOM_TEXT_STORAGE_KEY",
)
shared_body = shared_body.replace(
    "function getSavedSummaryTemplate",
    "export function getSavedSummaryTemplate",
)
shared_body = shared_body.replace(
    "function getSavedCustomSummaryText",
    "export function getSavedCustomSummaryText",
)
shared_body = shared_body.replace("function useStorefrontCatalog", "export function useStorefrontCatalog")
shared_body = shared_body.replace("function StoreHeader", "export function StoreHeader")

out_dir = ROOT / "frontend/src/pages/storefront"
out_dir.mkdir(parents=True, exist_ok=True)
(out_dir / "storefrontShared.tsx").write_text(shared_body, encoding="utf-8")

# Per-page import templates (minimal, hand-curated from usage)
PAGE_IMPORTS: dict[str, str] = {
    "LandingPage": '''import { Link } from "react-router-dom";
import { BRAND_NAME, BRAND_TAGLINE } from "../../brand";
import { socialProof } from "../../features/storefront";
import { StoreHeader } from "./storefrontShared";

''',
    "HomePage": '''import { socialProof } from "../../features/storefront";
import { StoreHeader } from "./storefrontShared";

''',
    "CatalogPage": '''import { Link } from "react-router-dom";
import { StoreHeader, useStorefrontCatalog } from "./storefrontShared";
import { StoreCatalogGridSkeleton } from "../../components/ui/StoreCatalogGridSkeleton";

''',
    "ProductPage": '''import { Link, Navigate, useParams } from "react-router-dom";
import { getProductBySlug, useCartStore } from "../../features/storefront";
import { StoreHeader, formatCurrency } from "./storefrontShared";

''',
    "CartPage": '''import { useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { finalizePosSale, getCurrentCashSession, type PosPaymentMethod } from "../../api";
import { useCartStore, useCartSummary } from "../../features/storefront";
import { StoreHeader, formatCurrency } from "./storefrontShared";

''',
    "SellerAreaPage": '''import { type ChangeEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  closeCashSession,
  getCloseCashSessionPreview,
  getCurrentCashSession,
  listBrands,
  listProducts,
  listSales,
  openCashSession,
  summarizeSales,
  summarizeSalesDaily,
} from "../../api";
import { useAuthStore } from "../../authStore";
import { sellerSnapshot } from "../../features/storefront";
import { TableSkeleton } from "../../components/ui/TableSkeleton";
import {
  formatCurrency,
  formatPercent,
  percentDelta,
  StoreHeader,
} from "./storefrontShared";

''',
    "OrdersPage": '''import { type ChangeEvent, useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import {
  Area,
  CartesianGrid,
  ComposedChart,
  Legend,
  Line,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { listBrands, listProducts, listSales, summarizeSales, summarizeSalesDaily } from "../../api";
import { useAuthStore } from "../../authStore";
import { TableSkeleton } from "../../components/ui/TableSkeleton";
import {
  type OrdersFilterPreset,
  type OrdersSortKey,
  parseOrderPresetsImport,
  sortSaleRows,
  useStorefrontOrdersFilters,
} from "../../features/orders";
import {
  csvEscape,
  formatCurrency,
  formatPercent,
  getSavedCustomSummaryText,
  getSavedSummaryTemplate,
  percentDelta,
  StoreHeader,
  SUMMARY_CUSTOM_TEXT_STORAGE_KEY,
  SUMMARY_TEMPLATE_STORAGE_KEY,
  type SummaryTemplate,
} from "./storefrontShared";

''',
    "PitchPage": '''import { Link } from "react-router-dom";
import { BRAND_NAME, BRAND_TAGLINE } from "../../brand";
import { socialProof } from "../../features/storefront";
import { StoreHeader } from "./storefrontShared";

''',
}

pattern = re.compile(r"export function (\w+)\(\) \{", re.MULTILINE)
matches = list(pattern.finditer(ORIGINAL))
for idx, match in enumerate(matches):
    name = match.group(1)
    start = match.start()
    end = matches[idx + 1].start() if idx + 1 < len(matches) else len(ORIGINAL)
    body = ORIGINAL[start:end]
    imports = PAGE_IMPORTS.get(name, "")
    if not imports:
        raise SystemExit(f"Missing import template for {name}")
    (out_dir / f"{name}.tsx").write_text(imports + body, encoding="utf-8")

barrel = """export { LandingPage } from "./storefront/LandingPage";
export { HomePage } from "./storefront/HomePage";
export { CatalogPage } from "./storefront/CatalogPage";
export { ProductPage } from "./storefront/ProductPage";
export { CartPage } from "./storefront/CartPage";
export { SellerAreaPage } from "./storefront/SellerAreaPage";
export { OrdersPage } from "./storefront/OrdersPage";
export { PitchPage } from "./storefront/PitchPage";
"""
(ROOT / "frontend/src/pages/StorefrontPages.tsx").write_text(barrel, encoding="utf-8")
print("regenerated", len(PAGE_IMPORTS), "pages")
