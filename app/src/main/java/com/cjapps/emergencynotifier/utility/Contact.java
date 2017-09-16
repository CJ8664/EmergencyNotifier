package com.cjapps.emergencynotifier.utility;

/**
 * This is a simple class to store contact info
 */
public class Contact {

    // This stores the type of object. Contact or Section header
    public final int type;

    // Contact details
    public String name;
    public String id;

    public boolean isSelected;

    public int sectionPosition;
    public int listPosition;

    public Contact(int type, String name, String id) {

        this.type = type;
        this.name = name;
        this.id = id;
    }

    @Override public String toString() {
        return name;
    }
}
