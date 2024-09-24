import { AfterViewInit, Component, ContentChild, Input, OnDestroy } from '@angular/core';
import { CdkTableModule, DataSource } from '@angular/cdk/table';
import { CommonModule } from '@angular/common';
import { BehaviorSubject, Observable, Subscription } from 'rxjs';
import { FormsModule } from '@angular/forms';
import { DtContentDirective } from './dt-content.directive';
import { TruncatePipe } from '../../pipes/truncate.pipe';
import { ToDateDirective } from '../to-date.directive';

export type TableOptions = {
  sizeOptions: number[];
  size: number;
  serverSide: boolean;
  filter: boolean;
};

export type DataTableColumn<T> = {
  name: string;
  displayName: string;
  property: keyof T | null;

  html?: boolean;
  className?: string;
  hidden?: boolean;
};

export type DataTableEntryInfo = {
  minPageEntry: number;
  maxPageEntry: number;
  totalFilteredEntries: number;
  totalEntries: number;
};

export type DataTableServerRequestInfo = {
  size: number;
  offset: number;
  sort: 'asc' | 'desc';
};

export type DataTableServerResponseInfo<T> = {
  totalEntries: number;
  filteredEntries: number;
  size: number;
  offset: number;
  data: T[];
};

@Component({
  selector: 'app-datatable',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkTableModule, TruncatePipe, ToDateDirective, ToDateDirective],
  templateUrl: './datatable.component.html',
  styleUrl: './datatable.component.scss',
})
export class DatatableComponent<T> implements AfterViewInit, OnDestroy {
  @Input({ required: true }) public datasource!: DataTableDataSource<T>;
  @Input({ required: true }) public displayColumns: DataTableColumn<T>[] = [];
  @Input() public truncate: boolean = false;
  @Input() public truncateLength: number = 30;

  @ContentChild(DtContentDirective) protected content!: DtContentDirective<T>;
  protected totalFilteredEntries: number = 0;
  protected totalEntries: number = 0;
  protected minPageEntry: number = 0;
  protected maxPageEntry: number = 0;

  protected get displayedColumns(): string[] {
    return this.displayColumns.map((column) => column.name);
  }

  private datasourceSubscription: Subscription = new Subscription();

  ngAfterViewInit(): void {
    if (this.datasource) {
      const subscription = this.datasource.getEntriesInfo().subscribe((entriesInfo) => {
        this.totalEntries = entriesInfo.totalEntries;
        this.totalFilteredEntries = entriesInfo.totalFilteredEntries;
        this.minPageEntry = entriesInfo.minPageEntry;
        this.maxPageEntry = entriesInfo.maxPageEntry;
      });
      this.datasourceSubscription.add(subscription);
    }
  }

  ngOnDestroy(): void {
    this.datasourceSubscription.unsubscribe();
  }

  applyFilter(event: Event): void {
    const filterValue = (event.target as HTMLInputElement).value;
    this.datasource.filter = filterValue.trim();
  }

  applyPaginationSize(sizeValue: string): void {
    this.datasource.options = { size: +sizeValue };
  }

  updatePage(pageNumber: number): void {
    this.datasource.updatePage(pageNumber);
  }

  checkIfNotDate(column: DataTableColumn<T>): boolean {
    return !column.className?.includes('date');
  }
}

export class DataTableDataSource<T> extends DataSource<T> {
  private _data = new BehaviorSubject<T[]>([]);
  private _filter = new BehaviorSubject<string>('');
  private _renderData = new BehaviorSubject<T[]>([]);
  private _options = new BehaviorSubject<TableOptions>({
    sizeOptions: [10, 50, 100, 250, 500],
    size: 50,
    filter: true,
    serverSide: false,
  });
  private _entriesInfo = new BehaviorSubject<DataTableEntryInfo>({
    minPageEntry: 0,
    maxPageEntry: 0,
    totalFilteredEntries: 0,
    totalEntries: 0,
  });
  private _entriesInfo$ = this._entriesInfo.asObservable();

  private filteredData: T[] = [];
  private _currentPage: number = 1;
  private _totalPages: number = 0;
  private serverRequestId: number = -1;
  private serverRequestFn?: (value: DataTableServerRequestInfo) => PromiseLike<DataTableServerResponseInfo<T>>;

  get data(): T[] {
    return this._data.value;
  }

