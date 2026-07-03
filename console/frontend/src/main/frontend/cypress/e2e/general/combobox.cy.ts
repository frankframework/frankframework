describe('Combobox', () => {
  const combobox = '[data-cy=test-pipeline__config__input]';
  const comboboxInput = `${combobox} input`;
  const options = '.combobox__options';
  const optionLabels = '.combobox__list-item__label';
  const selectedOption = '.combobox__list-item--selected';
  const clearButton = '.combobox__list-item--clear';

  beforeEach(() => {
    cy.visit('/#/test-pipeline');
  });

  it('open the suggestions, navigate and select with keyboard', () => {
    cy.get(comboboxInput).focus();
    cy.get(options).should('be.visible');

    cy.get(comboboxInput).type('{downArrow}');
    cy.get(selectedOption).should('exist');

    cy.get(comboboxInput).type('{enter}');
    cy.get(options).should('not.exist');
    cy.get(comboboxInput).should('not.have.value', '');
  });

  it('select and option by typing it out', () => {
    cy.get(comboboxInput).type('HTTP');
    cy.get(optionLabels).should('contain.text', 'HTTP');

    cy.get(comboboxInput).type('{enter}');
    cy.get(comboboxInput).should('have.value', 'HTTP');
    cy.get(options).should('not.exist');
  });

  it('select and option by typing it partially and clicking with mouse', () => {
    cy.get(comboboxInput).type('HTT');
    cy.get(optionLabels).should('contain.text', 'HTTP');

    cy.contains('.combobox__list-item', 'HTTP').click();
    cy.get(comboboxInput).should('have.value', 'HTTP');
    cy.get(options).should('not.exist');
  });

  it('check if the suggestions are getting filtered correctly', () => {
    cy.get(comboboxInput).click();
    cy.get(optionLabels)
      .its('length')
      .then((totalCount) => {
        cy.get(comboboxInput).type('HTTP');

        cy.get(optionLabels).each(($label) => {
          expect($label.text().toLowerCase()).to.contain('http');
        });

        cy.get(optionLabels).its('length').should('be.lte', totalCount);
      });
  });

  it('resets the selection using the clear button', () => {
    cy.get(comboboxInput).type('HTTP{enter}');
    cy.get(comboboxInput).should('have.value', 'HTTP');

    cy.get(comboboxInput).type('{downArrow}');
    cy.get(options).should('be.visible');
    cy.get(clearButton).should('be.visible').click();

    cy.get(comboboxInput).should('have.value', '');
  });

  it('resets the selection by clicking the input after losing focus', () => {
    cy.get(comboboxInput).type('HTTP{enter}');
    cy.get(comboboxInput).should('have.value', 'HTTP');

    cy.get(comboboxInput).blur();
    cy.get(options).should('not.exist');

    cy.get(comboboxInput).click();
    cy.get(comboboxInput).should('have.value', '');
    cy.get(options).should('be.visible');
    cy.get(optionLabels).should('have.length.greaterThan', 0);
  });
});

