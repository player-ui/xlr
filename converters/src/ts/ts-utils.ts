import ts from "typescript";

/**
 * Returns if the Object Property is optional
 */
export function isOptionalProperty(node: ts.PropertySignature): boolean {
  return node.questionToken?.kind === ts.SyntaxKind.QuestionToken;
}

/**
 * Returns if the node is an Interface or Type with Generics
 */
export function isGenericInterfaceDeclaration(
  node: ts.InterfaceDeclaration,
): boolean {
  const length = node.typeParameters?.length;
  return length ? length > 0 : false;
}

/**
 * Returns if the node is an Type Declaration with Generics
 */
export function isGenericTypeDeclaration(
  node: ts.TypeAliasDeclaration,
): boolean {
  const length = node.typeParameters?.length;
  return length ? length > 0 : false;
}

/**
 * Returns if the referenced type is a generic
 */
export function isTypeReferenceGeneric(
  node: ts.TypeReferenceNode,
  typeChecker: ts.TypeChecker,
): boolean {
  const symbol = typeChecker.getSymbolAtLocation(node.typeName);
  if (symbol && symbol.declarations) {
    return symbol.declarations[0].kind === ts.SyntaxKind.TypeParameter;
  }

  return false;
}

export type TopLevelDeclaration =
  | ts.InterfaceDeclaration
  | ts.TypeAliasDeclaration;

/**
 * Returns if the node is an interface or a type declaration
 */
export function isTopLevelDeclaration(
  node: ts.Node,
): node is TopLevelDeclaration {
  return (
    node.kind === ts.SyntaxKind.InterfaceDeclaration ||
    node.kind === ts.SyntaxKind.TypeAliasDeclaration
  );
}

export type TopLevelNode = TopLevelDeclaration | ts.VariableStatement;

/**
 * Returns if the node is an interface or a type declaration
 */
export function isTopLevelNode(node: ts.Node): node is TopLevelNode {
  return (
    node.kind === ts.SyntaxKind.InterfaceDeclaration ||
    node.kind === ts.SyntaxKind.TypeAliasDeclaration ||
    node.kind === ts.SyntaxKind.VariableStatement
  );
}

/**
 * Returns the required type or the optionally required type
 */
export function tsStripOptionalType(node: ts.TypeNode): ts.TypeNode {
  return ts.isOptionalTypeNode(node) ? node.type : node;
}

/**
 * Returns if the top level declaration is exported
 */
export function isExportedDeclaration(node: ts.Statement): boolean {
  const modifiers = ts.canHaveModifiers(node)
    ? ts.getModifiers(node)
    : undefined;

  if (modifiers) {
    return modifiers.some((m) => m.kind === ts.SyntaxKind.ExportKeyword);
  }
  return false;
}

/**
 * Returns if the node is exported from the source file
 */
export function isNodeExported(node: ts.Node): boolean {
  return (
    (ts.getCombinedModifierFlags(node as ts.Declaration) &
      ts.ModifierFlags.Export) !==
      0 ||
    (!!node.parent && node.parent.kind === ts.SyntaxKind.SourceFile)
  );
}

/**
 * Returns the actual type and will following import chains if needed
 */
export function getReferencedType(
  node: ts.TypeReferenceNode,
  typeChecker: ts.TypeChecker,
):
  | {
      declaration: ts.TypeAliasDeclaration | ts.InterfaceDeclaration;
      exported: boolean;
    }
  | undefined {
  let symbol = typeChecker.getSymbolAtLocation(node.typeName);

  if (
    symbol &&
    (symbol.flags & ts.SymbolFlags.Alias) === ts.SymbolFlags.Alias
  ) {
    // follow alias if it is a symbol
    symbol = typeChecker.getAliasedSymbol(symbol);
  }

  const varDecl = symbol?.declarations?.[0];
  if (
    varDecl &&
    (ts.isInterfaceDeclaration(varDecl) || ts.isTypeAliasDeclaration(varDecl))
  ) {
    return { declaration: varDecl, exported: isNodeExported(varDecl) };
  }
}

/**
 * Checks if a type reference points to a TypeScript built-in type
 * by examining whether its declaration comes from TypeScript's lib files.
 *
 * This is more robust than maintaining a hardcoded list of built-in types
 * as it automatically handles all TypeScript lib types (Map, Set, WeakMap,
 * Promise, Array, Date, Error, RegExp, etc.).
 */
export function isTypeScriptLibType(
  node: ts.TypeReferenceNode,
  typeChecker: ts.TypeChecker,
): boolean {
  let symbol = typeChecker.getSymbolAtLocation(node.typeName);

  if (!symbol) return false;

  // Follow alias if it is a symbol
  if ((symbol.flags & ts.SymbolFlags.Alias) === ts.SymbolFlags.Alias) {
    symbol = typeChecker.getAliasedSymbol(symbol);
  }

  const declarations = symbol.getDeclarations();
  if (!declarations || declarations.length === 0) return false;

  // Check if any declaration comes from TypeScript lib files
  return declarations.some((decl) => {
    const sourceFile = decl.getSourceFile();
    if (!sourceFile) return false;

    const filePath = sourceFile.fileName;
    return (
      filePath.includes("/typescript/lib/") ||
      filePath.includes("\\typescript\\lib\\") ||
      (filePath.endsWith(".d.ts") && filePath.includes("lib."))
    );
  });
}

/**
 * Returns list of string literals from potential union of strings
 */
export function getStringLiteralsFromUnion(node: ts.Node): Set<string> {
  if (ts.isUnionTypeNode(node)) {
    return new Set(
      node.types.map((type) => {
        if (ts.isLiteralTypeNode(type) && ts.isStringLiteral(type.literal)) {
          return type.literal.text;
        }

        return "";
      }),
    );
  }

  if (ts.isLiteralTypeNode(node) && ts.isStringLiteral(node.literal)) {
    return new Set([node.literal.text]);
  }

  return new Set();
}

/**
 * Converts a format string into a regex that can be used to validate a given string matches the template
 */
export function buildTemplateRegex(
  node: ts.TemplateLiteralTypeNode,
  typeChecker: ts.TypeChecker,
): string {
  let regex = node.head.text;
  node.templateSpans.forEach((span) => {
    // process template tag
    let type = span.type.kind;
    if (ts.isTypeReferenceNode(span.type)) {
      let symbol = typeChecker.getSymbolAtLocation(
        span.type.typeName,
      ) as ts.Symbol;

      if (
        symbol &&
        (symbol.flags & ts.SymbolFlags.Alias) === ts.SymbolFlags.Alias
      ) {
        // follow alias if it is a symbol
        symbol = typeChecker.getAliasedSymbol(symbol);
      }

      type = (symbol?.declarations?.[0] as ts.TypeAliasDeclaration).type.kind;
    }

    if (type === ts.SyntaxKind.StringKeyword) {
      regex += ".*";
    } else if (type === ts.SyntaxKind.NumberKeyword) {
      regex += "[0-9]*";
    } else if (type === ts.SyntaxKind.BooleanKeyword) {
      regex += "true|false";
    }

    // add non-tag element
    regex += span.literal.text;
  });
  return regex;
}

/**
 * Returns if the top level declaration is exported
 */
export function isExportedModuleDeclaration(
  node: ts.Statement,
): node is ts.ModuleDeclaration {
  return isExportedDeclaration(node) && ts.isModuleDeclaration(node);
}
