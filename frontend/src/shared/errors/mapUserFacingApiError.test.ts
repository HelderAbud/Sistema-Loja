import { describe, expect, it } from "vitest";
import { mapFetchFailureToUserMessage } from "./mapUserFacingApiError";

describe("mapFetchFailureToUserMessage", () => {
  it("avisa que a demo pode estar a acordar quando o fetch falha", () => {
    const err = new TypeError("Failed to fetch");
    expect(mapFetchFailureToUserMessage(err)).toMatch(/acordar/i);
  });
});
