load("@npm//:defs.bzl", "npm_link_all_packages")
load("@rules_player//javascript:defs.bzl", "js_pipeline")
load("//helpers:defs.bzl", "tsup_config", "vitest_config")
load("@aspect_rules_js//js:defs.bzl", "js_binary", "js_library")

npm_link_all_packages(name = "node_modules")

tsup_config(name = "tsup_config")

vitest_config(name = "vitest_config")

js_library(
        name = "entrypoint",
        srcs =  glob(["bin/*"]),
        deps = [],
    )

js_pipeline(
    package_name = "@xlr-lib/xlr-converters",
    peer_deps = [
        "//:node_modules/typescript",
    ],
    test_deps = [
        "//:node_modules",
        "//:vitest_config",
        "//:node_modules/@typescript/vfs",
        ":node_modules/@xlr-lib/test-utils",
    ],
    deps = [
        ":node_modules/@xlr-lib/xlr",
        ":node_modules/@xlr-lib/xlr-utils",
    ],
    include_packaging_targets = [
       ":entrypoint"
    ],
    create_package_json_args = {
        "custom_entrypoints": True
    }
)
