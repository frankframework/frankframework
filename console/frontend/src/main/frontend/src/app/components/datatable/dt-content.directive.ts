import { Directive, TemplateRef } from '@angular/core';

export interface DtContent<T> {
  rowElement: T;
}

@Directive({
  selector: '[appDtContent]',
  standalone: true,
})
export class DtContentDirective<T> {
  constructor(public templateReference: TemplateRef<DtContent<T>>) {}
}
