import { Directive, ElementRef, inject, Input } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { LinkName } from '../views/security-items/security-items.service';

@Directive({
  selector: '[appHasAccessToLink]',
  standalone: true,
})
export class HasAccessToLinkDirective {
  private readonly authService: AuthService = inject(AuthService);
  private readonly elementRef: ElementRef<HTMLElement> = inject(ElementRef);

  @Input() noAccessToLinkClassName: string = 'disabled';

  @Input('appHasAccessToLink') set hasAccessToLink(linkNames: LinkName | LinkName[]) {
    linkNames = typeof linkNames === 'string' ? [linkNames] : linkNames;

    this.elementRef.nativeElement.classList.add(this.noAccessToLinkClassName);
    this.elementRef.nativeElement.style.pointerEvents = 'none';

    this.authService.loadPermissions().then(() => {
      const hasAccess = linkNames.some((name) => this.authService.hasAccessToLink(name));

      if (hasAccess) {
        this.elementRef.nativeElement.classList.remove(this.noAccessToLinkClassName);
        this.elementRef.nativeElement.style.pointerEvents = 'auto';
      }
    });
  }
}
