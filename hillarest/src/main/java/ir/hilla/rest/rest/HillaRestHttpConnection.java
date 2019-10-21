package ir.hilla.rest.rest;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import ir.hilla.rest.gson.HillaBaseGsonConverterFactory;
import ir.hilla.rest.rest.asynctasks.HillaRestAsyncTask;
import ir.hilla.rest.rest.base.HillaRestBaseHeader;
import ir.hilla.rest.rest.base.HillaRestParamModel;


public class HillaRestHttpConnection {

    private HttpURLConnection httpURLConnection;
    private final URL baseUrl;
    private final HillaRestBaseHeader headers;
    private final int connectionTimeout;
    private final int readTimeout;
    private final HillaBaseGsonConverterFactory converterFactory;

    private HillaRestHttpConnection(Builder builder) {

        this.httpURLConnection = builder.httpURLConnection;
        this.baseUrl = builder.baseUrl;
        this.headers = builder.headers;
        this.readTimeout = builder.readTimeout;
        this.connectionTimeout = builder.connectionTimeout;
        this.converterFactory = builder.converterFactory;


    }


    public HttpURLConnection connection() {
        return httpURLConnection;
    }

    public URL baseUrl() {
        return this.baseUrl;
    }

    public HillaRestBaseHeader headers() {
        return this.headers;
    }

    public int connectionTimeout() {
        return this.connectionTimeout;
    }

    public int readTimeout() {
        return this.readTimeout;
    }

    public HillaBaseGsonConverterFactory converterFactory() {
        return this.converterFactory;
    }

    public <Result> void openConnection(String url, Class<Result> resultClass, String type, HillaRestCallback<Result> callback) {
        this.openConnection(url, resultClass, new ArrayList<HillaRestParamModel>(), type, callback);
    }

    public <Result> void openConnection(String url, Class<Result> resultClass, List<HillaRestParamModel> params, String type,
                                        HillaRestCallback<Result> callback) {
        this.openConnection(url, resultClass, null, params, type, callback);
    }

    public <T, Result> void openConnection(String url, Class<Result> resultClass, T bodyModel, String type, HillaRestCallback<Result> callback) {
        this.openConnection(url, resultClass, bodyModel, new ArrayList<HillaRestParamModel>(), type, callback);
    }


    public <T, Result> void openConnection(String url, Class<Result> resultClass, final T bodyModel, List<HillaRestParamModel> params,
                                           String type, final HillaRestCallback<Result> callback) {


        HillaRestAsyncTask<T, Result> task = new HillaRestAsyncTask<T, Result>(url, resultClass, bodyModel, params, type,

                new HillaRestAsyncTask.SendSDKAsyncTaskCallBack<T, Result>() {
                    @Override
                    public HillaRestResponse<Result> execute(String url, Class<Result> resultClass, T bodyModel, List<HillaRestParamModel> params, String requestType) {
                        try {
                            return privateOpenConnection(url, resultClass, bodyModel, params, requestType);
                        } catch (IOException | IllegalArgumentException | NullPointerException e) {
                            e.printStackTrace();
                            callback.onFailure(e);
                        }
                        return null;
                    }

                    @Override
                    public void result(HillaRestResponse<Result> result) {
                        if (result != null)
                            callback.onResponse(result);
                    }
                });
        task.execute();


    }

