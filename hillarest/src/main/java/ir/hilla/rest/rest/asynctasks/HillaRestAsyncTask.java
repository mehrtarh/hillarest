package ir.hilla.rest.rest.asynctasks;

import android.os.AsyncTask;

import java.util.List;

import ir.hilla.rest.rest.HillaRestResponse;
import ir.hilla.rest.rest.base.HillaRestParamModel;

public class HillaRestAsyncTask<T, Result> extends AsyncTask<Void, Void, HillaRestResponse<Result>> {


    private final String url;
    private final T bodyModel;
    private final List<HillaRestParamModel> params;
    private final SendSDKAsyncTaskCallBack<T, Result> callBack;
    private final String requestType;
    private final Class<Result> resultClass;

    public HillaRestAsyncTask(String url, Class<Result> resultClass, T bodyModel, List<HillaRestParamModel> params, String requestType
            , SendSDKAsyncTaskCallBack<T, Result> callBack) {
        this.url = url;
        this.bodyModel = bodyModel;
        this.params = params;
        this.callBack = callBack;
        this.requestType = requestType;
        this.resultClass = resultClass;
    }

    @Override
    protected HillaRestResponse<Result> doInBackground(Void... hillaPayCallbacks) {
        return callBack.execute(url, resultClass, bodyModel, params, requestType);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected void onPostExecute(HillaRestResponse<Result> result) {
        super.onPostExecute(result);
        callBack.result(result);
    }


    public interface SendSDKAsyncTaskCallBack<T, R> {
        HillaRestResponse<R> execute(String url, Class<R> resultClass, T bodyModel, List<HillaRestParamModel> params, String requestType);

        void result(HillaRestResponse<R> result);
    }
}
