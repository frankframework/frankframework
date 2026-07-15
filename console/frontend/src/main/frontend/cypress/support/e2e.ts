import './commands';

Cypress.on('uncaught:exception', (error, _runnable) => {
  // Ignore monaco-editor uncaught exceptions (thanks Microsoft)
  if (error.name === 'TypeError' && error.message === 'Property descriptor must be an object, got undefined') {
    return false;
  }
  return;
});
