import { Directive, ElementRef, EventEmitter, Input, OnInit, Output, QueryList } from '@angular/core';

export type SortDirection = 'asc' | 'desc' | null;
export type SortEvent = {
  column: string;
  direction: SortDirection;
}

export function updateSortableHeaders(headers: QueryList<ThSortableDirective>, column: string) {
  for (const header of headers) {
    if (header.sortable !== column) {
      header.direction = null;
    }
  }
}


const compare = (v1: string | number, v2: string | number) => (v1 < v2 ? -1 : v1 > v2 ? 1 : 0);
export function basicTableSort<T extends Record<string, any>>(arr: T[], headers: QueryList<ThSortableDirective>, { column, direction }: SortEvent): T[] {
  updateSortableHeaders(headers, column);

  if (direction == null || column == '')
    return arr;

  return [...arr].sort((a, b) => {
    const order = compare(a[column], b[column]);
    return direction === 'asc' ? order : -order;
  });
}

@Directive({
  selector: 'th[sortable]',
  host: {
    // '[class.asc]': 'direction === "asc"',
    // '[class.desc]': 'direction === "desc"',
    '(click)': 'nextSort()',
  }
})
export class ThSortableDirective implements OnInit {
  @Input() sortable: string = '';
  @Input() direction: SortDirection = null;
  @Output() onSort = new EventEmitter<SortEvent>();

  private nextSortOption: Record<string, SortDirection> = {
    '': 'asc',
    asc: 'desc',
    desc: null
  }
  private headerText = "";

  constructor(private elementRef: ElementRef<HTMLTableCellElement>) { }

  ngOnInit() {
    this.headerText = this.elementRef.nativeElement.innerHTML;
  }

  nextSort() {
    this.direction = this.nextSortOption[this.direction ?? ''];

    let updateColumnName = "";
    if (this.direction == null)
      updateColumnName = this.headerText;
    else {
      updateColumnName = this.headerText + (
        this.direction == 'asc' ? ' <i class="fa fa-arrow-up"></i>' : ' <i class="fa fa-arrow-down"></i>'
      );
    }
    this.elementRef.nativeElement.innerHTML = updateColumnName;

    this.onSort.emit({ column: this.sortable, direction: this.direction });
  }

}
