import { Component } from "@angular/core";
import { SidebarService } from "../sidebar.service";

@Component({
  selector: 'app-minimaliza-sidebar',
  template: '<a class="navbar-minimalize minimalize" href="" (click)="toggleSidebar()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>'
})
export class MinimalizaSidebarComponent {
  constructor(private Sidebar: SidebarService){}
	toggleSidebar() { this.Sidebar.toggle() };
}
