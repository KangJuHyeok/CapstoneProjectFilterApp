package com.example.prototypefilterapp;

// AI 필터링된 Base64 데이터를 임시로 저장하는 Singleton 클래스
public class GlobalData {
    private static GlobalData instance;
    private String filteredImageBase64Data;

    private GlobalData() {}

    public static synchronized GlobalData getInstance() {
        if (instance == null) {
            instance = new GlobalData();
        }
        return instance;
    }

    public void setFilteredImageBase64Data(String data) {
        this.filteredImageBase64Data = data;
    }

    public String getFilteredImageBase64Data() {
        return filteredImageBase64Data;
    }

    public void clearData() {
        this.filteredImageBase64Data = null;
    }
}