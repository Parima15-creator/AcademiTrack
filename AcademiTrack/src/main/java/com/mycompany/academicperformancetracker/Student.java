/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.academicperformancetracker;

public class Student {
    private int roll;
    private String name;
    private double gpa; // Must match the variable used in your servlet [cite: 1683]

    // This constructor matches the 'new Student(...)' call in your servlet [cite: 1683]
    public Student(int roll, String name, double gpa) {
        this.roll = roll;
        this.name = name;
        this.gpa = gpa;
    }

    public int getRoll() { return roll; }
    public String getName() { return name; }
    public double getGpa() { return gpa; } // This matches s.getGpa() [cite: 1688]
}