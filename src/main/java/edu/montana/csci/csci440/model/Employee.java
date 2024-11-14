package edu.montana.csci.csci440.model;

import edu.montana.csci.csci440.util.DB;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Employee extends Model {

    private Long employeeId;
    private Long reportsTo;
    private String firstName;
    private String lastName;
    private String email;
    private String title;

    public Employee() {
        // new employee for insert
    }

    private Employee(ResultSet results) throws SQLException {
        firstName = results.getString("FirstName");
        lastName = results.getString("LastName");
        email = results.getString("Email");
        employeeId = results.getLong("EmployeeId");
        reportsTo = results.getLong("ReportsTo");
        title = results.getString("Title");
    }

    public static List<Employee.SalesSummary> getSalesSummaries() {
        List<SalesSummary> salesSummaries = new ArrayList<>();
        String query = """
            SELECT e.FirstName, e.LastName, e.Email,
                   COUNT(i.InvoiceId) AS SalesCount,
                   SUM(i.Total) AS SalesTotal
            FROM employees e
            JOIN customers c ON e.EmployeeId = c.SupportRepId
            JOIN invoices i ON c.CustomerId = i.CustomerId
            GROUP BY e.EmployeeId, e.FirstName, e.LastName, e.Email
            """;
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                salesSummaries.add(new SalesSummary(results));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return salesSummaries;
    }

    @Override
    public boolean verify() {
        _errors.clear(); // clear any existing errors
        if (firstName == null || "".equals(firstName)) {
            addError("FirstName can't be null or blank!");
        }
        // TODO - add in additional validations:        finished
        //   last name can't be null or blank
        //   email can't be null or blank and must contain an @
        if (lastName == null || "".equals(lastName)) {
            addError("LastName can't be null or blank!");
        }
        if (email == null || "".equals(email) || !email.contains("@")) {
            addError("Email can't be null or blank and must contain an @!");
        }
        return !hasErrors();
    }

    @Override
    public boolean  update() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE employees SET FirstName=?, LastName=?, Email=? WHERE EmployeeId=?")) {
                stmt.setString(1, this.getFirstName());
                stmt.setString(2, this.getLastName());
                stmt.setString(3, this.getEmail());
                stmt.setLong(4, this.getEmployeeId());
                stmt.executeUpdate();
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean create() {
        if (verify()) {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO employees (FirstName, LastName, Email) VALUES (?, ?, ?)")) {
                stmt.setString(1, this.getFirstName());
                stmt.setString(2, this.getLastName());
                stmt.setString(3, this.getEmail());
                stmt.executeUpdate();
                employeeId = DB.getLastID(conn);
                return true;
            } catch (SQLException sqlException) {
                throw new RuntimeException(sqlException);
            }
        } else {
            return false;
        }
    }

    @Override
    public void delete() {
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public List<Customer> getCustomers() {
        return Customer.forEmployee(employeeId);
    }

    public Long getReportsTo() {
        return reportsTo;
    }

    public void setReportsTo(Long reportsTo) {
        this.reportsTo = reportsTo;
    }

    public List<Employee> getReports() {
        try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM employees WHERE ReportsTo=?")) {
                ArrayList<Employee> result = new ArrayList<>();
                stmt.setLong(1, getEmployeeId());
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Employee(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public Employee getBoss() {
        if(reportsTo != null){
            return Employee.find(reportsTo);
        }
        return null;
    }

    public static List<Employee> all() {
        return all(0, Integer.MAX_VALUE);
    }

    public static List<Employee> all(int page, int count) {
         try {
            try(Connection connect = DB.connect();
                PreparedStatement stmt = connect.prepareStatement("SELECT  * FROM employees LIMIT ? OFFSET ?")) {
                ArrayList<Employee> result = new ArrayList<>();
                stmt.setInt(1, count);
                stmt.setInt(2, (page - 1) * count);
                ResultSet resultSet = stmt.executeQuery();
                while (resultSet.next()) {
                    result.add(new Employee(resultSet));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Employee findByEmail(String newEmailAddress) {
        try (Connection conn = DB.connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM employees WHERE Email = ?")) {
            stmt.setString(1, newEmailAddress);
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return new Employee(resultSet);
            } else {
                return null;  // No employee found
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Employee find(long employeeId) {
        try {
            try (Connection conn = DB.connect();
                 PreparedStatement stmt = conn.prepareStatement(
                         "SELECT * FROM Employees WHERE EmployeeId = ?")) {
                stmt.setLong(1, employeeId);
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    return new Employee(resultSet);
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void setTitle(String programmer) {
        title = programmer;
    }

    public void setReportsTo(Employee employee) {
        if (employee != null) {
            this.reportsTo = employee.getEmployeeId();
        }
        else{
            this.reportsTo = null;
        }
    }

    public static class SalesSummary {
        private String firstName;
        private String lastName;
        private String email;
        private Long salesCount;
        private BigDecimal salesTotals;
        private SalesSummary(ResultSet results) throws SQLException {
            firstName = results.getString("FirstName");
            lastName = results.getString("LastName");
            email = results.getString("Email");
            salesCount = results.getLong("SalesCount");
            salesTotals = results.getBigDecimal("SalesTotal");
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getEmail() {
            return email;
        }

        public Long getSalesCount() {
            return salesCount;
        }

        public BigDecimal getSalesTotals() {
            return salesTotals;
        }
    }
}
