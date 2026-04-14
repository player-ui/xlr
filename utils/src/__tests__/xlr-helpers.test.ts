import { test, expect, describe } from "vitest";
import { NodeType } from "@xlr-lib/xlr";

import {
  applyPickOrOmitToNodeType,
  applyPartialOrRequiredToNodeType,
} from "../xlr-helpers";

describe("applyPickOrOmitToNodeType", () => {
  test("Omit - Doesn't filter property that doesn't exist", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: { foo: { node: { type: "string" }, required: false } },
      additionalProperties: false,
    };
    const operation = "Omit";
    const properties = new Set(["bar"]);

    const result = applyPickOrOmitToNodeType(baseObject, operation, properties);

    expect(result).toStrictEqual(baseObject);
  });

  test("Omit - Filters property that do exist", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: false },
        bar: { node: { type: "string" }, required: false },
      },
      additionalProperties: false,
    };

    const filteredObject: NodeType = {
      type: "object",
      properties: { foo: { node: { type: "string" }, required: false } },
      additionalProperties: false,
    };
    const operation = "Omit";
    const properties = new Set(["bar"]);

    const result = applyPickOrOmitToNodeType(baseObject, operation, properties);

    expect(result).toStrictEqual(filteredObject);
  });

  test("Pick - Selects property that do exist", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: false },
        bar: { node: { type: "string" }, required: false },
      },
      additionalProperties: false,
    };

    const filteredObject: NodeType = {
      type: "object",
      properties: { bar: { node: { type: "string" }, required: false } },
      additionalProperties: false,
    };
    const operation = "Pick";
    const properties = new Set(["bar"]);

    const result = applyPickOrOmitToNodeType(baseObject, operation, properties);

    expect(result).toStrictEqual(filteredObject);
  });
});

describe("applyPartialOrRequiredToNodeType", () => {
  test("Partial - Makes required properties optional", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: false },
        bar: { node: { type: "string" }, required: true },
      },
      additionalProperties: false,
    };

    const modifiedObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: false },
        bar: { node: { type: "string" }, required: false },
      },
      additionalProperties: false,
    };

    const result = applyPartialOrRequiredToNodeType(baseObject, false);

    expect(result).toStrictEqual(modifiedObject);
  });

  test("Required - Makes optional properties required", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: false },
        bar: { node: { type: "string" }, required: true },
      },
      additionalProperties: false,
    };

    const modifiedObject: NodeType = {
      type: "object",
      properties: {
        foo: { node: { type: "string" }, required: true },
        bar: { node: { type: "string" }, required: true },
      },
      additionalProperties: false,
    };

    const result = applyPartialOrRequiredToNodeType(baseObject, true);

    expect(result).toStrictEqual(modifiedObject);
  });
});
