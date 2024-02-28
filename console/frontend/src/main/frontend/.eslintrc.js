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
        'plugin:@angular-eslint/recommended',
        'plugin:@angular-eslint/template/process-inline-templates',
        'plugin:unicorn/recommended',
        'plugin:prettier/recommended',
      ],
      rules: {
        'unicorn/prevent-abbreviations': 'warn',
        'unicorn/no-array-reduce': 'off',
        'unicorn/prefer-ternary': 'warn',
        'unicorn/no-null': 'off',
        '@typescript-eslint/explicit-function-return-type': 'error',
      },
    },
    {
      files: ['*.html', '*.css', '*.scss'],
      extends: [
        'plugin:@angular-eslint/template/recommended',
        'plugin:prettier/recommended'
      ],
      rules: {},
    },
  ],
};
