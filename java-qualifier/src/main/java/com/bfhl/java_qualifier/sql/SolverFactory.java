package com.bfhl.java_qualifier.sql;

public class SolverFactory {
    private final SqlSolver q1 = new Question1Solver();
    private final SqlSolver q2 = new Question2Solver();

    public SqlSolver forRegNo(String regNo) {
        String digits = regNo.replaceAll("\\D", "");
        int last2 = digits.isEmpty() ? 0 : Integer.parseInt(digits.substring(Math.max(0, digits.length()-2)));
        return (last2 % 2 == 0) ? q2 : q1;
    }
}
