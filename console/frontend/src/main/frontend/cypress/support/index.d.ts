declare namespace Cypress {
  // eslint-disable-next-line @typescript-eslint/consistent-type-definitions -- interface required for Cypress declaration merging
  interface Chainable {
    setMonacoValue(value: string, modelIndex?: number): Chainable<void>;
  }
}
