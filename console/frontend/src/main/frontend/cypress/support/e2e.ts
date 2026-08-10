import './commands';

Cypress.on('uncaught:exception', (err, runnable) => {
  // ignore monaco-editor uncaught exceptions (thanks microsoft)
  if (err.name === 'TypeError' && err.message === 'Property descriptor must be an object, got undefined') {
    return false;
  }
  return;
});
