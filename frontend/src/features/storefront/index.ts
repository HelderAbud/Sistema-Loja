export type { StoreProduct } from "./domain/catalog";
export {
  storefrontProducts,
  socialProof,
  sellerSnapshot,
  getProductBySlug,
} from "./domain/catalog";
export { calculateCartSubtotal, calculateCartTotal } from "./domain/cartTotals";
export type { CartItemInput } from "./domain/cartTotals";
export { useCartStore, useCartSummary } from "./application/cartStore";
