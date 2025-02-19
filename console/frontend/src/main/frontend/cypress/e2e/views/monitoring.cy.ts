const monitorName = 'HAL 9000';

const findMonitor = () => {
  return cy
    .get('[data-cy="monitors__monitor__ibox"]')
    .contains('[data-cy="monitors__monitor__ibox__title"]', monitorName)
    .parentsUntil('[data-cy="monitors__monitor__ibox"]');
};

const editMonitor = () => {
  findMonitor().find('[data-cy="monitors__edit-monitor-button"]').click();
};

const raiseMonitor = () => {
  findMonitor().find('[data-cy="monitors__raise-monitor-button"]').click();
};

const clearMonitor = () => {
  findMonitor().find('[data-cy="monitors__clear-monitor-button"]').click();
};

const deleteTrigger = () => {
  findMonitor()
    .parent()
    .find('[data-cy="monitors__monitor__trigger__row"]')
    .find('[data-cy="monitors__delete-trigger-button"]')
    .click();
};

const deleteMonitor = () => {
  editMonitor();
  cy.get('[data-cy="monitors__delete-monitor-button"]').click();
};

describe('Monitoring page', () => {
  beforeEach(() => {
    cy.visit('/#/monitors?configuration=TX');
  });

  it('Add monitor with trigger', () => {
    // Add a monitor
    cy.get('[data-cy="monitors__add-monitor-button"]').click();
    cy.get('[data-cy="monitors-new__name__input"]').click().type(`${monitorName}{ctrl}{enter}`);
    editMonitor();

    // Add a trigger
    cy.get('[data-cy="monitors__add-trigger-button"]').click();
    cy.get('[data-cy="monitors-add-edit__threshold__input"]').clear().type('-1{ctrl}{enter}');
    cy.get('[data-cy="monitors-add-edit__alert"]').should('have.text', 'Negative values are not allowed');
    cy.get('[data-cy="monitors-add-edit__threshold__input"]').clear().type('0{ctrl}{enter}');
    findMonitor().parent().find('[data-cy="monitors__monitor__trigger__row"]').should('be.visible');

    // Raise and lower the monitor
    raiseMonitor();
    findMonitor().should('have.class', 'danger');
    clearMonitor();
    findMonitor().should('have.class', 'primary');

    // Delete the trigger
    editMonitor();
    deleteTrigger();
    findMonitor().parent().find('[data-cy="monitors__monitor__trigger__row"]').should('not.exist');

    // Delete the monitor
    deleteMonitor();
  });
});
