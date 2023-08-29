import { AlertService } from "src/angularjs/app/services/alert.service";
import { ApiService } from "src/angularjs/app/services/api.service";
import { AuthService } from "src/angularjs/app/services/authservice.service";
import { Base64Service } from "src/angularjs/app/services/base64.service";
import { CookiesService } from "src/angularjs/app/services/cookies.service";
import { DebugService } from "src/angularjs/app/services/debug.service";
import { GDPRService } from "src/angularjs/app/services/gdpr.service";
import { MiscService } from "src/angularjs/app/services/misc.service";
import { NotificationService } from "src/angularjs/app/services/notification.service";
import { PollerService } from "src/angularjs/app/services/poller.service";
import { SessionService } from "src/angularjs/app/services/session.service";
import { SweetAlertService } from "src/angularjs/app/services/sweetalert.service";
import { ToastrService } from "src/angularjs/app/services/toastr.service";

export function alertServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Alert');
}

export function apiServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Api');
}

export function authServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('authService');
}

export function base64ServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Base64');
}

export function cookiesServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Cookies');
}

export function debugServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Debug');
}

export function gdprServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('GDPR');
}

export function miscServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Misc');
}

export function notificationServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Notification');
}

export function pollerServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Poller');
}

export function sessionServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Session');
}

export function sweetalertServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('SweetAlert');
}

export function toastrServiceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Toastr');
}

export const alertServiceProvider = {
  provide: AlertService,
  useFactory: alertServiceFactory,
  deps: ['$injector']
};

export const apiServiceProvider = {
  provide: ApiService,
  useFactory: apiServiceFactory,
  deps: ['$injector']
};

export const authServiceProvider = {
  provide: AuthService,
  useFactory: authServiceFactory,
  deps: ['$injector']
};

export const base64ServiceProvider = {
  provide: Base64Service,
  useFactory: base64ServiceFactory,
  deps: ['$injector']
};

export const cookiesServiceProvider = {
  provide: CookiesService,
  useFactory: cookiesServiceFactory,
  deps: ['$injector']
};

export const debugServiceProvider = {
  provide: DebugService,
  useFactory: debugServiceFactory,
  deps: ['$injector']
};

export const gdprServiceProvider = {
  provide: GDPRService,
  useFactory: gdprServiceFactory,
  deps: ['$injector']
};

export const miscServiceProvider = {
  provide: MiscService,
  useFactory: miscServiceFactory,
  deps: ['$injector']
};

export const notificationServiceProvider = {
  provide: NotificationService,
  useFactory: notificationServiceFactory,
  deps: ['$injector']
};

export const pollerServiceProvider = {
  provide: PollerService,
  useFactory: pollerServiceFactory,
  deps: ['$injector']
};

export const sessionServiceProvider = {
  provide: SessionService,
  useFactory: sessionServiceFactory,
  deps: ['$injector']
};

export const sweetalertServiceProvider = {
  provide: SweetAlertService,
  useFactory: sweetalertServiceFactory,
  deps: ['$injector']
};

export const toastrServiceProvider = {
  provide: ToastrService,
  useFactory: toastrServiceFactory,
  deps: ['$injector']
};
