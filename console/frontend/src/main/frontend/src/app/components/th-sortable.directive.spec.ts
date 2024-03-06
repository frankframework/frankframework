import { Component } from '@angular/core';
import { ThSortableDirective } from './th-sortable.directive';
import { ComponentFixture, TestBed } from '@angular/core/testing';

@Component({
  standalone: true,
  template: `<!-- TODO -->`,
  imports: [ThSortableDirective],
})
class TestComponent {}

describe('ThSortableDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [ThSortableDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });
});
