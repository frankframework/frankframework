import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToDateDirective } from './to-date.directive';

@Component({
  standalone: true,
  template: `<!-- TODO -->`,
  imports: [ToDateDirective],
})
class TestComponent {}

describe('ToDateDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [ToDateDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });
});
