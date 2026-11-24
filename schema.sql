-- OPTIONAL: the app creates payroll.db and these tables automatically on first run.
-- This file is only for reference. The app starts with no employees.
CREATE TABLE IF NOT EXISTS employees (
  emp_id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL, email TEXT, phone TEXT, department TEXT, designation TEXT,
  join_date TEXT, basic_salary REAL NOT NULL DEFAULT 0
);
CREATE TABLE IF NOT EXISTS salary (
  sal_id INTEGER PRIMARY KEY AUTOINCREMENT,
  emp_id INTEGER NOT NULL, month_year TEXT NOT NULL,
  basic REAL, hra REAL, da REAL, allowances REAL, pf REAL, tax REAL, other_deductions REAL,
  gross REAL, total_deductions REAL, net REAL,
  generated_on TEXT DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (emp_id, month_year),
  FOREIGN KEY (emp_id) REFERENCES employees(emp_id) ON DELETE CASCADE
);
