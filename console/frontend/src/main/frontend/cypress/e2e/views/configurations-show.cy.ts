describe('Configurations page', () => {
  beforeEach(() => {
    cy.visit('/#/configurations');
  });

  it('should have monaco-editor loaded in with readonly', () => {
    cy.get('[data-cy="configurations__editor"] .monaco-editor.no-user-select').should('exist');
  });

  it('should highlight line on clicking line number', () => {
    cy.get('[data-cy="configurations__editor"] .cdr.monaco-editor__line--highlighted').should('not.exist');

    cy.get('[data-cy="configurations__editor"] .line-numbers').first().click();
    cy.get('[data-cy="configurations__editor"] .cdr.monaco-editor__line--highlighted').should('exist');
    cy.url().should('contain', '#L1');

    cy.reload();
    cy.get('[data-cy="configurations__editor"] .cdr.monaco-editor__line--highlighted').should('exist');
  });

  it('should highlight adapter', () => {
    cy.visit('/#/status');
    cy.wait(200); // wait for configuration page to unload before navigating back
    cy.visit('/#/configurations?name=HTTP&adapter=WebServiceListener');
    cy.get('[data-cy="configurations__editor"] .cdr.monaco-editor__line--highlighted').should('exist');
    cy.url().should('contain', '#L');
  });
});
