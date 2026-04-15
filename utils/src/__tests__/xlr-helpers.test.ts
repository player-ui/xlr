import { test, expect, describe } from "vitest";
import {
  NodeType,
  NodeTypeWithGenerics,
  ObjectNode,
  OrType,
} from "@xlr-lib/xlr";

import {
  applyPickOrOmitToNodeType,
  applyPartialOrRequiredToNodeType,
  fillInGenerics,
  applyExcludeToNodeType,
} from "../xlr-helpers";

describe("fillInGenerics", () => {
  test("returns primitive node unchanged when no generics", () => {
    const node: NodeType = { type: "string" };
    expect(fillInGenerics(node)).toBe(node);
  });

  test("fills ref with value from generics map", () => {
    const refNode: NodeType = {
      type: "ref",
      ref: "T",
    };
    const generics = new Map<string, NodeType>([["T", { type: "string" }]]);
    const result = fillInGenerics(refNode, generics);
    expect(result).toEqual({ type: "string" });
  });

  test("ref with genericArguments fills each argument", () => {
    const refNode: NodeType = {
      type: "ref",
      ref: "Outer",
      genericArguments: [{ type: "ref", ref: "T" }],
    };
    const generics = new Map<string, NodeType>([["T", { type: "number" }]]);
    const result = fillInGenerics(refNode, generics);
    expect(result).toMatchObject({
      type: "ref",
      ref: "Outer",
      genericArguments: [{ type: "number" }],
    });
  });

  test("ref not in map returns ref with filled genericArguments", () => {
    const refNode: NodeType = {
      type: "ref",
      ref: "Outer",
      genericArguments: [{ type: "ref", ref: "T" }],
    };
    const generics = new Map<string, NodeType>([["T", { type: "boolean" }]]);
    const result = fillInGenerics(refNode, generics);
    expect(result).toMatchObject({
      type: "ref",
      ref: "Outer",
      genericArguments: [{ type: "boolean" }],
    });
  });

  test("object properties are filled recursively", () => {
    const obj: NodeType = {
      type: "object",
      properties: {
        p: { required: true, node: { type: "ref", ref: "T" } },
      },
      additionalProperties: false,
    };
    const generics = new Map<string, NodeType>([["T", { type: "string" }]]);
    const result = fillInGenerics(obj, generics);
    expect(result).toMatchObject({
      type: "object",
      properties: {
        p: { required: true, node: { type: "string" } },
      },
    });
  });

  test("array elementType is filled", () => {
    const arr: NodeType = {
      type: "array",
      elementType: { type: "ref", ref: "T" },
    };
    const generics = new Map<string, NodeType>([["T", { type: "number" }]]);
    const result = fillInGenerics(arr, generics);
    expect(result).toEqual({
      type: "array",
      elementType: { type: "number" },
    });
  });

  test("or type members are filled", () => {
    const orNode: NodeType = {
      type: "or",
      or: [{ type: "ref", ref: "T" }, { type: "string" }],
    };
    const generics = new Map<string, NodeType>([["T", { type: "number" }]]);
    const result = fillInGenerics(orNode, generics);
    expect(result).toMatchObject({
      type: "or",
      or: [{ type: "number" }, { type: "string" }],
    });
  });

  test("and type members are filled", () => {
    const andNode: NodeType = {
      type: "and",
      and: [
        { type: "object", properties: {}, additionalProperties: false },
        { type: "ref", ref: "T" },
      ],
    };
    const generics = new Map<string, NodeType>([["T", { type: "null" }]]);
    const result = fillInGenerics(andNode, generics);
    expect(result.type).toBe("and");
    expect((result as { and: NodeType[] }).and).toHaveLength(2);
    expect((result as { and: NodeType[] }).and[1]).toEqual({ type: "null" });
  });

  test("record keyType and valueType are filled", () => {
    const recordNode: NodeType = {
      type: "record",
      keyType: { type: "ref", ref: "K" },
      valueType: { type: "ref", ref: "V" },
    };
    const generics = new Map<string, NodeType>([
      ["K", { type: "string" }],
      ["V", { type: "number" }],
    ]);
    const result = fillInGenerics(recordNode, generics);
    expect(result).toEqual({
      type: "record",
      keyType: { type: "string" },
      valueType: { type: "number" },
    });
  });

  test("generic node without map builds defaults from genericTokens", () => {
    const genericObj: NodeTypeWithGenerics<ObjectNode> = {
      type: "object",
      properties: {},
      additionalProperties: false,
      genericTokens: [
        {
          symbol: "T",
          default: { type: "string" },
          constraints: undefined,
        },
      ],
    };
    const result = fillInGenerics(genericObj);
    expect(result).toMatchObject({ type: "object" });
  });

  test("conditional type with both sides non-ref resolves via resolveConditional", () => {
    const conditionalNode: NodeType = {
      type: "conditional",
      check: {
        left: { type: "string" },
        right: { type: "string" },
      },
      value: {
        true: { type: "number" },
        false: { type: "boolean" },
      },
    };
    const result = fillInGenerics(conditionalNode);
    expect(result).toEqual({ type: "number" });
  });

  test("conditional type with ref in check returns unresolved conditional", () => {
    const conditionalNode: NodeType = {
      type: "conditional",
      check: {
        left: { type: "ref", ref: "T" },
        right: { type: "string" },
      },
      value: {
        true: { type: "number" },
        false: { type: "boolean" },
      },
    };
    const result = fillInGenerics(conditionalNode);
    expect(result).toMatchObject({
      type: "conditional",
      check: { left: { type: "ref", ref: "T" }, right: { type: "string" } },
      value: { true: { type: "number" }, false: { type: "boolean" } },
    });
  });

  test("object with genericTokens and extends fills constraints and extends", () => {
    const objWithExtends: NodeTypeWithGenerics<ObjectNode> = {
      type: "object",
      properties: { p: { required: false, node: { type: "ref", ref: "T" } } },
      additionalProperties: false,
      genericTokens: [
        { symbol: "T", default: { type: "string" }, constraints: undefined },
      ],
      extends: { type: "ref", ref: "Base" },
    };
    const generics = new Map<string, NodeType>([
      ["T", { type: "number" }],
      ["Base", { type: "object", properties: {}, additionalProperties: false }],
    ]);
    const result = fillInGenerics(objWithExtends, generics);
    expect(result).toMatchObject({
      type: "object",
      properties: { p: { node: { type: "number" } } },
      extends: { type: "object" },
    });
  });
});

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

  test("Pick - no matching properties returns undefined", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: { foo: { node: { type: "string" }, required: false } },
      additionalProperties: false,
    };
    const result = applyPickOrOmitToNodeType(
      baseObject,
      "Pick",
      new Set(["bar"]),
    );
    expect(result).toBeUndefined();
  });

  test("Omit - all properties with additionalProperties false returns undefined", () => {
    const baseObject: NodeType = {
      type: "object",
      properties: { foo: { node: { type: "string" }, required: false } },
      additionalProperties: false,
    };
    const result = applyPickOrOmitToNodeType(
      baseObject,
      "Omit",
      new Set(["foo"]),
    );
    expect(result).toBeUndefined();
  });

  test("and type - applies to each member and returns and", () => {
    const baseObject: NodeType = {
      type: "and",
      and: [
        {
          type: "object",
          properties: {
            a: { node: { type: "string" }, required: false },
            b: { node: { type: "string" }, required: false },
          },
          additionalProperties: false,
        },
        {
          type: "object",
          properties: {
            b: { node: { type: "string" }, required: false },
            c: { node: { type: "string" }, required: false },
          },
          additionalProperties: false,
        },
      ],
    };
    const result = applyPickOrOmitToNodeType(
      baseObject,
      "Pick",
      new Set(["b"]),
    );
    expect(result).toMatchObject({ type: "and" });
    const and = (result as { and: NodeType[] }).and;
    expect(and).toHaveLength(2);
    expect(and[0]).toMatchObject({
      type: "object",
      properties: { b: { node: { type: "string" }, required: false } },
    });
    expect(and[1]).toMatchObject({
      type: "object",
      properties: { b: { node: { type: "string" }, required: false } },
    });
  });

  test("or type - applies to each member and returns or", () => {
    const baseObject: NodeType = {
      type: "or",
      or: [
        {
          type: "object",
          properties: {
            x: { node: { type: "string" }, required: false },
            y: { node: { type: "string" }, required: false },
          },
          additionalProperties: false,
        },
      ],
    };
    const result = applyPickOrOmitToNodeType(
      baseObject,
      "Pick",
      new Set(["x"]),
    );
    expect(result).toMatchObject({
      type: "object",
      properties: { x: { node: { type: "string" }, required: false } },
    });
  });

  test("or type with multiple members returns single or", () => {
    const baseObject: NodeType = {
      type: "or",
      or: [
        {
          type: "object",
          properties: { a: { node: { type: "string" }, required: false } },
          additionalProperties: false,
        },
        {
          type: "object",
          properties: { b: { node: { type: "string" }, required: false } },
          additionalProperties: false,
        },
      ],
    };
    const result = applyPickOrOmitToNodeType(
      baseObject,
      "Pick",
      new Set(["a", "b"]),
    );
    expect(result).toMatchObject({ type: "or", or: expect.any(Array) });
    expect((result as { or: NodeType[] }).or.length).toBe(2);
  });

  test("throws when applying Pick/Omit to non-object non-union non-intersection", () => {
    const baseObject: NodeType = { type: "string" };
    expect(() =>
      applyPickOrOmitToNodeType(baseObject, "Pick", new Set(["x"])),
    ).toThrow(/Can not apply Pick to type string/);
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

  test("and type - applies modifier to each member", () => {
    const baseObject: NodeType = {
      type: "and",
      and: [
        {
          type: "object",
          properties: { a: { node: { type: "string" }, required: false } },
          additionalProperties: false,
        },
      ],
    };
    const result = applyPartialOrRequiredToNodeType(baseObject, true);
    expect(result).toMatchObject({ type: "and" });
    expect((result as { and: NodeType[] }).and[0]).toMatchObject({
      type: "object",
      properties: { a: { required: true, node: { type: "string" } } },
    });
  });

  test("or type - applies modifier to each member", () => {
    const baseObject: NodeType = {
      type: "or",
      or: [
        {
          type: "object",
          properties: { a: { node: { type: "string" }, required: true } },
          additionalProperties: false,
        },
      ],
    };
    const result = applyPartialOrRequiredToNodeType(baseObject, false);
    expect(result).toMatchObject({ type: "or" });
    expect((result as { or: NodeType[] }).or[0]).toMatchObject({
      type: "object",
      properties: { a: { required: false, node: { type: "string" } } },
    });
  });

  test("throws when applying Partial/Required to non-object non-union non-intersection", () => {
    const baseObject: NodeType = { type: "number" };
    expect(() => applyPartialOrRequiredToNodeType(baseObject, false)).toThrow(
      /Can not apply Partial to type number/,
    );
  });
});

