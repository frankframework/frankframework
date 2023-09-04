import { StateService } from "@uirouter/angularjs";

export function $stateServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('$state');
}

export const $stateServiceProvider = {
  provide: StateService,
  useFactory: $stateServiceFactory,
  deps: ['$injector']
};
