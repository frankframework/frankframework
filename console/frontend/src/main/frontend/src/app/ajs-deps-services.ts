import { FactoryProvider } from "@angular/core";
import { StateService, StateParams } from "@uirouter/angularjs";

export function $stateServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('$state');
}

export function $stateParamsServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('$stateParams');
}

export function idleServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Idle');
}

export const $stateServiceProvider: FactoryProvider = {
  provide: StateService,
  useFactory: $stateServiceFactory,
  deps: ['$injector']
}
export const $stateParamsServiceProvider: FactoryProvider = {
  provide: StateParams,
  useFactory: $stateParamsServiceFactory,
  deps: ['$injector']
}
