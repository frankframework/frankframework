describe('Keycloak', () => {
  it('should allow making a request to keycloak', () => {
    const base = Cypress.env('keycloakUrl');
    const realm = Cypress.env('realm');
    const discoveryUrl = `${base}/realms/${realm}/.well-known/openid-configuration`;

    cy.log(`Trying to discover if realm: ${realm} exists on: ${discoveryUrl}`);
    cy.request({
      url: discoveryUrl,
      retryOnStatusCodeFailure: true,
      retryOnNetworkFailure: true,
    }).its('status').should('eq', 200);
  });
})
