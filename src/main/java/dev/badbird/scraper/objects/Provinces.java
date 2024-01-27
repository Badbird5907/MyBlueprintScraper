package dev.badbird.scraper.objects;

public enum Provinces {
    // public static final String ONTARIO = "8", ALBERTA = "1", BRITISH_COLUMBIA = "2", MANITOBA = "3", NEWFOUNDLAND_AND_LABRADOR = "4", NEW_BRUNSWICK = "5", NORTHWEST_TERRITORIES = "6", NOVA_SCOTIA = "7", PRINCE_EDWARD_ISLAND = "9", QUEBEC = "10", SASKATCHEWAN = "11", NUNAVUT = "12", YUKON = "13";
    ONTARIO("8"), ALBERTA("1"), BRITISH_COLUMBIA("2"), MANITOBA("3"), NEWFOUNDLAND_AND_LABRADOR("4"), NEW_BRUNSWICK("5"), NORTHWEST_TERRITORIES("6"), NOVA_SCOTIA("7"), PRINCE_EDWARD_ISLAND("9"), QUEBEC("10"), SASKATCHEWAN("11"), NUNAVUT("12"), YUKON("13");
    public String id;
    Provinces(String id) {
        this.id = id;
    }
    public static String getUrlParam(Provinces in) {
        return "%2C" + in;
    }

    @Override
    public String toString() {
        return id;
    }
}
