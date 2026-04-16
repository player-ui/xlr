# 0.1.1-next.5 (Thu Apr 16 2026)

### Release Notes

#### Remove TypeScript Dependencies From SDK Import Chain ([#9](https://github.com/player-ui/xlr/pull/9))

The typescript npm package (~10 MB) was being pulled into every browser bundle that used `@xlr-lib/xlr-sdk` even though TypeScript's compiler API is only needed at build time (for .ts → XLR conversion).

### @xlr-lib/xlr-sdk

XLRSDK.exportRegistry() has been removed. Use exportTypesToTypeScript from @xlr-lib/xlr-converters instead:

// Before
```
const output = sdk.exportRegistry("TypeScript", importMap);
```

// After
```
import { exportTypesToTypeScript } from "@xlr-lib/xlr-converters";
const output = exportTypesToTypeScript(sdk.listTypes(), importMap);
```

### @xlr-lib/xlr-utils

The following TypeScript-compiler-API-specific exports have been moved to the `@xlr-lib/xlr-converters` package:

`decorateNode`, `createDocString`, `createTSDocString`, `symbolDisplayToString`, `tsStripOptionalType`, `isExportedDeclaration`,` isNodeExported`, `getReferencedType`, `isTypeScriptLibType`, `getStringLiteralsFromUnion`, `buildTemplateRegex`, `isOptionalProperty`,` isGenericInterfaceDeclaration`, `isGenericTypeDeclaration`, `isTypeReferenceGeneric`, `TopLevelDeclaration`, `isTopLevelDeclaration`, `TopLevelNode`, `isTopLevelNode`

### @xlr-lib/xlr-converters
Now contains all TypeScript compiler related code:

---

#### 🐛 Bug Fix

- Remove TypeScript Dependencies From SDK Import Chain [#9](https://github.com/player-ui/xlr/pull/9) ([@KetanReddy](https://github.com/KetanReddy))

#### Authors: 1

- Ketan Reddy ([@KetanReddy](https://github.com/KetanReddy))

---

# 0.1.1-next.4 (Tue Apr 14 2026)

### Release Notes

#### Fix `package.json` issue with Converters Package ([#10](https://github.com/player-ui/xlr/pull/10))

Fix type export for `@xlr-lib/converters` package causing `ERR_UNSUPPORTED_NODE_MODULES_TYPE_STRIPPING` error when imported

---

#### 🐛 Bug Fix

- Fix `package.json` issue with Converters Package [#10](https://github.com/player-ui/xlr/pull/10) ([@KetanReddy](https://github.com/KetanReddy))

#### Authors: 1

- Ketan Reddy ([@KetanReddy](https://github.com/KetanReddy))

---

# 0.1.1-next.3 (Tue Apr 14 2026)

### Release Notes

#### Bring in Changes from Tools Repo ([#8](https://github.com/player-ui/xlr/pull/8))

Sync latest from upstream tools repository.

---

#### 🐛 Bug Fix

- Bring in Changes from Tools Repo [#8](https://github.com/player-ui/xlr/pull/8) ([@KetanReddy](https://github.com/KetanReddy))

#### Authors: 1

- Ketan Reddy ([@KetanReddy](https://github.com/KetanReddy))

---

# 0.1.1-next.2 (Fri Mar 13 2026)

### Release Notes

#### Create Entrypoint for Standalone XLR Export ([#2](https://github.com/player-ui/xlr/pull/2))

Offer single file export from `converters` package

---

#### 🚀 Enhancement

- Create Entrypoint for Standalone XLR Export [#2](https://github.com/player-ui/xlr/pull/2) ([@KetanReddy](https://github.com/KetanReddy))

#### Authors: 1

- Ketan Reddy ([@KetanReddy](https://github.com/KetanReddy))

---

# 0.1.1-next.1 (Tue Feb 17 2026)

### Release Notes

#### Pull in Latest Changes from Tools ([#3](https://github.com/player-ui/xlr/pull/3))

Pull in latest changes from main tools repo

---

#### 🚀 Enhancement

- Pull in Latest Changes from Tools [#3](https://github.com/player-ui/xlr/pull/3) ([@KetanReddy](https://github.com/KetanReddy))

#### 🐛 Bug Fix

- Migration of `xlr` packages from tools repo [#1](https://github.com/player-ui/xlr/pull/1) ([@KetanReddy](https://github.com/KetanReddy))

#### Authors: 1

- Ketan Reddy ([@KetanReddy](https://github.com/KetanReddy))