  set data(value: T[]) {
    this._data.next(value);

    if (!this.options.serverSide) this.updateRenderedData(value);
  }

  get options(): TableOptions {
    return this._options.value;
  }

  set options(value: Partial<TableOptions>) {
    this._options.next({ ...this._options.value, ...value });
    this.updatePage(1);
  }

  get filter(): string {
    return this._filter.value;
  }

  set filter(value: string) {
    this._filter.next(value);
    if (this.currentPage !== 1) {
      this.updatePage(1);
      return;
    }
    this.updateRenderedData(this.data);
  }

  get totalPages(): number {
    return this._totalPages;
  }

  get currentPage(): number {
    return this._currentPage;
  }

  connect(): Observable<T[]> {
    if (this.options.serverSide) {
      this.updateDataFromEndpoint();
    }
    return this._renderData;
  }

  disconnect(): void {}

  getEntriesInfo(): Observable<DataTableEntryInfo> {
    return this._entriesInfo$;
  }

  updatePage(page: number): void {
    this._currentPage = page;
    this.updateRenderedData(this.data);
  }

  updateTable(): void {
    this.updateRenderedData(this.data);
  }

  setServerRequest(
    requestFunction: (value: DataTableServerRequestInfo) => PromiseLike<DataTableServerResponseInfo<T>>,
  ): void {
    if (!this.options.serverSide) return;
    this.serverRequestFn = requestFunction;
  }

  private updateRenderedData(data: T[]): void {
    if (this.options.serverSide) {
      this.updateDataFromEndpoint();
      return;
    }

    const filteredData = this.filterData(data);
    this.paginateData(filteredData);
  }

  private updateDataFromEndpoint(): void {
    Promise.resolve<DataTableServerRequestInfo>({
      size: this.options.size,
      offset: (this._currentPage - 1) * this.options.size,
      sort: 'asc',
    })
      .then(this.serverRequestFn)
      .then((response) => {
        this.data = response.data;
        this._totalPages = Math.ceil(response.totalEntries / this.options.size);
        this._renderData.next(this.data);
        this._entriesInfo.next({
          minPageEntry: response.offset + 1,
          maxPageEntry: response.offset + response.size,
          totalFilteredEntries: response.filteredEntries,
          totalEntries: response.totalEntries,
        });
      })
      .catch((error) => {
        this.data = [];
        console.error(error);
      });
  }

  private paginateData(data: T[]): T[] {
    const currentStart = (this._currentPage - 1) * this.options.size;
    const paginatedData = data.slice(currentStart, currentStart + this.options.size);
    this._renderData.next(paginatedData);
    this._entriesInfo.next({
      minPageEntry: (this.currentPage - 1) * this.options.size + 1,
      maxPageEntry: Math.min(this.currentPage * this.options.size, this.filteredData.length),
      totalFilteredEntries: this.filteredData.length,
      totalEntries: this.data.length,
    });
    return paginatedData;
  }

  private filterData(data: T[]): T[] {
    this.filteredData = this.filter === '' ? data : data.filter((row) => this.filterPredicate(row, this.filter));
    this._totalPages = Math.ceil(this.filteredData.length / this.options.size);
    this._renderData.next(this.filteredData);
    return this.filteredData;
  }

  // from https://github.com/angular/components/blob/main/src/material/table/table-data-source.ts#L231
  private filterPredicate(data: T, filter: string): boolean {
    // Transform the data into a lowercase string of all property values.
    const dataString = Object.keys(data as unknown as Record<string, unknown>)
      .reduce((currentTerm: string, key: string) => {
        // Use an obscure Unicode character to delimit the words in the concatenated string.
        // This avoids matches where the values of two columns combined will match the user's query
        // (e.g. `Flute` and `Stop` will match `Test`). The character is intended to be something
        // that has a very low chance of being typed in by somebody in a text field. This one in
        // particular is "White up-pointing triangle with dot" from
        // https://en.wikipedia.org/wiki/List_of_Unicode_characters
        return `${currentTerm + (data as unknown as Record<string, unknown>)[key]}◬`;
      }, '')
      .toLowerCase();

    // Transform the filter by converting it to lowercase and removing whitespace.
    const transformedFilter = filter.trim().toLowerCase();
    return dataString.includes(transformedFilter);
  }
}
