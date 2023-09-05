import { AppService } from "src/angularjs/app/app.service";

export function serviceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('appService');
}

export const appServiceProvider = {
  provide: AppService,
  useFactory: serviceFactory,
  deps: ['$injector']
};
