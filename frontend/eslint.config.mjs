import tseslint from "typescript-eslint";

export default tseslint.config(
    { ignores: ["dist/**", "node_modules/**", ".expo/**"] },
    ...tseslint.configs.recommended,
    { rules: {
        "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
        "@typescript-eslint/no-require-imports": ["error", { allow: ["\\.png$"] }],
    } },
);
