package ir.hilla.rest.rest.base;

public class HillaRestHeaderModel {

    private final String key;
    private final String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public HillaRestHeaderModel(String key, String value) {
        this.key = key;
        this.value = value;
    }
}
