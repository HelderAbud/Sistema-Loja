import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toSalesApiInstantRange } from "../features/dashboard/domain/dateIsoRange";
import { listSales } from "./sales";

describe("listSales date query params", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(() =>
        Promise.resolve({
          ok: true,
          status: 200,
          text: async () =>
            JSON.stringify({
              content: [],
              totalElements: 0,
              totalPages: 0,
              size: 25,
              number: 0,
              first: true,
              last: true,
            }),
        } as Response),
      ),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("não envia YYYY-MM-DD cru; envia Instant ISO do helper", async () => {
    const range = toSalesApiInstantRange("2026-09-01", "2026-09-30");
    await listSales({ page: 0, size: 25, from: range.from, to: range.to });

    const href = String(vi.mocked(fetch).mock.calls[0]?.[0]);
    const url = new URL(href, "http://localhost");
    expect(url.searchParams.get("from")).toBe(range.from);
    expect(url.searchParams.get("to")).toBe(range.to);
    expect(url.searchParams.get("from")).toMatch(/T.*Z$/);
    expect(url.searchParams.get("from")).not.toBe("2026-09-01");
  });
});
