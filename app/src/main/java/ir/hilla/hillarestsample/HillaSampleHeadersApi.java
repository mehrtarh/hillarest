package ir.hilla.hillarestsample;

import java.util.ArrayList;
import java.util.List;

import ir.hilla.rest.rest.base.HillaRestBaseHeader;
import ir.hilla.rest.rest.base.HillaRestHeaderModel;

public class HillaSampleHeadersApi extends HillaRestBaseHeader {

    private final String header1;
    private final String header2;
    private final String header3;

    public HillaSampleHeadersApi(String header1, String header2, String header3) {
        this.header1 = header1;
        this.header2 = header2;
        this.header3 = header3;
    }


    @Override
    public List<HillaRestHeaderModel> getHeaders() {
        List<HillaRestHeaderModel> headers = new ArrayList<>();


        headers.add(new HillaRestHeaderModel(
                "header1",
                header1
        ));
        headers.add(new HillaRestHeaderModel(
                "header2",
                header2
        ));
        headers.add(new HillaRestHeaderModel(
                "header3",
                header3
        ));


        return headers;
    }
}
