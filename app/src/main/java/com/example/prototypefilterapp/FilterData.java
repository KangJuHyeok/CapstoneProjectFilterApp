package com.example.prototypefilterapp;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

public class FilterData {

    private static FilterData instance;
    private List<FilterItem> filterList;

    public static class FilterItem {
        public String filterName;
        public String filterDescription;
        public String filterType;
        public List<String> filterTags;

        public FilterItem(String filterName, String filterDescription, String filterType, String filterTagsString) {
            this.filterName = filterName;
            this.filterDescription = filterDescription;
            this.filterType = filterType;
            this.filterTags = new ArrayList<>();
            String[] tags = filterTagsString.split(",");
            for (String tag : tags) {
                this.filterTags.add(tag.trim());
            }
        }
        public JsonObject toJsonObject() {
            JsonObject json = new JsonObject();
            json.addProperty("filterName", filterName);
            json.addProperty("filterDescription", filterDescription);
            json.addProperty("filterType", filterType);
            return json;
        }
    }

    // 싱글톤 인스턴스를 얻는 메서드
    public static FilterData getInstance() {
        if (instance == null) {
            instance = new FilterData();
        }
        return instance;
    }

    private FilterData() {
        filterList = new ArrayList<>();

        filterList.add(new FilterItem("따뜻한 아날로그", "따스하고 어두운 아날로그 느낌의 필터", "#unpack @blend ol grainFilm5.jpg 95 @adjust saturation 0.80 @adjust brightness -0.1 @adjust contrast 0.80 @adjust whitebalance 0.40 1", "평온,향수,차분"));
        filterList.add(new FilterItem("옛날 디카", "날카롭고 선명한 옛날 디지털 카메라 느낌의 필터", "@adjust sharpen 5 @adjust contrast 1.2 @adjust saturation 1.4", "기쁨,즐거움,활기"));
        filterList.add(new FilterItem("석양의 아날로그", "석양빛이 매우 강한 아날로그 느낌의 필터", "#unpack @blend ol grainFilm.jpg 70 @adjust lut late_sunset.png @adjust saturation 1.1 @adjust brightness 0.2 @adjust contrast 1.1 @adjust whitebalance 0.1 1", "설렘,애정,열정"));
        filterList.add(new FilterItem("필름카메라 빛샘", "필름에 빛이 들어간 느낌의 필터", "#unpack @blend ol grainFilm6.jpg 90 @adjust saturation 1.1  @adjust contrast 0.9", "우울,슬픔,불안,아련"));
        filterList.add(new FilterItem("한 줄기 빛샘", "필름에 빛샘 효과가 한 줄기만 들어간 필터", "#unpack @blend ol grainFilm4.jpg 70 @adjust saturation 1.0 @adjust brightness 0.2 @adjust contrast 1.20", "희망,기대,설렘"));
        filterList.add(new FilterItem("은은한 무지개 빛", "은은한 무지개 빛이 비치는 필터", "#unpack @blend ol hehe.jpg 50 @adjust saturation 0.90 @adjust brightness 0.1 @adjust contrast 0.90 @adjust whitebalance 0.30 1", "몽환,신비,평온"));
        filterList.add(new FilterItem("어두운 안개", "짙은 회색 빛의 안개가 낀 느낌의 필터", "@adjust lut foggy_night.png @adjust brightness 0.3 @adjust contrast 0.80 @adjust whitebalance 0.20 1", "분노,좌절,무서움"));
        filterList.add(new FilterItem("상쾌한 빛", "기존의 노란빛이 좀 더 하얗게 변함", "@adjust lut wildbird.png @adjust saturation 1.2 @adjust brightness 0.2", "행복,기쁨,편안"));
        filterList.add(new FilterItem("밝고 선명", "기존의 빛들이 많이 밝아지고 선명해짐", "@adjust lut filmstock.png @adjust sharpen 2 @adjust contrast 1.2", "만족,기쁨,자신감"));
        filterList.add(new FilterItem("따뜻한 촛불", "은은한 촛불처럼 따스하고 로맨틱한 분위기", "@adjust lut candlelight.png @adjust saturation 1.1 @adjust brightness 0.1", "편안한,사랑스러운,만족스러운"));
        filterList.add(new FilterItem("청량한 블루", "푸른색이 강조되어 맑고 차가운 느낌", "@adjust lut drop_blues.png @adjust contrast 1.15 @adjust brightness 0.05", "고요한,시원한,평화로운"));
        filterList.add(new FilterItem("극적인 대비", "일광화 효과가 적용된 강한 대비와 독특한 색감", "@adjust lut solarize.png @adjust saturation 1.2 @adjust contrast 1.3", "기대하는,긴장되는,활기찬"));
        filterList.add(new FilterItem("밝은 활력", "전체적으로 밝고 생동감 있는 색상 톤", "@adjust lut herderite.png @adjust brightness 0.15 @adjust saturation 1.25", "기쁜,즐거운,행복한"));
        filterList.add(new FilterItem("시네마틱 필름", "후지 이터나 필름의 시네마틱한 색 분리 효과", "@adjust lut fuji_eterna_250d_fuji_3510.png @adjust contrast 1.1", "흥미로운,만족스러운,편안한"));
        filterList.add(new FilterItem("긴장된 녹색", "낮은 채도와 녹색 톤으로 긴장감 있는 분위기", "@adjust lut tension_green.png @adjust saturation 0.9 @adjust contrast 1.05", "불안한,걱정스러운,긴장되는"));
        filterList.add(new FilterItem("하이퍼 선명", "색상이 선명하고 대비가 높아 강렬한 인상을 줌", "@adjust lut hypersthene.png @adjust sharpen 3 @adjust contrast 1.25", "자신있는,당황스러운,흥분한"));
        filterList.add(new FilterItem("적외선 판타지", "적외선 사진처럼 비현실적이고 몽환적인 색감", "@adjust lut infra-false-color.png @adjust saturation 1.1", "놀란,무서운,신비한"));
        filterList.add(new FilterItem("어둠 속 달빛", "깊고 어두운 푸른색으로 달빛 아래의 느낌을 표현", "@adjust lut moonlight.png @adjust brightness -0.2 @adjust contrast 1.1", "슬픈,외로운,그리운"));
    }

    public FilterItem getFilterItemByName(String name) {
        for (FilterItem item : filterList) {
            if (item.filterName.equals(name)) {
                return item;
            }
        }
        return null;
    }
    public FilterItem getFilterItemByType(String type) {
        for (FilterItem item : filterList) {
            if (item.filterType.equals(type)) {
                return item;
            }
        }
        return null;
    }

    public List<FilterItem> getFilterList() {
        return filterList;
    }
}