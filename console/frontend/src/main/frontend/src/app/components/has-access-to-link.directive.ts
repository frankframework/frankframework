import { Directive, inject, Input, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { LinkName } from '../views/security-items/security-items.service';
import { lastValueFrom } from 'rxjs';

@Directive({
  selector: '[appHasAccessToLink]',
  standalone: true,
})
export class HasAccessToLinkDirective {
  private readonly authService: AuthService = inject(AuthService);
  private readonly templateRef = inject(TemplateRef);
  private readonly viewContainer: ViewContainerRef = inject(ViewContainerRef);

  @Input('appHasAccessToLink') set hasAccessToLink(linkName: LinkName | LinkName[]) {
    if (typeof linkName === 'string') {
      linkName = [linkName];
    }

    this.authService
      .loadPermissions()
      .then(() => {
        const hasAccess = linkName.some((name) => this.authService.hasAccessToLink(name));

        if (hasAccess) {
          this.viewContainer.createEmbeddedView(this.templateRef);
        } else {
          this.viewContainer.clear();
        }
      })
      .catch(() => {
        this.viewContainer.clear();
      });
  }
}
