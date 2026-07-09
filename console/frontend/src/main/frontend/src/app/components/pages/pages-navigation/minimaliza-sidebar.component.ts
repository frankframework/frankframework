import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { AppService } from '../../../app.service';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';
import { faAngleDoubleLeft, faAngleDoubleRight } from '@fortawesome/free-solid-svg-icons';

@Component({
  selector: 'app-minimaliza-sidebar',
  imports: [FaIconComponent],
  styleUrls: ['./minimaliza-sidebar.component.scss'],
  changeDetection: ChangeDetectionStrategy.Eager,
  template: `
    <a class="navbar-minimalize minimalize" tabindex="0" (click)="toggle()"
      ><fa-icon class="left" size="sm" [icon]="faAngleDoubleLeft" /><fa-icon
        class="right"
        size="sm"
        [icon]="faAngleDoubleRight"
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
