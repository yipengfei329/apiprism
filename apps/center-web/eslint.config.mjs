import nextCoreVitals from "eslint-config-next/core-web-vitals";
import nextTypeScript from "eslint-config-next/typescript";

const eslintConfig = [
  ...nextCoreVitals,
  ...nextTypeScript,
  {
    rules: {
      // 未使用变量报错，以下划线开头的参数/变量允许忽略
      "@typescript-eslint/no-unused-vars": [
        "error",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_" },
      ],
      // 禁止显式 any（警告，逐步收紧）
      "@typescript-eslint/no-explicit-any": "warn",
      // 优先使用 const
      "prefer-const": "error",
      // 生产代码不应保留 console.log，允许 warn/error
      "no-console": ["warn", { allow: ["warn", "error"] }],
    },
  },
];

export default eslintConfig;
