import tseslint from "typescript-eslint";
import reactHooks from "eslint-plugin-react-hooks";

export default tseslint.config(
    { ignores: ["dist/**", "node_modules/**", ".expo/**"] },
    ...tseslint.configs.recommended,
    {
        files: ["**/*.ts", "**/*.tsx"],
        languageOptions: { parserOptions: { projectService: true, tsconfigRootDir: import.meta.dirname } },
        plugins: { "react-hooks": reactHooks },
        rules: {
            "@typescript-eslint/no-unsafe-assignment": "error",
            "@typescript-eslint/no-unsafe-member-access": "error",
            "@typescript-eslint/no-unsafe-call": "error",
            "@typescript-eslint/no-unsafe-return": "error",
            "@typescript-eslint/no-floating-promises": "error",
            "react-hooks/rules-of-hooks": "error",
            "react-hooks/exhaustive-deps": "error",
        },
    },
    {
        rules: {
            curly: ["error", "all"],
            eqeqeq: "error",
            "no-var": "error",
            "@typescript-eslint/no-shadow": "error",
            "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
            "@typescript-eslint/no-require-imports": ["error", { allow: ["\\.png$"] }],
        },
    },
    {
        files: ["**/*.cjs"],
        // CommonJS test and CLI entry points require Node-style imports.
        rules: { "@typescript-eslint/no-require-imports": "off" },
    },
);
