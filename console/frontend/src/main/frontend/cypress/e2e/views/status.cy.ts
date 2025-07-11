describe('Status page', () => {
  beforeEach(() => {
    cy.visit('/#/status');
  });

  it('Configuration messages should have startup massage', () => {
    cy.get('[data-cy="configuration-messages__box"]').click();
    cy.get('[data-cy="configuration-messages__table__message"]')
      .invoke('text')
      .should('match', /Application \[.+\] startup in \d+ ms/);
  });

  it('Adapter status should have message', () => {
    cy.get('[data-cy="adapter-status__box"]').first().click();
    cy.get('[data-cy="adapter-status__table__message"]')
      .first()
      .should('contain.text', 'successfully configured PipeLine');
  });
});
