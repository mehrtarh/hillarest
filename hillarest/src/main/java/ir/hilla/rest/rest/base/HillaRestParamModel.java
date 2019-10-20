package ir.hilla.rest.rest.base;

public class HillaRestParamModel {

    private String key;
    private String value;

    public HillaRestParamModel(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
