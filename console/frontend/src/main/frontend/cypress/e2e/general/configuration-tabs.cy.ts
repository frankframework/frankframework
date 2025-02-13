describe('Status page', () => {
  beforeEach(() => {
    cy.visit('/#/status');
  });

  it('Configuration tab should stay persistent', () => {
    cy.get('[data-cy="status__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('not.have.class', 'active');
    cy.get('[data-cy="status__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').click();
    cy.get('[data-cy="status__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('have.class', 'active');
    cy.get('[data-cy="pages-navigation__monitors"]').click();
    cy.get('[data-cy="monitors__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('have.class', 'active');
    cy.reload()
    cy.get('[data-cy="monitors__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('have.class', 'active');
    cy.get('[data-cy="pages-navigation__status"]').click();
    cy.get('[data-cy="status__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('have.class', 'active');
    cy.go('back');
    cy.get('[data-cy="monitors__configuration-tab-list"] [data-cy="tab-list__nav-tabs"]').contains('a', 'HTTP').parent().should('have.class', 'active');
  });
});
