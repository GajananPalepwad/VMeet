package com.gn4k.vmeet;

public class MyData {
    private String myField;

    public MyData() {
        // Default constructor required for Firebase
    }

    public MyData(String myField) {
        this.myField = myField;
    }

    public String getMyField() {
        return myField;
    }

    public void setMyField(String myField) {
        this.myField = myField;
    }
}
