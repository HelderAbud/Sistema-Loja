import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { createBrand, listBrands } from "../../../api";
import { TableSkeleton } from "../../../components/ui/TableSkeleton";
import { invalidateLojappDataQueries, queryKeys } from "../../../queryKeys";

export function BrandsTab() {
  const queryClient = useQueryClient();
  const [newBrandName, setNewBrandName] = useState("");

  const brandsQuery = useQuery({
    queryKey: queryKeys.brands(),
    queryFn: listBrands,
  });

  const createBrandMut = useMutation({
    mutationFn: (name: string) => createBrand(name),
    onSuccess: async (_, name) => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.brands() });
      invalidateLojappDataQueries(queryClient);
      toast.success(`Marca «${name}» adicionada`);
    },
    onError: (e: unknown) => {
      toast.error(String(e));
    },
  });

  async function onCreateBrand(e: FormEvent) {
    e.preventDefault();
    const name = newBrandName.trim();
    if (!name) return;
    await createBrandMut.mutateAsync(name);
    setNewBrandName("");
  }

  const brands = brandsQuery.data ?? null;
  const busyBrands = createBrandMut.isPending;

  if (brandsQuery.isError) {
    return <p className="error banner">{String(brandsQuery.error)}</p>;
  }

  if (brandsQuery.isFetching && !brandsQuery.data) {
    return (
      <div className="card" aria-busy="true">
        <div className="section-head">
          <h2>Marcas</h2>
        </div>
        <TableSkeleton rows={4} label="A carregar marcas" />
      </div>
    );
  }

  if (!brands) return null;

  return (
    <section className="card">
      <div className="section-head">
        <h2>Marcas</h2>
      </div>
      <p className="muted small section-lead">
        Organize o catálogo por fabricante ou linha comercial.
      </p>
      <form onSubmit={onCreateBrand} className="form inline">
        <input
          placeholder="Nova marca"
          value={newBrandName}
          onChange={(ev) => setNewBrandName(ev.target.value)}
          maxLength={200}
        />
        <button type="submit" className="primary" disabled={busyBrands}>
          {busyBrands ? (
            <span className="btn-inline-loading">
              <span
                className="ui-spinner"
                style={{ width: "0.95rem", height: "0.95rem", color: "#fff" }}
              />
              A gravar…
            </span>
          ) : (
            "Adicionar"
          )}
        </button>
      </form>
      <ul className="list">
        {brands.map((b) => (
          <li key={b.id}>{b.name}</li>
        ))}
      </ul>
      {brands.length === 0 ? <p className="muted">Nenhuma marca ainda.</p> : null}
    </section>
  );
}
