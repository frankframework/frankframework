import typescriptParser from '@typescript-eslint/parser';
import prettierPlugin from 'eslint-plugin-prettier';
import tsPlugin from '@typescript-eslint/eslint-plugin';
import angularPlugin from '@angular-eslint/eslint-plugin';
import angularTemplate from '@angular-eslint/eslint-plugin-template';
import angularTemplateParser from '@angular-eslint/template-parser';
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';
import eslintPluginUnicorn from 'eslint-plugin-unicorn';
import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';

export default [
  {
    ignores: ['.cache/', '.git/', '.github/', 'node_modules/'],
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        project: ['./tsconfig.json', './tsconfig.app.json', './tsconfig.spec.json'],
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
      '@angular-eslint': angularPlugin,
      prettier: prettierPlugin,
    },
    rules: {
      // TypeScript: https://typescript-eslint.io/rules/
      ...tsPlugin.configs.recommended.rules,
      ...tsPlugin.configs.stylistic.rules,
      '@typescript-eslint/explicit-function-return-type': 'error',
      '@typescript-eslint/triple-slash-reference': 'warn',
      '@typescript-eslint/member-ordering': 'error',
      '@typescript-eslint/consistent-type-definitions': ['error', 'type'],

      // Angular: https://github.com/angular-eslint/angular-eslint/blob/main/packages/eslint-plugin/README.md
      ...angularPlugin.configs.recommended.rules,
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'app', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'app', style: 'kebab-case' }],

      // EcmaScript: https://eslint.org/docs/latest/rules/
      ...js.configs.recommended.rules,
      'prefer-template': 'error',
      'no-undef': 'off',

      // Prettier: https://github.com/prettier/eslint-config-prettier?tab=readme-ov-file#special-rules
      ...eslintConfigPrettier.rules,
      'prettier/prettier': 'warn',
    },
  },
  {
    files: ['**/*.html'],
    languageOptions: {
      parser: angularTemplateParser,
    },
    plugins: {
      '@angular-eslint': angularPlugin,
      '@angular-eslint/template': angularTemplate,
      prettier: prettierPlugin,
    },
    rules: {
      // Angular template: https://github.com/angular-eslint/angular-eslint/blob/main/packages/eslint-plugin-template/README.md
      ...angularTemplate.configs.recommended.rules,
      ...angularTemplate.configs.accessibility.rules,
      '@angular-eslint/template/prefer-self-closing-tags': 'error',
      '@angular-eslint/template/no-interpolation-in-attributes': ['error'],
      '@angular-eslint/template/click-events-have-key-events': 'off',
      '@angular-eslint/template/interactive-supports-focus': [
        'error',
        {
          allowList: ['li'],
        },
      ],
      '@angular-eslint/contextual-decorator': 'warn',
      '@angular-eslint/prefer-signals': 'error',
      '@angular-eslint/template/attributes-order': [
        'error',
        {
          alphabetical: false,
          order: [
            'TEMPLATE_REFERENCE',
            'ATTRIBUTE_BINDING',
            'STRUCTURAL_DIRECTIVE',
            'INPUT_BINDING',
            'TWO_WAY_BINDING',
            'OUTPUT_BINDING',
          ],
        },
      ],

      // Prettier: https://github.com/prettier/eslint-config-prettier?tab=readme-ov-file#special-rules
      ...eslintConfigPrettier.rules,
      'prettier/prettier': ['error', { parser: 'angular' }],
    },
  },
  // Unicorn: https://github.com/sindresorhus/eslint-plugin-unicorn
  eslintPluginUnicorn.configs.recommended,
  {
    rules: {
      'unicorn/prevent-abbreviations': 'warn',
      'unicorn/no-array-reduce': 'off',
      'unicorn/prefer-ternary': 'warn',
      'unicorn/no-null': 'off',
      'unicorn/prefer-dom-node-text-content': 'warn',
      'unicorn/consistent-function-scoping': [
        'error',
        {
          checkArrowFunctions: false,
        },
      ],
    },
  },
  eslintPluginPrettierRecommended,
  // SonarJS: https://github.com/SonarSource/SonarJS/blob/master/packages/jsts/src/rules/README.md
  // sonarjs.configs.recommended,
  // {
  //   rules: {
  //     'sonarjs/cognitive-complexity': 'error',
  //     'sonarjs/no-duplicate-string': 'error',
  //   },
  // },
];
