import { useEffect } from "react";
import { useIsFetching, useQuery } from "@tanstack/react-query";
import { toast } from "sonner";
import { dashboardBrands, dashboardInventoryKpis, dashboardProductAbc } from "../../../api";
import { queryKeys } from "../../../queryKeys";

export function useDashboardData(applied: { from?: string; to?: string }) {
  const rangeKey = `${applied.from ?? ""}|${applied.to ?? ""}`;

  const brandsQ = useQuery({
    queryKey: queryKeys.dashboard.brands(rangeKey),
    queryFn: () => dashboardBrands(applied.from, applied.to),
  });

  const abcQ = useQuery({
    queryKey: queryKeys.dashboard.abc(rangeKey),
    queryFn: () => dashboardProductAbc(applied.from, applied.to),
  });

  const invQ = useQuery({
    queryKey: queryKeys.dashboard.inventory(),
    queryFn: dashboardInventoryKpis,
  });

  const err = brandsQ.error ?? abcQ.error ?? invQ.error;
  useEffect(() => {
    if (err) toast.error(String(err));
  }, [err]);

  const fetchingDash = useIsFetching({ queryKey: queryKeys.dashboard.root() }) > 0;

  return { brandsQ, abcQ, invQ, fetchingDash };
}
