import { Component, inject } from '@angular/core';
import { AppService } from '../../../app.service';

@Component({
  selector: 'app-hamburger',
  template: '<a class="hamburger btn btn-primary" (click)="toggle()"><i class="fa fa-bars"></i></a>',
  standalone: true,
})
export class HamburgerComponent {
  private readonly appService: AppService = inject(AppService);

  toggle(): void {
    this.appService.toggleSidebar();
  }
}
