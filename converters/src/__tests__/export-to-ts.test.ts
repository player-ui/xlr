import { test, expect, describe } from "vitest";
import type { NamedType } from "@xlr-lib/xlr";
import { exportTypesToTypeScript } from "../ts/xlr-to-ts";

const IMPORT_MAP = new Map([["@player-ui/types", ["Asset", "Binding"]]]);

const ASSET_TYPE: NamedType = {
  name: "Asset",
  type: "object",
  source: "test",
  properties: {
    id: { required: true, node: { type: "string" } },
    type: { required: true, node: { type: "string" } },
  },
  additionalProperties: false,
};

const ACTION_ASSET_TYPE: NamedType = {
  name: "ActionAsset",
  type: "object",
  source: "test",
  properties: {
    id: { required: true, node: { type: "string" } },
    type: { required: true, node: { type: "string", const: "action" } },
    value: { required: false, node: { type: "string" } },
  },
  additionalProperties: false,
};

describe("exportTypesToTypeScript", () => {
  test("exports named types as a .d.ts string", () => {
    const result = exportTypesToTypeScript(
      [ASSET_TYPE, ACTION_ASSET_TYPE],
      IMPORT_MAP,
    );
    expect(typeof result).toBe("string");
    expect(result).toContain("export interface Asset");
    expect(result).toContain("export interface ActionAsset");
    expect(result).toMatchSnapshot();
  });

  test("emits import declarations for referenced types", () => {
    const typeWithRef: NamedType = {
      name: "Wrapper",
      type: "object",
      source: "test",
      properties: {
        asset: {
          required: true,
          node: { type: "ref", ref: "Asset" },
        },
      },
      additionalProperties: false,
    };

    const result = exportTypesToTypeScript([typeWithRef], IMPORT_MAP);
    expect(result).toContain("@player-ui/types");
    expect(result).toContain("Asset");
    expect(result).toMatchSnapshot();
  });

  test("skips import declarations for unreferenced packages", () => {
    const result = exportTypesToTypeScript([ASSET_TYPE], IMPORT_MAP);
    // Asset itself is defined, not imported — so the import declaration should be absent
    expect(result).not.toContain("@player-ui/types");
    expect(result).toMatchSnapshot();
  });
});
