import { Component } from "@angular/core";
import { SidebarService } from "src/angularjs/app/components/pages/sidebar.service";

@Component({
  selector: 'app-hamburger',
  template: '<a class="hamburger btn btn-primary " href="" (click)="toggleSidebar()"><i class="fa fa-bars"></i></a>'
})
export class HamburgerComponent {
  constructor(private Sidebar: SidebarService){}
	toggleSidebar() { this.Sidebar.toggle() };
}
