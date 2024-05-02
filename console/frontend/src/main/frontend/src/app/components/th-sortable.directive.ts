import {
  Directive,
  ElementRef,
  EventEmitter,
  HostListener,
  Input,
  OnInit,
  Output,
  QueryList,
} from '@angular/core';

export type SortDirection = 'asc' | 'desc' | null;
export type SortEvent = {
  column: string;
  direction: SortDirection;
};

export function updateSortableHeaders(
  headers: QueryList<ThSortableDirective>,
  column: string,
): void {
  for (const header of headers) {
    if (header.sortable !== column) {
      header.updateDirection(null);
    }
  }
}

const compare = (v1: string | number, v2: string | number): 1 | -1 | 0 =>
  v1 < v2 ? -1 : v1 > v2 ? 1 : 0;
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

@Directive({
  selector: 'th[sortable]',
  standalone: true,
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

  constructor(private elementRef: ElementRef<HTMLTableCellElement>) {}

  ngOnInit(): void {
    this.headerText = this.elementRef.nativeElement.innerHTML;
  }

  updateIcon(direction: SortDirection): void {
    let updateColumnName = '';
    updateColumnName =
      direction == null
        ? this.headerText
        : this.headerText +
          (direction == 'asc'
            ? ' <i class="fa fa-arrow-up"></i>'
            : ' <i class="fa fa-arrow-down"></i>');
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
