import { useNavigate } from "@tanstack/react-router";
import { Search } from "lucide-react";
import { useEffect, useMemo, useRef, useState } from "react";
import { Input } from "@/components/ui/input";
import { globalSearch, type SearchResult } from "@/lib/esg-data";

export function GlobalSearch() {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();
  const boxRef = useRef<HTMLDivElement>(null);

  const results = useMemo(() => globalSearch(query), [query]);

  useEffect(() => {
    const onClick = (e: MouseEvent) => {
      if (boxRef.current && !boxRef.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", onClick);
    return () => document.removeEventListener("mousedown", onClick);
  }, []);

  const go = (r: SearchResult) => {
    setOpen(false);
    setQuery("");
    navigate({ to: r.to, params: r.params as never });
  };

  const groups = ["Requirements", "Documents", "Reports", "Frameworks"] as const;

  return (
    <div ref={boxRef} className="relative hidden max-w-sm flex-1 md:block">
      <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
      <Input
        value={query}
        onChange={(e) => {
          setQuery(e.target.value);
          setOpen(true);
        }}
        onFocus={() => setOpen(true)}
        placeholder="Search requirements, documents, reports…"
        className="h-9 bg-card/60 pl-9 backdrop-blur"
      />
      {open && query.trim() !== "" && (
        <div className="glass-panel absolute left-0 right-0 top-11 z-50 max-h-96 overflow-y-auto p-2">
          {results.length === 0 ? (
            <p className="px-3 py-4 text-sm text-muted-foreground">
              No matches for “{query}”.
            </p>
          ) : (
            groups.map((g) => {
              const items = results.filter((r) => r.group === g);
              if (!items.length) return null;
              return (
                <div key={g} className="mb-1">
                  <p className="px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-muted-foreground">
                    {g}
                  </p>
                  {items.map((r) => (
                    <button
                      key={`${g}-${r.id}`}
                      onClick={() => go(r)}
                      className="block w-full rounded-lg px-3 py-2 text-left transition-colors hover:bg-accent/70"
                    >
                      <p className="text-sm font-medium">{r.title}</p>
                      <p className="text-[11px] text-muted-foreground">{r.subtitle}</p>
                    </button>
                  ))}
                </div>
              );
            })
          )}
        </div>
      )}
    </div>
  );
}
