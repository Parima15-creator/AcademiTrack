/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.academicperformancetracker;

/**
 * The Student class is a Data Transfer Object (DTO).
 * It represents a single student record in the application's memory.
 */
public class Student {
    private int roll;
    private String name;
    private double gpa; // Must match the variable used in your servlet

    /**
     * CONSTRUCTOR
     * Used to initialize a Student object with data retrieved from the database.
     */
    public Student(int roll, String name, double gpa) {
        // Private fields: Encapsulation ensures these cannot be changed directly from outside
        this.roll = roll;
        this.name = name;
        this.gpa = gpa;
    }
    
    // --- GETTER METHODS ---
    // These allow other classes (like Servlets or JSPs) to read the private data.
    public int getRoll() { return roll; }
    public String getName() { return name; }
    public double getGpa() { return gpa; }
}