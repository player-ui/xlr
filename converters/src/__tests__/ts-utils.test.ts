import { test, expect, describe } from "vitest";
import * as ts from "typescript";

import {
  tsStripOptionalType,
  isExportedDeclaration,
  getStringLiteralsFromUnion,
  isOptionalProperty,
  buildTemplateRegex,
  getReferencedType,
  isExportedModuleDeclaration,
  isNodeExported,
  isTypeScriptLibType,
} from "../ts/ts-utils";
import { ScriptTarget } from "typescript";

/** Create a TypeChecker from a source string (single file). */
function createChecker(source: string, fileName = "test.ts"): ts.TypeChecker {
  const sourceFile = ts.createSourceFile(
    fileName,
    source,
    ts.ScriptTarget.Latest,
    true,
  );
  const defaultHost = ts.createCompilerHost({});
  const host: ts.CompilerHost = {
    ...defaultHost,
    getSourceFile: (name) =>
      name === fileName
        ? sourceFile
        : defaultHost.getSourceFile(name, ScriptTarget.ESNext),
    writeFile: () => {},
    getCurrentDirectory: () => "",
    readFile: () => "",
  };
  const program = ts.createProgram([fileName], {}, host);
  return program.getTypeChecker();
}

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

test("tsStripOptionalType strips optional type", () => {
  const inner = ts.factory.createKeywordTypeNode(ts.SyntaxKind.StringKeyword);
  const optional = ts.factory.createOptionalTypeNode(inner);
  const actual = tsStripOptionalType(optional);
  expect(actual).toBe(inner);
  expect(ts.isOptionalTypeNode(actual)).toBe(false);
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

  test("should return false for node without modifiers", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "const x = 1;",
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;
    expect(ts.canHaveModifiers(node)).toBe(true);
    const result = isExportedDeclaration(node);
    expect(result).toBe(false);
  });
});

describe("isExportedModuleDeclaration", () => {
  test("returns true for exported module declaration", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "export module M { }",
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;
    expect(isExportedModuleDeclaration(node)).toBe(true);
    expect(ts.isModuleDeclaration(node)).toBe(true);
  });

  test("returns false for non-exported module declaration", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "module M { }",
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;
    expect(isExportedModuleDeclaration(node)).toBe(false);
  });

  test("returns false for exported non-module declaration", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "export interface I { }",
      ts.ScriptTarget.Latest,
      true,
    );
    const node = source.statements[0] as ts.Statement;
    expect(isExportedModuleDeclaration(node)).toBe(false);
  });
});

describe("isNodeExported", () => {
  test("returns true when node has Export modifier", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "export interface I { x: number; }",
      ts.ScriptTarget.Latest,
      true,
    );
    const decl = source.statements[0] as ts.InterfaceDeclaration;
    expect(isNodeExported(decl)).toBe(true);
  });

  test("returns false when node is nested and has no Export modifier", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "namespace N { interface I { x: number; } }",
      ts.ScriptTarget.Latest,
      true,
    );
    const mod = source.statements[0] as ts.ModuleDeclaration;
    const body = mod.body as ts.ModuleBlock;
    const decl = body.statements[0] as ts.InterfaceDeclaration;
    expect(isNodeExported(decl)).toBe(false);
  });

  test("returns true when parent is SourceFile (top-level)", () => {
    const source = ts.createSourceFile(
      "test.ts",
      "type T = string;",
      ts.ScriptTarget.Latest,
      true,
    );
    const decl = source.statements[0];
    expect(decl.parent?.kind).toBe(ts.SyntaxKind.SourceFile);
    expect(isNodeExported(decl)).toBe(true);
  });
});

