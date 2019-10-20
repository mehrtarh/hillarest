package ir.hilla.hillarestsample;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import ir.hilla.rest.gson.HillaBaseGsonConverterFactory;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.stream.HillaJsonWriter;
import ir.hilla.rest.gson.HillaGson;
import ir.hilla.rest.gson.stream.HillaJsonWriter;


public final class HillaSampleGsonConverterFactory extends HillaBaseGsonConverterFactory {// extends Converter.Factory{

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    public static HillaBaseGsonConverterFactory create() {
        return create(new HillaGson());
    }

    /**
     * Create an instance using {@code gson} for conversion. Encoding to JSON and
     * decoding from JSON (when no charset is specified by a header) will use UTF-8.
     */
    public static HillaBaseGsonConverterFactory create(HillaGson gson) {
        if (gson == null) throw new NullPointerException("gson == null");
        return new HillaSampleGsonConverterFactory(gson);
    }

    private final HillaGson gson;

    private HillaSampleGsonConverterFactory(HillaGson gson) {
        this.gson = gson;
    }

    @Override
    public <T> void modelToJsonConverter(T model, OutputStream outputStream) throws IOException {

        final HillaJsonWriter writer = new HillaJsonWriter(new OutputStreamWriter(outputStream, UTF_8));
        gson.toJson(model, model.getClass(), writer);
        writer.flush();
        writer.close();

    }

    @Override
    public <T> T jsonToModelConverter(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);

    }


}
