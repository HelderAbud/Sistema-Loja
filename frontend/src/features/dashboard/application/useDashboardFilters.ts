import { type FormEvent, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { queryKeys } from "@/queryKeys";
import { toIsoEndOfDay, toIsoStartOfDay } from "../domain/dateIsoRange";

export function useDashboardFilters() {
  const queryClient = useQueryClient();
  const [fromDay, setFromDay] = useState("");
  const [toDay, setToDay] = useState("");
  const [applied, setApplied] = useState<{ from?: string; to?: string }>({});

  function onSubmit(e: FormEvent) {
    e.preventDefault();
    const from = toIsoStartOfDay(fromDay.trim());
    const to = toIsoEndOfDay(toDay.trim() || fromDay.trim());
    const rk = `${from ?? ""}|${to ?? ""}`;
    setApplied({ from, to });
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.brands(rk) });
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.abc(rk) });
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.inventory() });
    toast.success("Indicadores atualizados");
  }

  function loadDefault() {
    const rk = "|";
    setFromDay("");
    setToDay("");
    setApplied({});
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.brands(rk) });
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.abc(rk) });
    void queryClient.invalidateQueries({ queryKey: queryKeys.dashboard.inventory() });
    toast.success("Vista: últimos 30 dias");
  }

  return {
    fromDay,
    toDay,
    applied,
    setFromDay,
    setToDay,
    onSubmit,
    loadDefault,
  };
}