describe("getReferencedType", () => {
  test("returns declaration and exported for interface reference", () => {
    const source = `interface Foo { a: number }
type Bar = Foo;`;
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[1] as ts.TypeAliasDeclaration;
    const typeRef = typeAlias.type as ts.TypeReferenceNode;
    const result = getReferencedType(typeRef, checker);
    expect(result).toBeDefined();
    expect(result!.declaration.kind).toBe(ts.SyntaxKind.InterfaceDeclaration);
    expect(ts.isInterfaceDeclaration(result!.declaration)).toBe(true);
    expect(result!.exported).toBe(true);
  });

  test("returns declaration for type alias reference", () => {
    const source = `type Foo = { a: number };
type Bar = Foo;`;
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[1] as ts.TypeAliasDeclaration;
    const typeRef = typeAlias.type as ts.TypeReferenceNode;
    const result = getReferencedType(typeRef, checker);
    expect(result).toBeDefined();
    expect(result!.declaration.kind).toBe(ts.SyntaxKind.TypeAliasDeclaration);
    expect(ts.isTypeAliasDeclaration(result!.declaration)).toBe(true);
  });

  test("returns undefined when reference is to class (not interface/type alias)", () => {
    const source = `class C { }
type Bar = C;`;
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[1] as ts.TypeAliasDeclaration;
    const typeRef = typeAlias.type as ts.TypeReferenceNode;
    const result = getReferencedType(typeRef, checker);
    expect(result).toBeUndefined();
  });
});

describe("isTypeScriptLibType", () => {
  test("returns true for Promise (lib type)", () => {
    const source = "type T = Promise<string>;";
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[0] as ts.TypeAliasDeclaration;
    const typeRef = typeAlias.type as ts.TypeReferenceNode;
    expect(isTypeScriptLibType(typeRef, checker)).toBe(true);
  });

  test("returns false for user-defined type", () => {
    const source = "interface Foo { } type T = Foo;";
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[1] as ts.TypeAliasDeclaration;
    const typeRef = typeAlias.type as ts.TypeReferenceNode;
    expect(isTypeScriptLibType(typeRef, checker)).toBe(false);
  });

  test("returns false when symbol is undefined", () => {
    const source = "type T = NonExistent;";
    const checker = createChecker(source);
    const typeRef = ts.factory.createTypeReferenceNode(
      ts.factory.createIdentifier("NonExistent"),
    );
    expect(isTypeScriptLibType(typeRef, checker)).toBe(false);
  });
});

describe("getStringLiteralsFromUnion", () => {
  test("extracts string literals from union type node", () => {
    const input: ts.Node = ts.factory.createUnionTypeNode([
      ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral("foo")),
      ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral("bar")),
    ]);
    const expected: Set<string> = new Set(["foo", "bar"]);
    const actual = getStringLiteralsFromUnion(input);
    expect(actual).toEqual(expected);
  });

  test("returns single string literal from LiteralTypeNode", () => {
    const input = ts.factory.createLiteralTypeNode(
      ts.factory.createStringLiteral("only"),
    );
    expect(getStringLiteralsFromUnion(input)).toEqual(new Set(["only"]));
  });

  test("returns empty set for non-union non-literal node", () => {
    const input = ts.factory.createKeywordTypeNode(ts.SyntaxKind.StringKeyword);
    expect(getStringLiteralsFromUnion(input)).toEqual(new Set());
  });

  test("union with non-string literal yields empty string in set", () => {
    const input = ts.factory.createUnionTypeNode([
      ts.factory.createLiteralTypeNode(ts.factory.createStringLiteral("a")),
      ts.factory.createLiteralTypeNode(ts.factory.createNumericLiteral(42)),
    ]);
    const actual = getStringLiteralsFromUnion(input);
    expect(actual).toEqual(new Set(["a", ""]));
  });
});

describe("buildTemplateRegex", () => {
  test("builds regex for template literal with string, number, boolean", () => {
    const source = "type T = `pre${string}mid${number}suf`;";
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[0] as ts.TypeAliasDeclaration;
    const templateNode = typeAlias.type as ts.TemplateLiteralTypeNode;
    const regex = buildTemplateRegex(templateNode, checker);
    expect(regex).toBe("pre.*mid[0-9]*suf");
  });

  test("template with one string span", () => {
    const source = "type T = `prefix-${string}-suffix`;";
    const checker = createChecker(source);
    const sourceFile = ts.createSourceFile(
      "test.ts",
      source,
      ts.ScriptTarget.Latest,
      true,
    );
    const typeAlias = sourceFile.statements[0] as ts.TypeAliasDeclaration;
    const templateNode = typeAlias.type as ts.TemplateLiteralTypeNode;
    const regex = buildTemplateRegex(templateNode, checker);
    expect(regex).toBe("prefix-.*-suffix");
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
