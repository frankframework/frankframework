import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JdbcExecuteQueryComponent } from './jdbc-execute-query.component';

describe('JdbcExecuteQueryComponent', () => {
  let component: JdbcExecuteQueryComponent;
  let fixture: ComponentFixture<JdbcExecuteQueryComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ JdbcExecuteQueryComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JdbcExecuteQueryComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
