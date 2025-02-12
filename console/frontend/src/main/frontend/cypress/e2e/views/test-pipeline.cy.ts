describe('Test a Pipeline', () => {
  beforeEach(() => {
    cy.visit('/#/test-pipeline');
  });

  it('Test a Pipeline', () => {
    cy.get('[data-cy=test-pipeline__config__input]').type('HTTP{enter}');
    cy.get('[data-cy=test-pipeline__adapter__input]').type('ApiListener_SimpleGet{enter}{ctrl}{enter}');
    cy.get('[data-cy=test-pipeline__alert]').should('have.text', 'SUCCESS');
    cy.get('[data-cy=test-pipeline__result]').should('have.text', '<success/>');
    cy.get('[data-cy=test-pipeline__adapter__input]').click();
    cy.get('[data-cy=test-pipeline__adapter__input] input').clear();
    cy.get('[data-cy=test-pipeline__adapter__input]').type('WebServiceListener{enter}{ctrl}{enter}');
    cy.get('[data-cy=test-pipeline__alert]').should('contain.text', 'payload must not be null');
    cy.get('[data-cy=test-pipeline__message__input]').click();
    cy.get('[data-cy=test-pipeline__message__input]').type("I've Played These Games Before!{ctrl}{enter}");
    cy.get('[data-cy=test-pipeline__result]').should('have.text', "I've Played These Games Before!");
    cy.get('[data-cy=test-pipeline__alert]').should('have.text', 'SUCCESS');
  });
});
