import { Directive, ElementRef, EventEmitter, HostListener, inject, Input, Output, QueryList } from '@angular/core';
import { anyCompare, compare } from '../utils';

export type SortDirection = 'ASC' | 'DESC' | 'NONE';
export type SortEvent = {
  column: string | number;
  direction: SortDirection;
};

export function updateSortableHeaders(headers: QueryList<ThSortableDirective>, column: string | number | symbol): void {
  for (const header of headers) {
    if (header.sortable !== column) {
      header.updateDirection('NONE');
    }
  }
}

export function basicTableSort<T extends Record<string, string | number>>(
  array: T[],
  headers: QueryList<ThSortableDirective>,
  { column, direction }: SortEvent,
): T[] {
  updateSortableHeaders(headers, column);

  if (direction == 'NONE' || column == '') return array;

  return [...array].sort((a, b) => {
    const order = compare(a[column], b[column]);
    return direction === 'ASC' ? order : -order;
  });
}

/** Doesn't support all keyof types sadly, for now only string | number */
export function basicAnyValueTableSort<T>(
  array: T[],
  headers: QueryList<ThSortableDirective>,
  { column, direction }: SortEvent,
): T[] {
  updateSortableHeaders(headers, column);

  if (direction == 'NONE' || column == '') return array;

  return [...array].sort((a, b) => {
    const order = anyCompare(a[column as keyof T], b[column as keyof T]);
    return direction === 'ASC' ? order : -order;
  });
}

@Directive({
  // eslint-disable-next-line @angular-eslint/directive-selector
  selector: 'th[sortable]',
})
export class ThSortableDirective {
  @Input() sortable = '';
  @Input() direction: SortDirection = 'NONE';
  @Output() sorted = new EventEmitter<SortEvent>();

  private elementReference: ElementRef<HTMLTableCellElement> = inject(ElementRef);
  private THElement = this.elementReference.nativeElement;

  @HostListener('click') nextSort(): void {
    this.updateDirection(this.nextSortOption(this.direction));
    this.sorted.emit({ column: this.sortable, direction: this.direction });
  }

  updateIcon(direction: SortDirection): void {
    const icon = this.THElement.querySelector('span.sort-icon');
    if (icon) {
      icon.remove();
    }
    if (direction === 'NONE') return;
    const iconElement = document.createElement('span');
    iconElement.classList.add('sort-icon');
    iconElement.innerHTML = direction == 'ASC' ? '&uarr;' : '&darr;';
    this.THElement.append(iconElement);
  }

  updateDirection(newDirection: SortDirection): void {
    this.direction = newDirection;
    this.updateIcon(this.direction);
  }

  private nextSortOption(sortOption: SortDirection): SortDirection {
    switch (sortOption) {
      case 'NONE': {
        return 'ASC';
      }
      case 'ASC': {
        return 'DESC';
      }
      default: {
        return 'NONE';
      }
    }
  }
}
