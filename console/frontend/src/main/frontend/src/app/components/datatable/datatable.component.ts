import { Component, Input, OnInit } from '@angular/core';
import { CdkTableModule, DataSource } from '@angular/cdk/table';
import { CommonModule } from '@angular/common';
import { BehaviorSubject, Observable } from 'rxjs';
import { FormsModule } from '@angular/forms';

export type TableOptions = {
  sizeOptions: number[];
  serverSide: boolean;
};

export type DataTableColumn<T> = {
  name: string;
  displayName: string;
  property: keyof T;
  html?: boolean;
};

@Component({
  selector: 'app-datatable',
  standalone: true,
  imports: [CommonModule, FormsModule, CdkTableModule],
  templateUrl: './datatable.component.html',
  styleUrl: './datatable.component.scss',
})
export class DatatableComponent<T> implements OnInit /*, OnChanges*/ {
  @Input({ required: true }) public datasource!: DataTableDataSource<T>;
  @Input({ required: true }) public displayColumns: DataTableColumn<T>[] = [];

  protected searchQuery = '';

  protected get displayedColumns(): string[] {
    return this.displayColumns.map((column) => column.name);
  }

  ngOnInit(): void {
    console.log(this.displayColumns);
    console.log(this.displayedColumns);
    console.log(this.datasource);
  }

  /*ngOnChanges(changes: SimpleChanges): void {
    if (changes['displayColumns']) {
      this.displayedColumns = this.displayColumns.map((column) => column.name);
    }
  }*/
}

export class DataTableDataSource<T> extends DataSource<T> {
  private _data = new BehaviorSubject<T[]>([]);
  private _filter = new BehaviorSubject<string>('');
  private _renderData = new BehaviorSubject<T[]>([]);
  private _options = new BehaviorSubject<TableOptions>({
    sizeOptions: [50, 100, 250, 500],
    serverSide: false,
  });

  private filteredData: T[] = [];

  get data(): T[] {
    return this._data.value;
  }

  set data(value: T[]) {
    this._data.next(value);
    this.filterData(value);
  }

  get options(): TableOptions {
    return this._options.value;
  }

  set options(value: Partial<TableOptions>) {
    this._options.next({ ...this._options.value, ...value });
  }

  get filter(): string {
    return this._filter.value;
  }

  set filter(value: string) {
    this._filter.next(value);
    this.filterData(this.data);
  }

  connect(): Observable<T[]> {
    return this._data;
  }

  disconnect(): void {
    this._data.complete();
  }

  private filterData(data: T[]): void {
    this.filteredData = this.filter === '' ? data : data.filter((row) => JSON.stringify(row).includes(this.filter));
    this._renderData.next(this.filteredData);
  }
}
