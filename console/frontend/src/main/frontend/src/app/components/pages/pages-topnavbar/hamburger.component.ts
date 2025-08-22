import { Component, inject } from '@angular/core';
import { AppService } from '../../../app.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faBars } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-hamburger',
  template: '<a class="hamburger btn btn-primary" (click)="toggle()"><fa-icon [icon]="faBars" /></a>',
  standalone: true,
  imports: [FaIconComponent],
})
export class HamburgerComponent {
  protected readonly faBars = faBars;
  private readonly appService: AppService = inject(AppService);

  toggle(): void {
    this.appService.toggleSidebar();
  }
}
