// EP-18 Despliegue en Azure.
// URL pública del backend (Azure App Service) terminada en /api, sin "/" final.
// Reemplazar por el "Dominio predeterminado" que muestra Azure en la página de la App Service
//(Información general), por ejemplo: https://sicares-api-xxxxxx.brazilsouth-01.azurewebsites.net
export const environment = {
  production: true,
  apiUrl: 'https://REEMPLAZAR-CON-DOMINIO-DE-APP-SERVICE.azurewebsites.net/api',
};
