package org.toyotatransportationplanning;

import java.util.Date;

public class Order {
    String grade;
    String id;
    Date date;
    String source;
    String destination;
    int size;
    Integer assignedVehicleID;
    String assignedVehicle;
    String assignedUnit;

    public Order(String grade, String id, Date date, String source, String destination, int size) {
        this.grade = grade;
        this.id = id;
        this.date = date;
        this.source = source;
        this.destination = destination;
        this.size = size;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public Integer getAssignedVehicleID() {
        return assignedVehicleID;
    }

    public void setAssignedVehicleID(Integer assignedVehicleID) {
        this.assignedVehicleID = assignedVehicleID;
    }

    public String getAssignedVehicle() {
        return assignedVehicle;
    }

    public void setAssignedVehicle(String assignedVehicle) {
        this.assignedVehicle = assignedVehicle;
    }

    public String getAssignedUnit() {
        return assignedUnit;
    }

    public void setAssignedUnit(String assignedUnit) {
        this.assignedUnit = assignedUnit;
    }
}
