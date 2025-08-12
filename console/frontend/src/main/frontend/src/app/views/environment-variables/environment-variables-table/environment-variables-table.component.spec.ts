import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EnvironmentVariablesTableComponent } from './environment-variables-table.component';

describe('EnvironmentVariablesTableComponent', () => {
  let component: EnvironmentVariablesTableComponent;
  let fixture: ComponentFixture<EnvironmentVariablesTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EnvironmentVariablesTableComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(EnvironmentVariablesTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
