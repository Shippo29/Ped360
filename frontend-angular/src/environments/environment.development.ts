// Configuración de DESARROLLO (usada por "ng serve" / "npm start").
// El frontend le habla solo al BFF (puerto 8082), que es quien reenvía a
// ms-pedidos360-orders y ms-pedidos360-catalog. Por eso ambas URLs apuntan
// al mismo host: ya no hay necesidad de que el navegador conozca los
// puertos 8080/8081 directamente.
export const environment = {
  production: false,
  ordersApiUrl: 'http://localhost:8082/api',
  catalogApiUrl: 'http://localhost:8082/api',
  azure: {
    clientId: 'd383250f-c257-4aa9-8203-f395b0ea0ab2',
    tenantId: '76d549dd-1b81-4a0d-97d5-09040b6180c4',
    redirectUri: 'http://localhost:4200',
    scope: 'api://17ee25a4-2f2f-44f0-b467-5133a1e81e24/access_as_user'
  }
};
