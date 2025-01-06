import {
  Directive,
  ElementRef,
  EventEmitter,
  HostListener,
  inject,
  Input,
  OnInit,
  Output,
  QueryList,
} from '@angular/core';
import { anyCompare, compare } from '../utils';

export type SortDirection = 'asc' | 'desc' | null;
export type SortEvent = {
  column: string | number;
  direction: SortDirection;
};

export function updateSortableHeaders(headers: QueryList<ThSortableDirective>, column: string | number | symbol): void {
  for (const header of headers) {
    if (header.sortable !== column) {
      header.updateDirection(null);
    }
  }
}

export function basicTableSort<T extends Record<string, string | number>>(
  array: T[],
  headers: QueryList<ThSortableDirective>,
  { column, direction }: SortEvent,
): T[] {
  updateSortableHeaders(headers, column);

  if (direction == null || column == '') return array;

  return [...array].sort((a, b) => {
    const order = compare(a[column], b[column]);
    return direction === 'asc' ? order : -order;
  });
}

/** Doesn't support all keyof types sadly, for now only string | number */
export function basicAnyValueTableSort<T>(
  array: T[],
  headers: QueryList<ThSortableDirective>,
  { column, direction }: SortEvent,
): T[] {
  updateSortableHeaders(headers, column);

  if (direction == null || column == '') return array;

  return [...array].sort((a, b) => {
    const order = anyCompare(a[column as keyof T], b[column as keyof T]);
    return direction === 'asc' ? order : -order;
  });
}

@Directive({
  selector: 'th[sortable]',
})
export class ThSortableDirective implements OnInit {
  @Input() sortable: string = '';
  @Input() direction: SortDirection = null;
  @Output() sorted = new EventEmitter<SortEvent>();

  private nextSortOption(sortOption: SortDirection): SortDirection {
    switch (sortOption) {
      case null: {
        return 'asc';
      }
      case 'asc': {
        return 'desc';
      }
      case 'desc': {
        return null;
      }
      default: {
        return sortOption as never;
      }
    }
  }
  private headerText = '';
  private elementRef: ElementRef<HTMLTableCellElement> = inject(ElementRef);

  ngOnInit(): void {
    this.headerText = this.elementRef.nativeElement.innerHTML;
  }

  updateIcon(direction: SortDirection): void {
    let updateColumnName = '';
    updateColumnName =
      direction == null
        ? this.headerText
        : this.headerText +
          (direction == 'asc' ? ' <i class="fa fa-arrow-up"></i>' : ' <i class="fa fa-arrow-down"></i>');
    this.elementRef.nativeElement.innerHTML = updateColumnName;
  }

  updateDirection(newDirection: SortDirection): void {
    this.direction = newDirection;
    this.updateIcon(this.direction);
  }

  @HostListener('click') nextSort(): void {
    this.updateDirection(this.nextSortOption(this.direction));
    this.sorted.emit({ column: this.sortable, direction: this.direction });
  }
}
