type StoreCatalogGridSkeletonProps = {
  count?: number;
};

/**
 * Grelha de cartões placeholder para o catálogo público (storefront).
 */
export function StoreCatalogGridSkeleton({ count = 6 }: StoreCatalogGridSkeletonProps) {
  return (
    <section
      className="store-grid"
      role="status"
      aria-live="polite"
      aria-busy="true"
      aria-label="A carregar catálogo"
    >
      {Array.from({ length: count }).map((_, i) => (
        <article key={i} className="store-product-card store-product-card--skeleton" aria-hidden>
          <div className="store-skel-line store-skel-line--brand" />
          <div className="store-skel-line store-skel-line--title" />
          <div className="store-skel-line store-skel-line--body" />
          <div className="store-skel-line store-skel-line--body short" />
          <div className="store-product-foot">
            <div className="store-skel-line store-skel-line--price" />
            <div className="store-skel-line store-skel-line--btn" />
          </div>
        </article>
      ))}
    </section>
  );
}
