package com.lms.appenza.hotspotfiletransfer;

public class StudentItem {
    String studentName;
    String studentMAC;
    boolean checked;

    public StudentItem(String name, String mac, boolean checked,boolean receivedQuiz) {
        this.studentName = name;
        this.studentMAC = mac;
        this.checked = checked;
    }

    public String getName() {
        return studentName;
    }

    public void setName(String name) {
        this.studentName = name;
    }

    public String getMAC() {
        return studentMAC;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void toggleChecked() {
        this.checked = !checked;
    }

}
