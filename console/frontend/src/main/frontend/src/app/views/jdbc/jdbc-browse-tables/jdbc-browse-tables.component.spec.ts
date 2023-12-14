import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JdbcBrowseTablesComponent } from './jdbc-browse-tables.component';

describe('JdbcBrowseTablesComponent', () => {
  let component: JdbcBrowseTablesComponent;
  let fixture: ComponentFixture<JdbcBrowseTablesComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      declarations: [ JdbcBrowseTablesComponent ]
    })
    .compileComponents();

    fixture = TestBed.createComponent(JdbcBrowseTablesComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
