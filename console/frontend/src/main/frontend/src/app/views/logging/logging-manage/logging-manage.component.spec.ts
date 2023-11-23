import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LoggingManageComponent } from './logging-manage.component';

describe('LoggingManageComponent', () => {
  let component: LoggingManageComponent;
  let fixture: ComponentFixture<LoggingManageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LoggingManageComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LoggingManageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
