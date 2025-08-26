import { Component, Input } from '@angular/core';
import { faArrowUp } from '@fortawesome/free-solid-svg-icons';
import { FaIconComponent } from '@fortawesome/angular-fontawesome';

@Component({
  selector: 'app-scroll-to-top',
  imports: [FaIconComponent],
  template:
    '<div class="scroll-to-top hidden-scroll"><a title="Scroll to top" (click)="scrollTop()"><fa-icon [icon]="faArrowUp"></fa-icon> <span class="nav-label">Scroll To Top</span></a></div>',
})
export class ScrollToTopComponent {
  @Input() navElem!: HTMLElement;

  protected readonly faArrowUp = faArrowUp;

  scrollTop(): void {
    document.body.scrollTo({ top: 0 });
    this.navElem.scrollTo({ top: 0 });
  }
}
