package com.anas.strata;

public class Main {
    public static void main(String[] args) {
        MemTable table = new MemTable();
        table.put("Ajay", "98xxx");
        System.out.println(table.get("Ajay"));
        table.delete("Ajay");

        try {
            System.out.println(table.get("Ajay"));
        } catch (ResourceNotFoundException e) {
            System.out.println("Rejected: " + e.getMessage());
        }

        try {
            System.out.println(table.get("Rahul"));
        } catch (ResourceNotFoundException e) {
            System.out.println("Rejected: " + e.getMessage());
        }
    }
}