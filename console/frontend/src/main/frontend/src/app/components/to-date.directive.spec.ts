import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ToDateDirective } from './to-date.directive';
import { By } from '@angular/platform-browser';

@Component({
  // standalone: true,
  template: `<!-- TODO -->`,
  imports: [ToDateDirective],
})
class TestComponent {}

describe('ToDateDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElements: DebugElement[];
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [TestComponent, ToDateDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    // all elements with an attached HighlightDirective
    directiveElements = fixture.debugElement.queryAll(
      By.directive(ToDateDirective),
    );
  });

  // temporatory, remove when making actual tests
  it('directive element list should exist', () => {
    expect(directiveElements).toBeTruthy();
  });
});
