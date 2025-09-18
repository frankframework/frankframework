import { defineConfig } from 'cypress';
// @ts-expect-error node:fs is a Node module
import * as fs from 'node:fs';

export default defineConfig({
  video: true,
  e2e: {
    experimentalStudio: true,
    defaultCommandTimeout: 10000,
    baseUrl: 'http://localhost:4200',
    specPattern: 'e2e/**/*.cy.{js,jsx,ts,tsx}',
    supportFile: 'support/e2e.ts',
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      configFile: 'reporter-config.json',
    },
    videosFolder: 'test-results/videos',
    screenshotsFolder: 'test-results/screenshot',
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
