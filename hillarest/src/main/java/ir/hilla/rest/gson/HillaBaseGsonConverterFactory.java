package ir.hilla.rest.gson;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

public abstract class HillaBaseGsonConverterFactory {

    public abstract <T> void modelToJsonConverter(T model, OutputStream outputStream) throws IOException;

    public abstract  <T> T jsonToModelConverter(String json, Class<T> clazz) ;

}
