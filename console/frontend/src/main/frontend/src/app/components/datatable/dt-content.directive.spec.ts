import { DtContentDirective } from './dt-content.directive';
import { Component, DebugElement, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';

@Component({
  standalone: true,
  template: `<ng-template appDtContent>TEST</ng-template>`,
  imports: [DtContentDirective],
})
class TestComponent {
  @ViewChild(DtContentDirective) content!: DtContentDirective<object>;
}

describe('DtContentDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  // let directiveElement: DebugElement;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [DtContentDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    // directiveElement = fixture.debugElement.query(By.directive(DtContentDirective));
  });

  it('should create an instance', () => {
    expect(fixture.componentInstance.content).toBeTruthy();
  });
});
