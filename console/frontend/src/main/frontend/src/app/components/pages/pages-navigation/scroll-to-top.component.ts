import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-scroll-to-top',
  template:
    '<div class="scroll-to-top hidden-scroll"><a title="Scroll to top" (click)="scrollTop()"><i class="fa fa-arrow-up"></i> <span class="nav-label">Scroll To Top</span></a></div>',
  standalone: true,
})
export class ScrollToTopComponent {
  @Input() navElem!: HTMLElement;

  scrollTop(): void {
    document.body.scrollTo({ top: 0 });
    this.navElem.scrollTo({ top: 0 });
  }
}
