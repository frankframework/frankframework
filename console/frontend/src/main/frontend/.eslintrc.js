module.exports = {
  root: true,
  ignorePatterns: ['projects/**/*'],
  overrides: [
    {
      files: ['*.ts'],
      parserOptions: {
        project: ['tsconfig.app.json', 'tsconfig.spec.json'],
        tsconfigRootDir: __dirname,
        createDefaultProgram: true,
        sourceType: 'module',
      },
      extends: [
        'plugin:@typescript-eslint/recommended',
        'plugin:@angular-eslint/recommended',
        'plugin:@angular-eslint/template/process-inline-templates',
        'plugin:unicorn/recommended',
        'plugin:prettier/recommended',
      ],
      rules: {
        'prefer-template': 'error',
        '@typescript-eslint/explicit-function-return-type': 'error',
        '@typescript-eslint/triple-slash-reference': 'warn',
        'unicorn/prevent-abbreviations': 'warn',
        'unicorn/no-array-reduce': 'off',
        'unicorn/prefer-ternary': 'warn',
        'unicorn/no-null': 'off',
        'unicorn/prefer-dom-node-text-content': 'warn',
      },
    },
    {
      files: ['*.component.html', '*.css', '*.scss'],
      extends: [
        'plugin:@angular-eslint/template/recommended',
        'plugin:prettier/recommended'
      ],
      rules: {},
    },
  ],
};
