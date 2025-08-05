import { defineConfig } from 'cypress';
import * as fs from 'node:fs';

export default defineConfig({
  video: true,
  e2e: {
    experimentalStudio: true,
    defaultCommandTimeout: 10000,
    baseUrl: 'http://localhost:4200',
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      configFile: 'cypress.reporter-config.json',
    },
    setupNodeEvents(on, config) {
      on('after:spec', (spec, results) => {
        if (results && results.video) {
          const failures = results.tests.some((test) => test.attempts.some((attempt) => attempt.state === 'failed'));
          if (!failures) {
            fs.unlinkSync(results.video);
          }
        }
      });
    },
  },
});
