import { Component } from '@angular/core';
import { SidebarService } from '../sidebar.service';

@Component({
  selector: 'app-hamburger',
  template: '<a class="hamburger btn btn-primary" (click)="toggleSidebar()"><i class="fa fa-bars"></i></a>',
  standalone: true,
})
export class HamburgerComponent {
  constructor(private Sidebar: SidebarService) {}
  toggleSidebar(): void {
    this.Sidebar.toggle();
  }
}
