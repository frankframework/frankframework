import { FactoryProvider } from "@angular/core";
import { StateService } from "@uirouter/angularjs";

export function $stateServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('$state');
}

export function idleServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Idle');
}

export const $stateServiceProvider: FactoryProvider = {
  provide: StateService,
  useFactory: $stateServiceFactory,
  deps: ['$injector']
};

export const idleServiceProvider: FactoryProvider = {
  provide: angular.idle.IIdleService,
  useFactory: idleServiceFactory,
  deps: ['$injector']
};
