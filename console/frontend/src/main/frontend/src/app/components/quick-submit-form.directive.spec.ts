import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { QuickSubmitFormDirective } from './quick-submit-form.directive';
import { By } from '@angular/platform-browser';

@Component({
  standalone: true,
  template: `<form (submit)="changeTrigger()">
    <textarea appQuickSubmitForm></textarea>
  </form>`,
  imports: [QuickSubmitFormDirective],
})
class TestComponent {
  triggered = false;
  changeTrigger(): void {
    this.triggered = true;
  }
}

describe('QuickSubmitFormDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElement: DebugElement;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [TestComponent, QuickSubmitFormDirective],
      declarations: [],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    directiveElement = fixture.debugElement.query(By.directive(QuickSubmitFormDirective));
  });

  it('emits event when ctrl+enter is pressed', () => {
    const event = new KeyboardEvent('keydown', {
      ctrlKey: true,
      key: 'Enter',
    });
    directiveElement.nativeElement.dispatchEvent(event);

    expect(fixture.componentInstance.triggered).toBe(true);
  });
});
