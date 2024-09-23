import { Component } from '@angular/core';
import { AppService } from '../../../app.service';

@Component({
  selector: 'app-minimaliza-sidebar',
  template:
    '<a class="navbar-minimalize minimalize" (click)="toggle()"><i class="fa left fa-angle-double-left"></i><i class="fa right fa-angle-double-right"></i></a>',
  standalone: true,
})
export class MinimalizaSidebarComponent {
  constructor(private appService: AppService) {}

  toggle(): void {
    this.appService.toggleSidebar();
  }
}
