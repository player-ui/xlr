import type {
  NamedType,
  NamedTypeWithGenerics,
  NodeType,
  NodeTypeWithGenerics,
  PrimitiveTypes,
  StringType,
  NumberType,
  BooleanType,
  ObjectType,
  ArrayType,
  RefType,
  OrType,
  AndType,
  RecordType,
} from "@xlr-lib/xlr";

/**
 * Returns if the NodeType has generic tokens
 */
export function isGenericNodeType<T extends NodeType = NodeType>(
  nt: NodeType,
): nt is NodeTypeWithGenerics<T> {
  return (nt as NodeTypeWithGenerics).genericTokens?.length > 0;
}

/**
 * Returns if the named type has generic tokens
 */
export function isGenericNamedType<T extends NamedType = NamedType>(
  nt: NodeType,
): nt is NamedTypeWithGenerics<T> {
  return (nt as NamedTypeWithGenerics).genericTokens?.length > 0;
}

/**
 * Returns if the node is a `PrimitiveTypes`
 */
export function isPrimitiveTypeNode(node: NodeType): node is PrimitiveTypes {
  return (
    node.type === "string" ||
    node.type === "number" ||
    node.type === "boolean" ||
    node.type === "null" ||
    node.type === "any" ||
    node.type === "never" ||
    node.type === "undefined" ||
    node.type === "unknown" ||
    node.type === "void"
  );
}

/**
 * Type Guard for non-null values
 */
export function isNonNullable<T>(a: T | null | undefined): a is NonNullable<T> {
  return a !== null || a !== undefined;
}

/**
 * Type guard for string type nodes
 */
export function isStringType(node: NodeType): node is StringType {
  return node.type === "string";
}

/**
 * Type guard for number type nodes
 */
export function isNumberType(node: NodeType): node is NumberType {
  return node.type === "number";
}

/**
 * Type guard for boolean type nodes
 */
export function isBooleanType(node: NodeType): node is BooleanType {
  return node.type === "boolean";
}

/**
 * Type guard for object type nodes
 */
export function isObjectType(node: NodeType): node is ObjectType {
  return node.type === "object";
}

/**
 * Type guard for array type nodes
 */
export function isArrayType(node: NodeType): node is ArrayType {
  return node.type === "array";
}

/**
 * Type guard for ref type nodes
 */
export function isRefType(node: NodeType): node is RefType {
  return node.type === "ref";
}

/**
 * Type guard for or (union) type nodes
 */
export function isOrType(node: NodeType): node is OrType {
  return node.type === "or";
}

/**
 * Type guard for and (intersection) type nodes
 */
export function isAndType(node: NodeType): node is AndType {
  return node.type === "and";
}

/**
 * Type guard for record type nodes
 */
export function isRecordType(node: NodeType): node is RecordType {
  return node.type === "record";
}

/**
 * Type guard for named types (have name and source)
 */
export function isNamedType<T extends NodeType = NodeType>(
  node: NodeType,
): node is NamedType<T> {
  return "name" in node && "source" in node && typeof node.name === "string";
}
