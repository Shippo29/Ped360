// Configuración de PRODUCCIÓN (build con --configuration production).
// azure.* y las IP/puertos reales se terminan de fijar cuando se despliegue
// a EC2 + Entra ID (ver checklist pendiente del proyecto).
export const environment = {
  production: true,
  ordersApiUrl: 'https://o2d0br883c.execute-api.us-east-1.amazonaws.com/orders-api/api',
  catalogApiUrl: 'https://o2d0br883c.execute-api.us-east-1.amazonaws.com/catalog-api/api',
  azure: {
    clientId: 'd383250f-c257-4aa9-8203-f395b0ea0ab2',
    tenantId: '76d549dd-1b81-4a0d-97d5-09040b6180c4',
    redirectUri: 'http://localhost:4200',
    scope: 'api://17ee25a4-2f2f-44f0-b467-5133a1e81e24/access_as_user'
  }
};
