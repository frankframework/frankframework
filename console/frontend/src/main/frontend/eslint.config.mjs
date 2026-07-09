// @ts-check
import eslint from '@eslint/js';
import { defineConfig } from 'eslint/config';
import tseslint from 'typescript-eslint';
import angular from 'angular-eslint';
import prettierPlugin from 'eslint-plugin-prettier';
import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';
import eslintPluginUnicorn from 'eslint-plugin-unicorn';

export default defineConfig([
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
      'unicorn/consistent-class-member-order': 'off', // handled by @typescript-eslint/member-ordering
      'unicorn/prefer-object-iterable-methods': 'off',
      'unicorn/name-replacements': [
        'warn',
        {
          replacements: { configuration: false },
        },
      ],
      'unicorn/prefer-await': 'off', // preferably only if the function works better as async
      'unicorn/consistent-boolean-name': 'off',
      'unicorn/no-empty-file': 'off',

      'unicorn/no-useless-template-literals': 'off', // doesnt work nice with angular templates
      'unicorn/no-incorrect-template-string-interpolation': 'off', // doesnt work nice with angular templates
      'unicorn/prefer-string-raw': 'off', // doesnt work nice with angular templates
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
  {
    files: ['**/*.ts'],
    extends: [
      eslint.configs.recommended,
      tseslint.configs.recommended,
      tseslint.configs.stylistic,
      angular.configs.tsRecommended,
    ],
    plugins: {
      prettier: prettierPlugin,
    },
    processor: angular.processInlineTemplates,
    rules: {
      '@typescript-eslint/explicit-function-return-type': 'error',
      '@typescript-eslint/triple-slash-reference': 'warn',
      '@typescript-eslint/member-ordering': [
        'error',
        {
          default: {
            memberTypes: [
              // Index signature
              'signature',
              'call-signature',

              // Fields
              'public-static-field',
              'protected-static-field',
              'private-static-field',
              '#private-static-field',

              'public-decorated-field',
              'protected-decorated-field',
              'private-decorated-field',

              'public-instance-field',
              'protected-instance-field',
              'private-instance-field',
              'private-instance-readonly-field',
              '#private-instance-field',
              '#private-instance-readonly-field',

              'public-abstract-field',
              'protected-abstract-field',

              'public-field',
              'protected-field',
              'private-field',
              '#private-field',

              'static-field',
              'instance-field',
              'abstract-field',

              'decorated-field',

              'field',

              // Static initialization
              'static-initialization',

              // Constructors
              'public-constructor',
              'protected-constructor',
              'private-constructor',

              'constructor',

              // Accessors
              'public-static-accessor',
              'protected-static-accessor',
              'private-static-accessor',
              '#private-static-accessor',

              'public-decorated-accessor',
              'protected-decorated-accessor',
              'private-decorated-accessor',

              'public-instance-accessor',
              'protected-instance-accessor',
              'private-instance-accessor',
              '#private-instance-accessor',

              'public-abstract-accessor',
              'protected-abstract-accessor',

              'public-accessor',
              'protected-accessor',
              'private-accessor',
              '#private-accessor',

              'static-accessor',
              'instance-accessor',
              'abstract-accessor',

              'decorated-accessor',

              'accessor',

              // Getters and Setters (merged)
              ['public-static-get', 'public-static-set'],
              ['protected-static-get', 'protected-static-set'],
              ['private-static-get', 'private-static-set'],
              ['#private-static-get', '#private-static-set'],

              ['public-decorated-get', 'public-decorated-set', 'public-instance-get', 'public-instance-set'],
              [
                'protected-decorated-get',
                'protected-decorated-set',
                'protected-instance-get',
                'protected-instance-set',
              ],
              ['private-decorated-get', 'private-decorated-set', 'private-instance-get', 'private-instance-set'],
              ['#private-instance-get', '#private-instance-set'],

              ['public-abstract-get', 'public-abstract-set'],
              ['protected-abstract-get', 'protected-abstract-set'],

              ['public-get', 'public-set'],
              ['protected-get', 'protected-set'],
              ['private-get', 'private-set'],
              ['#private-get', '#private-set'],

              ['static-get', 'static-set'],
              ['instance-get', 'instance-set'],
              ['abstract-get', 'abstract-set'],

              ['decorated-get', 'decorated-set'],

              ['get', 'set'],

              // Methods
              'public-static-method',
              'protected-static-method',
              'private-static-method',
              '#private-static-method',

              'public-decorated-method',
              'protected-decorated-method',
              'private-decorated-method',

              'public-instance-method',
              'protected-instance-method',
              'private-instance-method',
              '#private-instance-method',

              'public-abstract-method',
              'protected-abstract-method',

              'public-method',
              'protected-method',
              'private-method',
              '#private-method',

              'static-method',
              'instance-method',
              'abstract-method',

              'decorated-method',

              'method',
            ],
          },
        },
      ],
      '@typescript-eslint/consistent-type-definitions': ['error', 'type'],
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          args: 'all',
          argsIgnorePattern: '^_',
          caughtErrors: 'all',
          caughtErrorsIgnorePattern: '^_',
          destructuredArrayIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          ignoreRestSiblings: true,
        },
      ],

      // Angular: https://github.com/angular-eslint/angular-eslint/blob/main/packages/eslint-plugin/README.md
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'app', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'app', style: 'kebab-case' }],
      '@angular-eslint/prefer-on-push-component-change-detection': 'warn',

      // EcmaScript: https://eslint.org/docs/latest/rules/
      ...js.configs.recommended.rules,
      'prefer-template': 'error',
      'no-undef': 'off',
      'no-unused-vars': 'off', // Handled by @typescript-eslint/no-unused-vars

      // Prettier: https://github.com/prettier/eslint-config-prettier?tab=readme-ov-file#special-rules
      ...eslintConfigPrettier.rules,
      'prettier/prettier': 'warn',

      'unicorn/no-useless-template-literals': 'error', // turn on for ts files only
      'unicorn/no-incorrect-template-string-interpolation': 'error', // turn on for ts files only
      'unicorn/prefer-string-raw': 'error', // turn on for ts files only
    },
  },
  {
    files: ['**/*.html'],
    extends: [angular.configs.templateRecommended, angular.configs.templateAccessibility],
    plugins: {
      prettier: prettierPlugin,
    },
    rules: {
      '@angular-eslint/template/prefer-self-closing-tags': 'error',
      '@angular-eslint/template/no-interpolation-in-attributes': ['error'],
      '@angular-eslint/template/click-events-have-key-events': 'off',
      '@angular-eslint/template/interactive-supports-focus': [
        'error',
        {
          allowList: ['li'],
        },
      ],
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
]);
