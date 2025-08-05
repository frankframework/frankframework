import { Directive, ElementRef, inject, Input, OnInit } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { LinkName } from '../views/security-items/security-items.service';

@Directive({
  selector: '[appHasAccessToLink]',
})
export class HasAccessToLinkDirective implements OnInit {
  private readonly authService: AuthService = inject(AuthService);
  private readonly elementRef: ElementRef<HTMLElement> = inject(ElementRef);

  private linkNames!: LinkName[];

  @Input() noAccessToLinkClassName = 'disabled';

  @Input({ required: true, alias: 'appHasAccessToLink' }) set hasAccessToLink(linkNames: LinkName | LinkName[]) {
    this.linkNames = typeof linkNames === 'string' ? [linkNames] : linkNames;
  }

  ngOnInit(): void {
    this.elementRef.nativeElement.classList.add(this.noAccessToLinkClassName);
    this.elementRef.nativeElement.style.pointerEvents = 'none';

    this.authService.loadPermissions().then(() => {
      const hasAccess = this.linkNames.some((name) => this.authService.hasAccessToLink(name));

      if (hasAccess) {
        this.elementRef.nativeElement.classList.remove(this.noAccessToLinkClassName);
        this.elementRef.nativeElement.style.removeProperty('pointer-events');
      }
    });
  }
}
