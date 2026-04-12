module.exports = {
  extends: ["@commitlint/config-conventional"],
  rules: {
    "scope-enum": [
      2,
      "always",
      [
        "center-server",
        "center-web",
        "starter",
        "java-core",
        "contracts",
        "canonical-model",
        "normalization",
        "deploy",
        "docs",
        "repo"
      ]
    ]
  }
};

