module.exports = {
  root: true,
  ignorePatterns: ['projects/**/*'],
  overrides: [
    {
      files: ['*.ts'],
      // parser: '@typescript-eslint/parser',
      parserOptions: {
        project: ['tsconfig.app.json', 'tsconfig.spec.json'],
        tsconfigRootDir: __dirname,
        createDefaultProgram: true,
        sourceType: 'module',
      },
      // plugins: ['@typescript-eslint'],
      extends: [
        // 'plugin:@typescript-eslint/recommended',
        'plugin:@angular-eslint/recommended',
        'plugin:@angular-eslint/template/process-inline-templates',
        'plugin:prettier/recommended',
        'plugin:unicorn/recommended',
      ],
      rules: {
        'unicorn/prevent-abbreviations': 'warn',
        'unicorn/no-array-reduce': 'off',
        'unicorn/prefer-ternary': 'warn',
        'unicorn/no-null': 'off',
        '@typescript-eslint/explicit-function-return-type': 'error',
        // '@typescript-eslint/explicit-member-accessibility': ['error', { accesibility: 'explicit' }]
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
