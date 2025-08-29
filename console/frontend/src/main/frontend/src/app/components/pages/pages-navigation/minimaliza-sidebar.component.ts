import { Component, inject } from '@angular/core';
import { AppService } from '../../../app.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faAngleDoubleLeft, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-minimaliza-sidebar',
  imports: [FaIconComponent],
  styleUrls: ['./minimaliza-sidebar.component.scss'],
  template: `
    <a class="navbar-minimalize minimalize" (click)="toggle()"
      ><fa-icon class="left" [icon]="faAngleDoubleLeft" size="sm" /><fa-icon
        class="right"
        [icon]="faAngleDoubleRight"
        size="sm"
    /></a>
  `,
})
export class MinimalizaSidebarComponent {
  protected readonly faAngleDoubleLeft = faAngleDoubleLeft;
  protected readonly faAngleDoubleRight = faAngleDoubleRight;
  private appService = inject(AppService);

  toggle(): void {
    this.appService.toggleSidebar();
  }
}
