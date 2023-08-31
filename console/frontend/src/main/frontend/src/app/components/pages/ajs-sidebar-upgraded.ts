import { SidebarService } from "src/angularjs/app/components/pages/sidebar.service";

export function serviceFactory($injector: angular.auto.IInjectorService) {
  return $injector.get('Sidebar');
}

export const sidebarServiceProvider = {
  provide: SidebarService,
  useFactory: serviceFactory,
  deps: ['$injector']
};
