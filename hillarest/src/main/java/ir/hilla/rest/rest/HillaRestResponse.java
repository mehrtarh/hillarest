package ir.hilla.rest.rest;


import java.util.List;

import ir.hilla.rest.rest.base.HillaRestHeaderModel;


public final class HillaRestResponse<T> {

    private final int code;
    private final String message;
    private final T body;
    private final String protocol;
    private final HillaRestRequest request;
    private final List headers;

    private HillaRestResponse(Builder<T> builder) {

        this.code = builder.code;
        this.message = builder.message;
        this.protocol = builder.protocol;
        this.request = builder.request;
        this.body = builder.body;
        this.headers = builder.headers;


    }

    private HillaRestResponse(HillaRestResponse response, T body) {

        this.code = response.code;
        this.message = response.message;
        this.protocol = response.protocol;
        this.request = response.request;
        this.body = body;
        this.headers = response.headers;


    }

    /**
     * Create a synthetic successful response with {@code body} as the deserialized body.
     */
    public static <T> HillaRestResponse<T> success(@Nullable T body) {


        return success(body, new Builder<T>()
                .code(200)
                .message("OK")
                .protocol("http/1.1")
                .request(new HillaRestRequest.Builder().url("http://localhost/").build())
                .build());

    }

    /**
     * Create a synthetic successful response with an HTTP status code of {@code code} and
     * {@code body} as the deserialized body.
     */
    public static <T> HillaRestResponse<T> success(int code, String url, @Nullable T body) {
//        if (code < 200 || code >= 300) {
//            throw new IllegalArgumentException("code < 200 or >= 300: " + code);
//        }

        return success(body, new Builder<T>()
                .body(body)
                .code(code)
                .message("HillaRestResponse.success()")
                .protocol("http/1.1")
                .request(new HillaRestRequest.Builder().url(url).build())
                .build());
    }

    /**
     * Create a synthetic successful response using {@code headers} with {@code body} as the
     * deserialized body.
     */
    public static <T> HillaRestResponse<T> success(@Nullable T body, List<HillaRestHeaderModel> headers) {

        if (headers == null)
            throw new NullPointerException("rawResponse == null");

        return success(body, new Builder<T>()
                .code(200)
                .message("OK")
                .protocol("http/1.1")
                .headers(headers)
                .request(new HillaRestRequest.Builder().url("http://localhost/").build())
                .build());
    }

    /**
     * Create a successful response from {@code rawResponse} with {@code body} as the deserialized
     * body.
     */
    public static <T> HillaRestResponse<T> success(@Nullable T body, HillaRestResponse response) {
//        if (response == null)
//            throw new NullPointerException("rawResponse == null");
//        if (!response.isSuccessful()) {
//            throw new IllegalArgumentException("response must be successful response");
//        }
        return new HillaRestResponse<>(response, body);
    }


    /**
     * Create a error response from {@code rawResponse} with {@code body} as the deserialized
     * body.
     */
    public static <T> HillaRestResponse<T> error(@Nullable T body, HillaRestResponse response) {
//        if (response == null)
//            throw new NullPointerException("rawResponse == null");
//        if (response.isSuccessful()) {
//            throw new IllegalArgumentException("response must be error response");
//        }
        return new HillaRestResponse<>(response, body);
    }

    /**
     * Create a synthetic error response with an HTTP status code of {@code code} and {@code body}
     * as the error body.
     */
    public static <T> HillaRestResponse<T> error(int code, String message, String url, T body) {

//        if (body == null )
//            throw new NullPointerException("body == null");
//
//        if (code < 400)
//            throw new IllegalArgumentException("code < 400: " + code);
        String responseMessage;
        if (message == null)
            responseMessage = "HillaRestResponse.error()";
        else
            responseMessage = message;

        return error(body, new Builder<T>()
                .body(body)
                .code(code)
                .message(responseMessage)
                .protocol("http/1.1")
                .request(new HillaRestRequest.Builder().url(url).build())
                .build());


    }

    /**
     * HTTP status code.
     */
    public int code() {
        return this.code;
    }

    /**
     * HTTP status message or null if unknown.
     */
    public String message() {
        return this.message;
    }


    /**
     * Returns true if {@link #code()} is in the range [200..300).
     */
    public boolean isSuccessful() {
        return true;
    }

    /**
     * The deserialized response body of a {@linkplain #isSuccessful() successful} response.
     */
    public @Nullable
    T body() {
        return body;
    }


    public static class Builder<K> {
        private int code;
        private String message;
        private String protocol;
        private K body;
        private HillaRestRequest request;
        private List<HillaRestHeaderModel> headers;


        public Builder code(int code) {
            this.code = code;
            return this;
        }

        public Builder body(K body) {
            this.body = body;
            return this;
        }

        public Builder request(HillaRestRequest request) {
            this.request = request;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder protocol(String protocol) {
            this.protocol = protocol;
            return this;
        }


        public Builder headers(List<HillaRestHeaderModel> headers) {
            this.headers = headers;
            return this;
        }


        public HillaRestResponse<K> build() {
            return new HillaRestResponse<K>(this);
        }


    }
}