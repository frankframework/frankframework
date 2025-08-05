import { Directive, inject, TemplateRef } from '@angular/core';

export type DtContent<T> = {
  rowElement: T;
};

@Directive({
  selector: '[appDtContent]',
  standalone: true,
})
export class DtContentDirective<T> {
  public templateReference = inject(TemplateRef<DtContent<T>>);
}
