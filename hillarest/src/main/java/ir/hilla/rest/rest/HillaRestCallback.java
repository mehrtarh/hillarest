package ir.hilla.rest.rest;

public interface HillaRestCallback<T> {
    /**
     * Invoked for a received HTTP response.
     * <p>
     * Note: An HTTP response may still indicate an application-level failure such as a 404 or 500.
     * HillaRestCall {@link HillaRestResponse#isSuccessful()} to determine if the response indicates success.
     */
    void onResponse(HillaRestResponse<T> response);

    /**
     * Invoked when a network exception occurred talking to the server or when an unexpected
     * exception occurred creating the request or processing the response.
     */
    void onFailure(Throwable t);
}
