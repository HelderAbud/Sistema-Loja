import { create } from "zustand";
import { calculateCartTotal } from "../domain/cartTotals";

type CartItem = {
  id: string;
  name: string;
  price: number;
  quantity: number;
};

type CartState = {
  items: CartItem[];
  addItem: (product: { id: string; name: string; price: number }) => void;
  removeItem: (productId: string) => void;
  clear: () => void;
};

export const useCartStore = create<CartState>((set) => ({
  items: [],
  addItem: (product) =>
    set((state) => {
      const existing = state.items.find((item) => item.id === product.id);
      if (existing) {
        return {
          items: state.items.map((item) =>
            item.id === product.id ? { ...item, quantity: item.quantity + 1 } : item,
          ),
        };
      }
      return {
        items: [
          ...state.items,
          { id: product.id, name: product.name, price: product.price, quantity: 1 },
        ],
      };
    }),
  removeItem: (productId) =>
    set((state) => ({
      items: state.items
        .map((item) => (item.id === productId ? { ...item, quantity: item.quantity - 1 } : item))
        .filter((item) => item.quantity > 0),
    })),
  clear: () => set({ items: [] }),
}));

export function useCartSummary() {
  const items = useCartStore((state) => state.items);
  const totals = calculateCartTotal(items);
  return { items, totals };
}
