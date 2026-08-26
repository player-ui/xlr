import type { NamedType, NodeType } from ".";

export type TransformFunction = (
  input: NamedType<NodeType> | NodeType,
  capabilityType: string,
) => NamedType | NodeType;

export interface Capability {
  /** Name of the capability that is provided to Player */
  name: string;
  /** List of XLRs that are provided for the Capability */
  provides: Array<string>;
}

export interface PlatformPackage {
  /** The package identifier for the platform: an npm name, a Pod name, or a Maven coordinate */
  name: string;
  /** The version of the package. */
  version?: string;
}

/**
 * The packages that implement a plugin's capabilities, keyed by platform.
 * A platform is absent when it does not provide them.
 */
export interface PlatformPackages {
  react?: PlatformPackage;
  ios?: PlatformPackage;
  android?: PlatformPackage;
}

export interface Manifest {
  /**
   * The version of the manifest format itself, unrelated to the version of any
   * package or of Player content schemas. Absent on manifests generated before
   * the field was introduced.
   */
  manifestVersion?: string;
  /** Name of the plugin */
  pluginName: string;
  /** The packages that implement these capabilities, keyed by platform */
  packages?: PlatformPackages;
  /** Map of capabilities provided by the plugin to the name of the XLR for the capabilities */
  capabilities?: Map<string, Array<string>>;
  /** CustomPrimitives that are the most basic types in the Payer Ecosystem */
  customPrimitives?: Array<string>;
}

export interface TSManifest {
  /**
   * The version of the manifest format itself, unrelated to the version of any
   * package or of Player content schemas. Absent on manifests generated before
   * the field was introduced.
   */
  manifestVersion?: string;

  /** Name of the plugin */
  pluginName: string;

  /** The packages that implement these capabilities, keyed by platform */
  packages?: PlatformPackages;

  /** Index of capabilities provided by the plugin to the name of the XLR for the capabilities */
  capabilities: {
    [capability: string]: Array<NamedType>;
  };
}
