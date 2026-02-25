import type { Manifest } from "@xlr-lib/xlr";
import ts from "typescript";
import path from "path";
import fs from "fs";
import { TsConverter } from "../ts/ts-to-xlr";
import { writeManifest } from "./writeManifest";

/** Basic Exporter to export a self contained type file */
export function converter(
  sourceFile: string,
  outputDirectory: string,
  customPrimitives: string[],
  tsOptions: ts.CompilerOptions = {},
): Manifest | undefined {
  const program = ts.createProgram([sourceFile], tsOptions);
  fs.mkdirSync(outputDirectory, { recursive: true });

  // Get the checker, we will use it to find more about classes
  const checker = program.getTypeChecker();

  const converter = new TsConverter(checker, customPrimitives);
  const convertedTypes = converter.convertSourceFile(
    program.getSourceFiles()[0],
  );

  if (convertedTypes.data.types.length === 0) {
    return undefined;
  }

  convertedTypes.data.types.forEach((type) => {
    fs.writeFileSync(
      path.join(outputDirectory, `${type.name}.json`),
      JSON.stringify(type, undefined, 4),
    );
  });

  const manifest: Manifest = {
    pluginName: "Types",
    capabilities: new Map([["Types", convertedTypes.convertedTypes]]),
    customPrimitives: customPrimitives,
  };

  writeManifest(manifest, outputDirectory);
}
