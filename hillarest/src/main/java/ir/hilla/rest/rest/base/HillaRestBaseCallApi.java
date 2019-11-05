package ir.hilla.rest.rest.base;

import java.util.List;

import ir.hilla.rest.rest.HillaRestCallback;
import ir.hilla.rest.rest.HillaRestHttpConnection;

public abstract class HillaRestBaseCallApi {

//    private String response = "";

    private final HillaRestHttpConnection hillaHttpRestConnection;

    protected HillaRestBaseCallApi(HillaRestHttpConnection hillaHttpRestConnection) {
        this.hillaHttpRestConnection = hillaHttpRestConnection;
    }

    public final <Result> void get(String url, Class<Result> resultClass, List<HillaRestParamModel> params, HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, params, "GET", callback);
    }

    public final <Result> void get(String url, Class<Result> resultClass, HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, "GET", callback);
    }

    public final <T, Result> void post(String url, Class<Result> resultClass, T bodyModel, List<HillaRestParamModel> params,
                                       HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, bodyModel, params, "POST", callback);
    }

    public final <T, Result> void post(String url, Class<Result> resultClass, T bodyModel,
                                       HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, bodyModel, "POST", callback);
    }

    public final <Result> void post(String url, Class<Result> resultClass,
                                    HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, "POST", callback);
    }

    public final <Result> void post(String url, Class<Result> resultClass, List<HillaRestParamModel> params,
                                    HillaRestCallback<Result> callback) {
        hillaHttpRestConnection.openConnection(url, resultClass, params, "POST", callback);
    }


}
