package edu.montana.csci.csci440.helpers;

import edu.montana.csci.csci440.controller.EmployeesController;
import edu.montana.csci.csci440.model.Employee;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmployeeHelper {

    public static String makeEmployeeTree() {
        // TODO, change this to use a single query operation to get all employees
        Employee boss = Employee.find(1); // root boss
        // and use this data structure to maintain reference information needed to build the tree structure
        Map<Long, List<Employee>> employeeMap = new HashMap<>();
        List<Employee> allEmployees = Employee.all();
        for (Employee emp : allEmployees) {
            List<Employee> employees = employeeMap.get(emp.getReportsTo());
            if(employees==null){
                employees = new ArrayList<>();
                employeeMap.put(emp.getReportsTo(), employees);
            }
            employees.add(emp);
        }
        return "<ul>" + makeTree(boss, employeeMap) + "</ul>";
    }

    // TODO - currently this method just uses the employee.getReports() function, which
    //  issues a query.  Change that to use the employeeMap variable instead
    public static String makeTree(Employee employee, Map<Long, List<Employee>> employeeMap) {
        StringBuilder list = new StringBuilder();
        list.append("<li><a href='/employees/").append(employee.getEmployeeId()).append("'>")
                .append(employee.getEmail()).append("</a><ul>");

        // Use employeeMap to get direct reports instead of querying
        List<Employee> reports = employeeMap.get(employee.getEmployeeId());
        if (reports != null) {
            for (Employee report : reports) {
                list.append(makeTree(report, employeeMap));
            }
        }

        list.append("</ul></li>");
        return list.toString();
    }
}
