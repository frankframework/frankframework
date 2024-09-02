import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoggingAddComponent } from './logging-add.component';

describe('LoggingAddComponent', () => {
  let component: LoggingAddComponent;
  let fixture: ComponentFixture<LoggingAddComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [LoggingAddComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(LoggingAddComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
