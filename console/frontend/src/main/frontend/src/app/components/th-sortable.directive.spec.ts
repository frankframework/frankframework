import { Component, DebugElement, QueryList, ViewChildren } from '@angular/core';
import { SortEvent, ThSortableDirective, basicTableSort } from './th-sortable.directive';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
  template: `
    <table>
      <thead>
        <tr>
          <th sortable="name" (sorted)="onSort($event)">Name</th>
          <th sortable="value" (sorted)="onSort($event)">Size</th>
        </tr>
      </thead>
      <tbody>
        @for (item of items; track item.name) {
          <tr>
            <td>{{ item.name }}</td>
            <td>{{ item.value }}</td>
          </tr>
        }
      </tbody>
    </table>
  `,
  imports: [ThSortableDirective],
})
class TestComponent {
  items = [
    { name: 'a', value: 2 },
    { name: 'b', value: 1 },
  ];

  @ViewChildren(ThSortableDirective) headers!: QueryList<ThSortableDirective>;

  onSort(event: SortEvent): void {
    this.items = basicTableSort(this.items, this.headers, event);
  }
}

describe('ThSortableDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElements: DebugElement[];
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [ThSortableDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    directiveElements = fixture.debugElement.queryAll(By.directive(ThSortableDirective));
  });

  it('on click switches from asc to desc', () => {
    directiveElements[0].nativeElement.click();
    const directiveInstance = directiveElements[0].injector.get(ThSortableDirective);

    expect(directiveInstance.direction).toBe('ASC');
    directiveElements[0].nativeElement.click();
    expect(directiveInstance.direction).toBe('DESC');
  });

  it('sorts table rows', () => {
    const directive0Instance = directiveElements[0].injector.get(ThSortableDirective);
    const directive0Element = directiveElements[0].nativeElement;
    const directive1Instance = directiveElements[1].injector.get(ThSortableDirective);
    const directive1Element = directiveElements[1].nativeElement;

    directive0Element.click();
    expect(directive0Instance.direction).toBe('ASC');
    expect(fixture.componentInstance.items[0]).toEqual({
      name: 'a',
      value: 2,
    });

    directive0Element.click();
    expect(directive0Instance.direction).toBe('DESC');
    expect(fixture.componentInstance.items[0]).toEqual({
      name: 'b',
      value: 1,
    });

    directive1Element.click();
    expect(directive1Instance.direction).toBe('ASC');
    expect(fixture.componentInstance.items[0]).toEqual({
      name: 'b',
      value: 1,
    });

    directive1Element.click();
    expect(directive1Instance.direction).toBe('DESC');
    expect(fixture.componentInstance.items[0]).toEqual({
      name: 'a',
      value: 2,
    });
  });
});
