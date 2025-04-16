package com.example.transferly.models;

import java.util.List;

public class SharedFolderModel {
    private Long id;
    private String folderName;
    private List<String> members;

    public Long getId() { return id; }
    public String getFolderName() { return folderName; }
    public List<String> getMembers() { return members; }
}
