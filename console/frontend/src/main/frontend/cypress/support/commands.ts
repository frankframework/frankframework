/// <reference types="cypress" />

Cypress.Commands.add('setMonacoValue', (value: string, modelIndex = 0) => {
  setMonacoValue(value, modelIndex);
});

type MonacoEditorModel = {
  setValue(value: string): void;
};

type MonacoInstance = {
  editor: {
    getModels(): MonacoEditorModel[];
  };
};

type MonacoWindow = Window & { monaco?: MonacoInstance };

function setMonacoValue(value: string, modelIndex = 0): void {
  cy.window().then((autWindow) => {
    const monacoWindow = autWindow as MonacoWindow;
    const framedWindow = autWindow.frames[0] as MonacoWindow | undefined;
    const monacoInstance = monacoWindow.monaco ?? framedWindow?.monaco ?? null;

    if (!monacoInstance) {
      throw new Error('Monaco editor not found on the window or frames[0]');
    }

    const model = monacoInstance.editor.getModels()[modelIndex];

    if (!model) {
      throw new Error(`Monaco editor model at index ${modelIndex} not found`);
    }

    model.setValue(value);
  });
}
