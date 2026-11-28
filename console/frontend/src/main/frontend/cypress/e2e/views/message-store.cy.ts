describe('Message Store', () => {
  it('Should display message on status page', () => {
    cy.visit('/#/status');

    cy.get('[data-cy="status__error-log-warning"]').should('exist');
    cy.contains('Errorlog contains 6 records');

    const adapterStatusBox = cy.get('#ApiListener_SimpleInsertWithException [data-cy="adapter-status__box"]');
    adapterStatusBox.get('[data-cy="status__adapter-status__message-store-error"]').should('be.visible');
    adapterStatusBox.click();

    cy.get('[data-cy="status__adapter-status__message-store__btn-error"]')
      .should('exist')
      .scrollIntoView()
      .should('be.visible');
  });

  it('Should list error messages in store', () => {
    cy.visit('/#/HTTP/adapters/ApiListener_SimpleInsertWithException/receivers/Receiver%20%5B1%5D/stores/Error');
    cy.get('[data-cy="message-store__row"]').should('have.length.greaterThan', 2);
  });

  it('Should be able to select and unselect all messages', () => {
    cy.visit('/#/HTTP/adapters/ApiListener_SimpleInsertWithException/receivers/Receiver%20%5B1%5D/stores/Error');
    cy.get('[data-cy="message-store__row"]').should('have.length.greaterThan', 0);
    cy.get('[data-cy="message-store__select-all"]').click();
    cy.get('[data-cy="message-store__row"] input[type="checkbox"]').should('be.checked');
    cy.get('[data-cy="message-store__unselect-all"]').click();
    cy.get('[data-cy="message-store__row"] input[type="checkbox"]').should('not.be.checked');
  });

  it('Should be able to delete messages', () => {
    cy.visit('/#/HTTP/adapters/ApiListener_SimpleInsertWithException/receivers/Receiver%20%5B1%5D/stores/Error');
    cy.get('[data-cy="message-store__row"]')
      .should('have.length.greaterThan', 0)
      .then(($rows) => {
        const initialCount = $rows.length;
        cy.get('[data-cy="message-store__row"]').last().find('[data-cy="message-store__row__delete-selected"]').click();
        cy.get('[data-cy="message-store__row"]').should('have.length', initialCount - 1);
      });
  });

  it('Should be able to delete all messages', () => {
    cy.visit('/#/HTTP/adapters/ApiListener_SimpleInsertWithException/receivers/Receiver%20%5B1%5D/stores/Error');
    cy.get('[data-cy="message-store__row"]').should('have.length.greaterThan', 0);
    cy.get('[data-cy="message-store__select-all"]').click();
    cy.get('[data-cy="message-store__delete-selected"]').click();
    cy.get('[data-cy="message-store__row"]').should('have.length', 0);
  });
});
