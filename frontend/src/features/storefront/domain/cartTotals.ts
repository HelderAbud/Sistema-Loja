export type CartItemInput = {
  id: string;
  quantity: number;
  price: number;
};

export function calculateCartSubtotal(items: CartItemInput[]) {
  return items.reduce((total, item) => total + item.quantity * item.price, 0);
}

export function calculateCartTotal(items: CartItemInput[]) {
  const subtotal = calculateCartSubtotal(items);
  const shipping = subtotal >= 500 || subtotal === 0 ? 0 : 24.9;
  return {
    subtotal,
    shipping,
    total: subtotal + shipping,
  };
}
