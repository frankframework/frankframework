import { ComponentFixture, TestBed } from '@angular/core/testing';

import { LiquibaseComponent } from './liquibase.component';

describe('LiquibaseComponent', () => {
  let component: LiquibaseComponent;
  let fixture: ComponentFixture<LiquibaseComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ LiquibaseComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(LiquibaseComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
