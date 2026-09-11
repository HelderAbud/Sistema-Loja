import { useCallback, useEffect, useRef, useState } from "react";
import { listProducts, type Product } from "@/api";

const SEARCH_DEBOUNCE_MS = 320;
const MIN_QUERY_LEN = 1;

type Props = {
  id: string;
  label: string;
  inputAriaLabel: string;
  onSelect: (product: Product) => void;
};

export function ProductSearchCombobox({ id, label, inputAriaLabel, onSelect }: Props) {
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [query, setQuery] = useState("");
  const [suggestions, setSuggestions] = useState<Product[]>([]);
  const [listOpen, setListOpen] = useState(false);
  const [suggestLoading, setSuggestLoading] = useState(false);

  const scheduleSearch = useCallback((raw: string) => {
    if (debounceRef.current) clearTimeout(debounceRef.current);
    const q = raw.trim();
    if (q.length < MIN_QUERY_LEN) {
      setSuggestions([]);
      setSuggestLoading(false);
      return;
    }
    setSuggestLoading(true);
    debounceRef.current = setTimeout(() => {
      listProducts({ page: 0, size: 20, q })
        .then((page) => {
          setSuggestions(page.content);
          setListOpen(true);
        })
        .catch(() => {
          setSuggestions([]);
        })
        .finally(() => setSuggestLoading(false));
    }, SEARCH_DEBOUNCE_MS);
  }, []);

  useEffect(
    () => () => {
      if (debounceRef.current) clearTimeout(debounceRef.current);
    },
    [],
  );

  return (
    <label className="combobox-wrap">
      {label}
      <div className="combobox">
        <input
          autoComplete="off"
          role="combobox"
          aria-label={inputAriaLabel}
          aria-autocomplete="list"
          aria-expanded={listOpen && (suggestLoading || suggestions.length > 0)}
          aria-controls={id}
          placeholder="Ex.: camiseta (mín. 1 letra)"
          value={query}
          onChange={(ev) => {
            const v = ev.target.value;
            setQuery(v);
            setListOpen(v.trim().length >= MIN_QUERY_LEN);
            scheduleSearch(v);
          }}
          onFocus={() => {
            if (query.trim().length >= MIN_QUERY_LEN && suggestions.length > 0) setListOpen(true);
          }}
          onBlur={() => {
            window.setTimeout(() => setListOpen(false), 180);
          }}
        />
        {listOpen && (suggestLoading || suggestions.length > 0) ? (
          <ul id={id} className="combobox-list" role="listbox" aria-label="Sugestões de produto">
            {suggestLoading ? (
              <li className="combobox-status muted small" role="option" aria-disabled="true">
                A pesquisar…
              </li>
            ) : (
              suggestions.map((p) => (
                <li key={p.id} role="option">
                  <button
                    type="button"
                    className="combobox-item"
                    onMouseDown={(ev) => ev.preventDefault()}
                    onClick={() => {
                      setQuery(`${p.name} (#${p.id})`);
                      setListOpen(false);
                      onSelect(p);
                    }}
                  >
                    #{p.id} — {p.name} ({p.brandName})
                  </button>
                </li>
              ))
            )}
          </ul>
        ) : null}
      </div>
    </label>
  );
}
