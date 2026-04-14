/* eslint-disable prettier/prettier */
import type {
  Manifest,
  NamedType,
  NodeType,
  ObjectNode,
  ObjectType,
  TransformFunction,
  TSManifest,
} from "@xlr-lib/xlr";
import {
  computeEffectiveObject,
  resolveConditional,
  resolveReferenceNode,
} from "@xlr-lib/xlr-utils";
import { fillInGenerics } from "@xlr-lib/xlr-utils";
import type { Node } from "jsonc-parser";
import fs from "fs";
import path from "path";

import type { XLRRegistry, Filters } from "./registry";
import { BasicXLRRegistry } from "./registry";
import { XLRValidator } from "./validator";
import { TransformFunctionMap, xlrTransformWalker } from "./utils";

export interface GetTypeOptions {
  /** Resolves `extends` fields in objects */
  getRawType?: boolean;
  /** Perform optimizations to resolve all references, type intersections, and conditionals */
  optimize?: boolean;
}

/**
 * Abstraction for interfacing with XLRs making it more approachable to use without understanding the inner workings of the types and how they are packaged
 */
export class XLRSDK {
  private registry: XLRRegistry;
  private validator: XLRValidator;
  private computedNodeCache: Map<string, NodeType>;
  private externalTransformFunctions: Map<string, TransformFunction>;

  constructor(customRegistry?: XLRRegistry) {
    this.registry = customRegistry ?? new BasicXLRRegistry();
    this.validator = new XLRValidator(this.getType.bind(this));
    this.computedNodeCache = new Map();
    this.externalTransformFunctions = new Map();
  }

  /**
   * Loads definitions from a path on the filesystem
   *
   * @param inputPath - path to the directory to load (above the xlr folder)
   * @param filters - Any filters to apply when loading the types (a positive match will omit)
   * @param transforms - any transforms to apply to the types being loaded
   */
  public loadDefinitionsFromDisk(
    inputPath: string,
    filters?: Omit<Filters, "pluginFilter">,
    transforms?: Array<TransformFunction>
  ) {
    this.computedNodeCache.clear();

    const transformsToRun = [
      ...this.externalTransformFunctions.values(),
      ...(transforms ?? []),
    ];

    const manifest = JSON.parse(
      fs.readFileSync(path.join(inputPath, "xlr", "manifest.json")).toString(),
      (key: unknown, value: unknown) => {
        // Custom parser because JSON objects -> JS Objects, not maps
        if (typeof value === "object" && value !== null) {
          if (key === "capabilities") {
            return new Map(Object.entries(value));
          }
        }

        return value;
      }
    ) as Manifest;

    manifest.capabilities?.forEach((capabilityList, capabilityName) => {
      if (
        filters?.capabilityFilter &&
        capabilityName.match(filters?.capabilityFilter)
      )
        return;
      capabilityList.forEach((extensionName) => {
        if (!filters?.typeFilter || !extensionName.match(filters?.typeFilter)) {
          const cType: NamedType<NodeType> = JSON.parse(
            fs
              .readFileSync(
                path.join(inputPath, "xlr", `${extensionName}.json`)
              )
              .toString()
          );
          const effectiveType =
            transformsToRun?.reduce(
              (typeAccumulator: NamedType<NodeType>, transformFn) =>
                transformFn(
                  typeAccumulator,
                  capabilityName
                ) as NamedType<NodeType>,
              cType
            ) ?? cType;

          this.registry.add(effectiveType, manifest.pluginName, capabilityName);
        }
      });
    });
  }

  /**
   * Load definitions from a js/ts file in memory
   *
   * @param manifest - The imported XLR manifest module
   * @param filters - Any filters to apply when loading the types (a positive match will omit)
   * @param transforms - any transforms to apply to the types being loaded
   */
  public async loadDefinitionsFromModule(
    manifest: TSManifest,
    filters?: Omit<Filters, "pluginFilter">,
    transforms?: Array<TransformFunction>
  ) {
    this.computedNodeCache.clear();

    const transformsToRun = [
      ...this.externalTransformFunctions.values(),
      ...(transforms ?? []),
    ];

    Object.keys(manifest.capabilities)?.forEach((capabilityName) => {
      if (
        filters?.capabilityFilter &&
        capabilityName.match(filters?.capabilityFilter)
      )
        return;
      const capabilityList = manifest.capabilities[capabilityName];
      capabilityList.forEach((extension) => {
        if (
          !filters?.typeFilter ||
          !extension.name.match(filters?.typeFilter)
        ) {
          const effectiveType =
            transformsToRun?.reduce(
              (typeAccumulator: NamedType<NodeType>, transformFn) =>
                transformFn(
                  typeAccumulator,
                  capabilityName
                ) as NamedType<NodeType>,
              extension
            ) ?? extension;

          this.registry.add(effectiveType, manifest.pluginName, capabilityName);
        }
      });
    });
  }

