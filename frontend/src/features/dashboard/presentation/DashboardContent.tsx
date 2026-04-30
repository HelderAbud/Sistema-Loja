import type { BrandDashboard, InventoryKpis, ProductAbcDashboard } from "@/api";
import { BrandKpiSection } from "./BrandKpiSection";
import { BrandRevenueChart } from "./BrandRevenueChart";
import { BrandTableSection } from "./BrandTableSection";
import { InventoryKpiSection } from "./InventoryKpiSection";
import { ProductAbcSection } from "./ProductAbcSection";

type Props = {
  data: BrandDashboard;
  abc?: ProductAbcDashboard;
  inv?: InventoryKpis;
  inventoryPending: boolean;
};

export function DashboardContent({ data, abc, inv, inventoryPending }: Props) {
  return (
    <>
      <BrandKpiSection data={data} abc={abc} inv={inv} inventoryPending={inventoryPending} />
      <BrandRevenueChart data={data} />
      <InventoryKpiSection inv={inv} />
      <ProductAbcSection abc={abc} />
      <BrandTableSection data={data} />
    </>
  );
}
