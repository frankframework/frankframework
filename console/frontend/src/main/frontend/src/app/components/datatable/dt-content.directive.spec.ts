import { DtContentDirective } from './dt-content.directive';
import { Component, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

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
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [DtContentDirective, TestComponent],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding
  });

  it('should create an instance', () => {
    expect(fixture.componentInstance.content).toBeTruthy();
  });
});
