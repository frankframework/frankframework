describe('Logging page', () => {
  beforeEach(() => {
    cy.visit('/#/logging');
  });

  it('Navigation should work', () => {
    const logFile = 'ibis4test.log';
    const loggingPageUrlRegExp = /\/#\/logging$/;
    const logFileUrlRegExp = new RegExp(`/#\/logging\/.+\/${logFile}$`);
    const loggingPageAfterFileUrlRegExp = new RegExp(`/#\/logging\/.+logs#${logFile}$`);

    cy.url().should('match', loggingPageUrlRegExp);
    cy.get('[data-cy="logging__search__input"]').type(logFile);
    cy.get('[data-cy="logging__table__file"]').first().click();
    cy.url().should('match', logFileUrlRegExp);
    cy.get('[data-cy="file-viewer__container"]')
      .should('be.visible')
      .and('not.have.text', '')
      .and('not.contain.text', 'Error requesting file data');
    cy.get('[data-cy="logging__back-button"]').click();
    cy.url().should('match', loggingPageAfterFileUrlRegExp);
    cy.get('[data-cy="logging__table__file"]').first().click();
    cy.get('[data-cy="logging__alert"]')
      .invoke('text')
      .should('match', /Access to path (.+) not allowed!/);
  });
});

describe('Logging settings', () => {
  beforeEach(() => {
    cy.visit('/#/logging/settings');
  });

  it('Adding a log defenition', () => {
    cy.get('[data-cy="logging-settings__add-logger-button"]').click();
    cy.get('[data-cy="logging-add__logger-name__input"]').type('org.frankframework.cool.test');
    cy.get('[data-cy="logging-add__level__select"]').select('ERROR');
    cy.get('[data-cy="logging-add__send-button"]').click();
    cy.get('[data-cy="logging-settings__table"]').contains('td', 'org.frankframework.cool.test').should('exist');
    cy.get('[data-cy="logging-settings__reconfigure-button"]').click();
    cy.get('[data-cy="logging-settings__table"]').contains('td', 'org.frankframework.cool.test').should('not.exist');
  });
});
