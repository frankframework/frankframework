import { Component, DebugElement } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync } from '@angular/core/testing';
import { QuickSubmitFormDirective } from './quick-submit-form.directive';
import { By } from '@angular/platform-browser';

@Component({
  // standalone: true,
  template: `<form>
    <textarea appQuickSubmitForm></textarea>
    <button type="submit" (click)="changeTrigger($event)">Submit</button>
  </form>`,
  imports: [QuickSubmitFormDirective],
})
class TestComponent {
  triggered = false;
  changeTrigger(event: Event): void {
    event.preventDefault();
    this.triggered = true;
    event.stopPropagation();
  }
}

describe('QuickSubmitFormDirective', () => {
  let fixture: ComponentFixture<TestComponent>;
  let directiveElement: DebugElement;
  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [],
      declarations: [TestComponent, QuickSubmitFormDirective],
    }).createComponent(TestComponent);

    fixture.detectChanges(); // initial binding

    directiveElement = fixture.debugElement.query(
      By.directive(QuickSubmitFormDirective),
    );
  });

  it('emits event when ctrl+enter is pressed', fakeAsync(() => {
    const event = new KeyboardEvent('keydown', {
      ctrlKey: true,
      key: 'Enter',
    });
    directiveElement.nativeElement.dispatchEvent(event);

    console.log(fixture.componentInstance.triggered);
    expect(fixture.componentInstance.triggered).toBe(true);
  }));
});
