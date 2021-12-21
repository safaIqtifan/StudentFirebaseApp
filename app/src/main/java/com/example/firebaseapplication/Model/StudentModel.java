package com.example.firebaseapplication.Model;

public class StudentModel {

    public String id;
    public String name;
    public double average;
    public String photo;

    public StudentModel() {
    }

    public StudentModel(String id, String name, double average, String photo) {
        this.id = id;
        this.name = name;
        this.average = average;
        this.photo = photo;
    }
}
