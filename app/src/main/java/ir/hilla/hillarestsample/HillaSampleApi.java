package ir.hilla.hillarestsample;

import java.util.ArrayList;
import java.util.List;

import ir.hilla.rest.gson.HillaGsonBuilder;
import ir.hilla.rest.rest.HillaRestCallback;
import ir.hilla.rest.rest.HillaRestHttpConnection;
import ir.hilla.rest.rest.base.HillaRestBaseCallApi;
import ir.hilla.rest.rest.base.HillaRestParamModel;

public class HillaSampleApi extends HillaRestBaseCallApi {


    public HillaSampleApi() {
        super(new HillaRestHttpConnection.Builder()
                .header(new HillaSampleHeadersApi("h1", "h2", "h3"))
                .baseUrl(SampleRestConfig.getBaseUrl())
                .readTimeout(10000)
                .connectionTimeout(15000)
                .addConverter(HillaSampleGsonConverterFactory.create(
                        new HillaGsonBuilder().create()))
                .build());


    }

    public void getPostModel(
            HillaRestCallback<PostSampleModel> callback) {

        SampleBodyModel sampleBodyModel = new SampleBodyModel("b1", "b2");
        List<HillaRestParamModel> paramModels = new ArrayList<>();
        paramModels.add(new HillaRestParamModel("param1", "p1"));
        paramModels.add(new HillaRestParamModel("param2", "p2"));

        post(SampleRestConfig.getPostUrl(), PostSampleModel.class, sampleBodyModel, paramModels, callback);

    }


}
