import { Manifest } from "@xlr-lib/xlr";
import path from "path";
import fs from "fs";

/** Serializes ES6 Maps */
function replacer(key: any, value: any) {
  if (value instanceof Map) {
    return Object.fromEntries(value.entries());
  }

  return value;
}

export function writeManifest(
  capabilities: Manifest,
  outputDirectory: string,
): void {
  // print out the manifest files
  const jsonManifest = JSON.stringify(capabilities, replacer, 4);
  fs.writeFileSync(path.join(outputDirectory, "manifest.json"), jsonManifest);

  const tsManifestFile = `${[...(capabilities.capabilities?.values() ?? [])]
    .flat(2)
    .map((capability) => {
      return `const ${capability} = require("./${capability}.json")`;
    })
    .join("\n")}

    module.exports = {
      "pluginName": "${capabilities.pluginName}",
      "capabilities": {
        ${[...(capabilities.capabilities?.entries() ?? [])]
          .map(([capabilityName, provides]) => {
            return `"${capabilityName}":[${provides.join(",")}],`;
          })
          .join("\n\t\t")}
      },
      "customPrimitives": [
        ${[capabilities.customPrimitives?.map((i) => `"${i}"`).join(",") ?? ""]}
      ]
    }
`;

  fs.writeFileSync(path.join(outputDirectory, "manifest.js"), tsManifestFile);
}
