export type StoreProduct = {
  id: string;
  slug: string;
  name: string;
  brand: string;
  category: string;
  price: number;
  previousPrice?: number;
  rating: number;
  reviews: number;
  stock: number;
  description: string;
};

export const storefrontProducts: StoreProduct[] = [
  {
    id: "p1",
    slug: "tenis-urbano-lx",
    name: "Tênis Urbano LX",
    brand: "Aurum",
    category: "Calçados",
    price: 299.9,
    previousPrice: 349.9,
    rating: 4.8,
    reviews: 126,
    stock: 18,
    description: "Tênis premium com palmilha em espuma de memória e acabamento respirável.",
  },
  {
    id: "p2",
    slug: "jaqueta-tech-pro",
    name: "Jaqueta Tech Pro",
    brand: "Nexar",
    category: "Roupas",
    price: 459.9,
    rating: 4.7,
    reviews: 89,
    stock: 11,
    description: "Jaqueta impermeável com forro térmico para uso urbano e outdoor leve.",
  },
  {
    id: "p3",
    slug: "mochila-commuter-28l",
    name: "Mochila Commuter 28L",
    brand: "Mira",
    category: "Acessórios",
    price: 219.9,
    previousPrice: 249.9,
    rating: 4.9,
    reviews: 201,
    stock: 26,
    description: "Mochila com compartimento para notebook e organização interna modular.",
  },
  {
    id: "p4",
    slug: "relogio-active-s",
    name: "Relógio Active S",
    brand: "Orion",
    category: "Wearables",
    price: 399.9,
    rating: 4.6,
    reviews: 154,
    stock: 9,
    description: "Smartwatch com monitoramento de saúde, NFC e bateria para até 7 dias.",
  },
];

export const socialProof = {
  stores: 58,
  orders: 12000,
  averageRating: 4.9,
};

export const sellerSnapshot = {
  monthRevenue: 128450,
  monthOrders: 382,
  conversionRate: 3.8,
  topBrand: "Aurum",
};

export function getProductBySlug(slug: string) {
  return storefrontProducts.find((product) => product.slug === slug) ?? null;
}
