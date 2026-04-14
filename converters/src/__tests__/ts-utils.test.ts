import { test, expect, describe } from "vitest";
import * as ts from "typescript";

import {
  tsStripOptionalType,
  isExportedDeclaration,
  getStringLiteralsFromUnion,
  isOptionalProperty,
} from "../ts/ts-utils";

test("tsStripOptionalType", () => {
  const input: ts.TypeNode = ts.factory.createKeywordTypeNode(
    ts.SyntaxKind.StringKeyword,
  );
  const expected: ts.TypeNode = ts.factory.createKeywordTypeNode(
    ts.SyntaxKind.StringKeyword,
  );
  const actual = tsStripOptionalType(input);
  expect(actual).toEqual(expected);
});

describe("isExportedDeclaration", () => {
  test("should return false for a non exported Statement", () => {
    const source = ts.createSourceFile(
      "test.ts",
      `
        interface Test {
          prop?: string;
        }
      `,
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;

    const result = isExportedDeclaration(node);
    expect(result).toBe(false);
  });

  test("should return true for an exported Statement", () => {
    const source = ts.createSourceFile(
      "test.ts",
      `
          export interface Test {
            prop?: string;
          }
        `,
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;

    const result = isExportedDeclaration(node);
    expect(result).toBe(true);
  });
});

describe("isOptionalProperty", () => {
  test("should return true for an optional property", () => {
    const source = ts.createSourceFile(
      "test.ts",
      `
      interface Test {
        prop?: string;
      }
    `,
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.InterfaceDeclaration;
    const prop = node.members[0] as ts.PropertySignature;
    expect(isOptionalProperty(prop)).toBe(true);
  });

  test("should return false for a non-optional property", () => {
    const source = ts.createSourceFile(
      "test.ts",
      `
      interface Test {
        prop: string;
      }
    `,
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.InterfaceDeclaration;
    const prop = node.members[0] as ts.PropertySignature;
    expect(isOptionalProperty(prop)).toBe(false);
  });
});

test("getStringLiteralsFromUnion", () => {
  const input: ts.Node = ts.factory.createUnionTypeNode([
    ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral("foo")),
    ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral("bar")),
  ]);
  const expected: Set<string> = new Set(["foo", "bar"]);
  const actual = getStringLiteralsFromUnion(input);
  expect(actual).toEqual(expected);
});