    private <T, Result> HillaRestResponse<Result> privateOpenConnection(String url, Class<Result> resultClass,
                                                                        T bodyModel, List<HillaRestParamModel> params,
                                                                        String requestType)
            throws IOException, IllegalArgumentException, NullPointerException {

        if (url != null && this.baseUrl != null) {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();
            URL complexUrl = appendParamToUrl(this.baseUrl.toString() + url, params);


            httpURLConnection = (HttpURLConnection) complexUrl.openConnection();
            httpURLConnection.setRequestMethod(requestType);
            setHeaders();

            if (requestType.equals("POST") && bodyModel != null) {
                httpURLConnection.setDoInput(true);
                httpURLConnection.setDoOutput(true);

                final OutputStream outputStream = new BufferedOutputStream(httpURLConnection.getOutputStream());
                parsModelToJson(bodyModel, outputStream);
            }
            httpURLConnection.connect();
            String response = "";
//            if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try {
                InputStream inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
                response = convertInputStreamToString(inputStream);
            } catch (Exception e) {
                try {
                    InputStream inputStream = new BufferedInputStream(httpURLConnection.getErrorStream());
                    response = convertInputStreamToString(inputStream);
                } catch (Exception ignored) {

                }
            }


            return createResponse(parsJsonToModel(response, resultClass));
//            }

//            return null;
        }

        return null;
    }

    private <T> HillaRestResponse<T> createResponse(T model)
            throws IOException, IllegalArgumentException, NullPointerException {

        int responseCode = httpURLConnection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300)
            return HillaRestResponse.success(httpURLConnection.getResponseCode(),
                    httpURLConnection.getURL().toString(), model);
        else
            return HillaRestResponse.error(httpURLConnection.getResponseCode(),
                    httpURLConnection.getResponseMessage(),httpURLConnection.getURL().toString(), model);
    }

    private String convertInputStreamToString(InputStream inputStream) {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder sb = new StringBuilder();
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuilder displayMessage = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = reader.readLine()) != null) {
                displayMessage.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return displayMessage.toString();
    }

    private <T> void parsModelToJson(T model, OutputStream outputStream) {
        try {
            converterFactory.modelToJsonConverter(model, outputStream);
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }

    }

    private <T> T parsJsonToModel(String json, Class<T> clazz) {
        try {
            if(json==null)
                return null;
            return converterFactory.jsonToModelConverter(json, clazz);
        } catch (Exception  e) {
            e.printStackTrace();
        }
        return null;
    }

    private URL appendParamToUrl(String url, List<HillaRestParamModel> params) throws MalformedURLException {
        if (params == null || params.size() <= 0)
            return new URL(url);
        else {
            StringBuilder tempUrl = new StringBuilder(url + "?");
            for (int i = 0; i < params.size(); i++) {
                if (i != 0)
                    tempUrl.append("&");
                tempUrl.append(params.get(i).getKey()).append("=").append(params.get(i).getValue());
            }
            return new URL(tempUrl.toString());
        }
    }

    private void setHeaders() {
        if (httpURLConnection != null)
            for (int i = 0; i < headers.getHeaders().size(); i++) {
                if (headers.getHeaders().get(i) != null &&
                        headers.getHeaders().get(i).getKey() != null &&
                        headers.getHeaders().get(i).getValue() != null)
                    httpURLConnection.setRequestProperty(headers.getHeaders().get(i).getKey(),
                            headers.getHeaders().get(i).getValue());
            }
    }


    public static final class Builder {

        private @Nullable
        URL baseUrl;

        private HttpURLConnection httpURLConnection;

        private HillaRestBaseHeader headers;

        private HillaBaseGsonConverterFactory converterFactory;

        private int connectionTimeout;

        private int readTimeout;

        public Builder() {
            // httpURLConnection;
        }

        public Builder baseUrl(@Nullable String url) {

            try {
                this.baseUrl = new URL(url);
                httpURLConnection = (HttpURLConnection) baseUrl.openConnection();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this;
        }

        public Builder header(HillaRestBaseHeader headers) {

            this.headers = headers;
            return this;
        }

        public Builder connectionTimeout(int connectionTimeout) {

            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder readTimeout(int readTimeout) {

            this.readTimeout = readTimeout;
            return this;
        }

        public Builder addConverter(HillaBaseGsonConverterFactory converterFactory) {
            this.converterFactory = converterFactory;
            return this;
        }

        public HillaRestHttpConnection build() {
            return new HillaRestHttpConnection(this);
        }


    }

}
