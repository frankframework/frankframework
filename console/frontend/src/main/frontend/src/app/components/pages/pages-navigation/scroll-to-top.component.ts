import { Component } from "@angular/core";

@Component({
  selector: 'app-scroll-to-top',
  template: '<div class="scroll-to-top"><a title="Scroll to top" (click)="scrollTop()"><i class="fa fa-arrow-up"></i> <span class="nav-label">Scroll To Top</span></a></div>'
})
export class ScrollToTopComponent {
  scrollTop() {
    $(window).scrollTop(0);
  };
}
