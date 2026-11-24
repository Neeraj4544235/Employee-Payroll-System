# Employee Payroll System

Java desktop application - **Swing GUI + SQLite (JDBC)**. No database server needed: data is stored in one file, `payroll.db`, created automatically in the project folder.

## Features
- **Dashboard**: employee count, departments, average basic, this month's net payout, department table and bar chart
- **Employees**: add / update / delete with input checks; search, department filter, salary range, 5 sort options, CSV export
- **Payroll**: calculate, preview slip, save, export slip (.txt), print, and **Run Payroll for ALL Employees**
- **Reports**: filter slips by month, department and name; totals; delete a slip; CSV export

## Salary rules
| Item | Formula |
|---|---|
| HRA | 20% of Basic |
| DA | 10% of Basic |
| Gross | Basic + HRA + DA + Allowances |
| PF | 12% of Basic |
| Income tax | Annual slabs on Gross x 12 (0% to 3L, 5% 3-7L, 10% 7-10L, 15% 10-12L, 20% 12-15L, 30% above), divided by 12 |
| Net Pay | Gross - (PF + Tax + Other Deductions) |

## Requirements
1. **JDK 11+** (`javac -version` to check)
2. Internet on first run (script downloads the SQLite JDBC driver into `lib/`)

## Run
- Windows: double-click `run.bat`
- Mac / Linux: `chmod +x run.sh && ./run.sh`

### Manual commands
Windows:
```
javac -cp lib\sqlite-jdbc-3.36.0.3.jar -d out src\payroll\*.java
java -cp "out;lib\sqlite-jdbc-3.36.0.3.jar" payroll.Main
```
Mac / Linux:
```
javac -cp lib/sqlite-jdbc-3.36.0.3.jar -d out src/payroll/*.java
java -cp "out:lib/sqlite-jdbc-3.36.0.3.jar" payroll.Main
```

## Troubleshooting
- **ClassNotFoundException: org.sqlite.JDBC / No suitable driver** -> driver jar missing in `lib/`. Download `sqlite-jdbc-3.36.0.3.jar` from Maven Central (org/xerial/sqlite-jdbc) and put it in `lib/`.
- **javac not recognized** -> install a JDK from https://adoptium.net and reopen the terminal.
- **Start fresh** -> close the app and delete `payroll.db`.
- View data: open `payroll.db` with "DB Browser for SQLite" (free).

## Structure
```
payroll-system/
  src/payroll/  Main, MainFrame, Theme, Refreshable, DashboardPanel, EmployeePanel,
                PayrollPanel, ReportPanel, Employee, SalaryRecord, PayrollCalculator,
                EmployeeDAO, SalaryDAO, DBConnection
  docs/ (project details), schema.sql (optional), run.sh, run.bat, README.md
```
