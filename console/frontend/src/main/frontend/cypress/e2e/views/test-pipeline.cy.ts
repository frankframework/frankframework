describe('Test a Pipeline', () => {
  beforeEach(() => {
    cy.visit('/#/test-pipeline');
    cy.get('[data-cy=test-pipeline__config__input] input').should('not.be.disabled');
    cy.get('[data-cy=test-pipeline__config__input]').type('HTTP{enter}');
  });

  it('runs the ApiListener_SimpleGet adapter successfully', () => {
    cy.get('[data-cy=test-pipeline__adapter__input]').type('ApiListener_SimpleGet{enter}{ctrl}{enter}');
    cy.get('[data-cy=test-pipeline__alert]').should('have.text', 'SUCCESS');
    cy.get('[data-cy=test-pipeline__result]').should('have.text', '<success/>');
  });

  it('runs the WebServiceListener adapter successfully without a result', () => {
    cy.get('[data-cy=test-pipeline__adapter__input]').type('WebServiceListener{enter}{ctrl}{enter}');
    cy.get('[data-cy=test-pipeline__alert]').should('have.text', 'SUCCESS');
    cy.get('[data-cy=test-pipeline__result]').should('not.exist');
  });

  it('echoes the message back when running the WebServiceListener adapter', () => {
    cy.get('[data-cy=test-pipeline__adapter__input]').type('WebServiceListener{enter}');

    cy.setMonacoValue("I've Played These Games Before!");
    cy.get('[data-cy-test-pipeline=send]').click();
    cy.get('[data-cy=test-pipeline__result]').should('have.text', "I've Played These Games Before!");
    cy.get('[data-cy=test-pipeline__alert]').should('have.text', 'SUCCESS');
  });
});
