import { AlertService } from "src/angularjs/app/services/alert.service";
import { SessionService } from "src/angularjs/app/services/session.service";

export function alertServiceFactory($injector: any) {
  return $injector.get('Alert');
}

export function sessionServiceFactory($injector: any) {
  return $injector.get('Session');
}

export const alertServiceProvider = {
  provide: AlertService,
  useFactory: alertServiceFactory,
  deps: ['$injector']
};

export const sessionServiceProvider = {
  provide: SessionService,
  useFactory: sessionServiceFactory,
  deps: ['$injector']
};
