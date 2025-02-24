import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    experimentalStudio: true,
    baseUrl: 'http://localhost:4200',
    reporter: 'cypress-multi-reporters',
    reporterOptions: {
      configFile: 'cypress.reporter-config.json'
    },
  },
})
