const environmentVariables = {
  'Application Constants': {
    'Global': {
      'test.property': 'xyz\n)(*&^%$#\\@abc'
    },
  },
};

describe("Environment Variables page", () => {

  it('Check that whitespaces are displayed properly', () => {
    cy.intercept('**/api/environmentvariables', {
      statusCode: 200,
      body: environmentVariables,
    });
    cy.visit('/#/environment-variables');
    cy.get('[data-cy="env-vars__global-table"] [data-cy=global-table__key]').eq(0).should('have.text', 'test.property');
    cy.get('[data-cy="env-vars__global-table"] [data-cy=global-table__value]').eq(0).should('have.text', String.raw`xyz\n)(*&^%$#\\@abc`);
  });
});
