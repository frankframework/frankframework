import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ToastsContainerComponent } from './toasts-container.component';

describe('ToastsContainerComponent', () => {
  let component: ToastsContainerComponent;
  let fixture: ComponentFixture<ToastsContainerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ ToastsContainerComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ToastsContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
