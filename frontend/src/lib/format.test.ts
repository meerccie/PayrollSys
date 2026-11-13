import { describe, expect, it } from "vitest";
import { formatPhp } from "./format";

describe("formatPhp", () => {
  it("formats peso amounts with 2 decimals", () => {
    expect(formatPhp(26542.45)).toContain("26,542.45");
    expect(formatPhp(0)).toContain("0.00");
  });
});
