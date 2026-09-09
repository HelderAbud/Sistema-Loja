import { describe, expect, it } from "vitest";
import { buildCloseCashBody } from "./cashClose";

describe("buildCloseCashBody", () => {
  it("monta o fecho com motivo vazio como null", () => {
    expect(
      buildCloseCashBody({
        cashSessionId: 7,
        countedRaw: "50",
        differenceReason: "  ",
        managerApproval: false,
      }),
    ).toEqual({
      body: {
        cashSessionId: 7,
        countedAmount: 50,
        differenceReason: null,
        managerApproval: false,
      },
    });
  });

  it("recusa valor contado inválido", () => {
    expect(
      buildCloseCashBody({
        cashSessionId: 7,
        countedRaw: "abc",
        differenceReason: "",
        managerApproval: true,
      }),
    ).toEqual({ error: "Valor contado no caixa inválido." });
  });
});