describe("applyExcludeToNodeType", () => {
  test("excludes single type from union", () => {
    const baseObject: OrType = {
      type: "or",
      or: [{ type: "string" }, { type: "number" }, { type: "boolean" }],
    };
    const result = applyExcludeToNodeType(baseObject, { type: "number" });
    expect(result).toMatchObject({
      type: "or",
      or: [{ type: "string" }, { type: "boolean" }],
    });
  });

  test("excludes with filter union (or)", () => {
    const baseObject: OrType = {
      type: "or",
      or: [{ type: "string" }, { type: "number" }, { type: "boolean" }],
    };
    const filters: OrType = {
      type: "or",
      or: [{ type: "number" }, { type: "boolean" }],
    };
    const result = applyExcludeToNodeType(baseObject, filters);
    expect(result).toEqual({ type: "string" });
  });

  test("single remaining member returns that member", () => {
    const baseObject: OrType = {
      type: "or",
      or: [{ type: "string" }, { type: "number" }],
    };
    const result = applyExcludeToNodeType(baseObject, { type: "number" });
    expect(result).toEqual({ type: "string" });
  });

  test("multiple remaining members returns or", () => {
    const baseObject: OrType = {
      type: "or",
      or: [{ type: "string" }, { type: "number" }, { type: "boolean" }],
    };
    const result = applyExcludeToNodeType(baseObject, { type: "number" });
    expect(result).toMatchObject({
      type: "or",
      or: [{ type: "string" }, { type: "boolean" }],
    });
  });
});