  /**
   * Statically load transform function that should be applied to every XLR bundle that is imported
   */
  public addTransformFunction(name: string, fn: TransformFunction): void {
    this.externalTransformFunctions.set(name, fn);
  }

  /**
   * Remove any transform function loaded via the `addTransformFunction` method by name
   */
  public removeTransformFunction(name: string): void {
    this.externalTransformFunctions.delete(name);
  }

  /**
   * Returns a Type that has been previously loaded
   *
   * @param id - Type to retrieve
   * @param options - `GetTypeOptions`
   * @returns `NamedType<NodeType>` | `undefined`
   */
  public getType(
    id: string,
    options?: GetTypeOptions
  ): NamedType<NodeType> | undefined {
    let type = this.registry.get(id);
    if (options?.getRawType === true || !type) {
      return type;
    }

    if (this.computedNodeCache.has(id)) {
      return JSON.parse(JSON.stringify(this.computedNodeCache.get(id))) as
        | NamedType<NodeType>
        | undefined;
    }

    type = this.resolveType(type, options?.optimize)

    this.computedNodeCache.set(id, type);

    return type;
  }

  /**
   * Returns if a Type with `id` has been loaded into the DSK
   *
   * @param id - Type to retrieve
   * @returns `boolean`
   */
  public hasType(id: string) {
    return this.registry.has(id);
  }

  /**
   * Lists types that have been loaded into the SDK
   *
   * @param filters - Any filters to apply to the types returned (a positive match will omit)
   * @returns `Array<NamedTypes>`
   */
  public listTypes(filters?: Filters) {
    return this.registry.list(filters);
  }

  /**
   * Returns meta information around a registered type
   *
   * @param id - Name of Type to retrieve
   * @returns `TypeMetaData` | `undefined`
   */
  public getTypeInfo(id: string) {
    return this.registry.info(id);
  }

  /**
   * Validates if a JSONC Node follows the XLR Type registered under the `typeName` specified
   *
   * @param typeName - Registered XLR Type to use for validation
   * @param rootNode - Node to validate
   * @returns `Array<ValidationErrors>`
   */
  public validateByName(typeName: string, rootNode: Node) {
    const xlr = this.getType(typeName);
    if (!xlr) {
      throw new Error(
        `Type ${typeName} does not exist in registry, can't validate`
      );
    }

    return this.validator.validateType(rootNode, xlr);
  }

  /**
   * Validates if a JSONC Node follows the supplied XLR Type
   *
   * @param type - Type to validate against
   * @param rootNode - Node to validate
   * @returns `Array<ValidationErrors>`
   */
  public validateByType(type: NodeType, rootNode: Node) {
    return this.validator.validateType(rootNode, type);
  }

  /**
   * Transforms a generated XLR node into its final representation by resolving all `extends` properties.
   * If `optimize` is set to true the following operations are also performed:
   *  - Solving any conditional types
   *  - Computing the effective types of any union elements
   *  - Resolving any ref nodes
   *  - filing in any remaining generics with their default value
   */
  private resolveType(type: NamedType, optimize = true): NamedType {
    const resolvedObject = fillInGenerics(type);

    let transformMap: TransformFunctionMap = {
      object: [(objectNode: ObjectType) => {
        if (objectNode.extends) {
          const refName = objectNode.extends.ref.split("<")[0];
          let extendedType = this.getType(refName, {getRawType: true});
          if (!extendedType) {
            throw new Error(
              `Error resolving ${objectNode.name}: can't find extended type ${refName}`
            );
          }

          extendedType = resolveReferenceNode(
            objectNode.extends,
            extendedType as NamedType<ObjectType>
          ) as NamedType;
          if (extendedType.type === "object") {
            return {
              ...computeEffectiveObject(
                extendedType as ObjectType,
                objectNode as ObjectType,
                false
              ),
              name: objectNode.name,
              description: objectNode.description,
            };
          }

          if( extendedType.type === "or"){
            return {
              ...this.validator.computeIntersectionType([
                objectNode,
                extendedType
              ]
              ),
              name: objectNode.name,
              description: objectNode.description,
            } as any;
          }

          // if the merge isn't straightforward, defer until validation time for now
          return {
            name: objectNode.name,
            type: "and",
            and: [
              {
                ...objectNode,
                extends: undefined,
              },
              extendedType,
            ],
          } as unknown as ObjectNode;
        }

        return objectNode;
      }],
    } 

    if(optimize){
      transformMap = {
        ...transformMap,
        conditional: [(node) => {
          return resolveConditional(node) as any
        }],
        and: [(node) => {
          return {
            ...this.validator.computeIntersectionType(node.and),
            ...(node.name ? { name: node.name } : {}),
          } as any
        }],
        ref: [(refNode) => {
          return this.validator.getRefType(refNode) as any
        }]
      }
    }

    return xlrTransformWalker(transformMap)(resolvedObject) as NamedType
  }

}
