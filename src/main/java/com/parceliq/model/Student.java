package com.parceliq.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Model: Student
 * Syllabus Concept: Java Encapsulation, Constructors, Collections
 * Represents a registered University student with shelter pickup credentials,
 * login authentication (name, password, roll no / regNo), profile info, rewards, and friends.
 */
public class Student {
    private String regNo;
    private String name;
    private String password;
    private String phone;
    private String dateOfBirth;
    private String hostelBlock;
    private String roomNumber;
    private int rewardPoints;
    private int giftTokens;
    private List<String> friendRegNos;

    public Student(String regNo, String name, String password, String phone, String dateOfBirth,
                   String hostelBlock, String roomNumber, int rewardPoints, int giftTokens) {
        this.regNo = regNo;
        this.name = name;
        this.password = password != null ? password : "password123";
        this.phone = phone != null ? phone : "";
        this.dateOfBirth = dateOfBirth != null ? dateOfBirth : "";
        this.hostelBlock = hostelBlock != null ? hostelBlock : "";
        this.roomNumber = roomNumber != null ? roomNumber : "";
        this.rewardPoints = rewardPoints;
        this.giftTokens = giftTokens;
        this.friendRegNos = new ArrayList<>();
    }

    public Student(String regNo, String name, String password) {
        this(regNo, name, password, "", "", "", "", 100, 2);
    }

    public Student(String regNo, String name, String phone, String dateOfBirth,
                   String hostelBlock, String roomNumber) {
        this(regNo, name, "password123", phone, dateOfBirth, hostelBlock, roomNumber, 100, 2);
    }

    public Student(String regNo, String name, String phone, String dateOfBirth,
                   String hostelBlock, String roomNumber, int rewardPoints, int giftTokens) {
        this(regNo, name, "password123", phone, dateOfBirth, hostelBlock, roomNumber, rewardPoints, giftTokens);
    }

    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getHostelBlock() { return hostelBlock; }
    public void setHostelBlock(String hostelBlock) { this.hostelBlock = hostelBlock; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public int getRewardPoints() { return rewardPoints; }
    public void setRewardPoints(int rewardPoints) { this.rewardPoints = rewardPoints; }

    public int getGiftTokens() { return giftTokens; }
    public void setGiftTokens(int giftTokens) { this.giftTokens = giftTokens; }

    public List<String> getFriendRegNos() { return friendRegNos; }
    public void setFriendRegNos(List<String> friendRegNos) { this.friendRegNos = friendRegNos; }

    public void addRewardPoints(int points) {
        this.rewardPoints += points;
        if (this.rewardPoints >= 100) {
            this.giftTokens += (points / 50);
        }
    }
}
