package ir.hilla.rest.rest;

public class HillaRestRequest {

    private final String url;

    private HillaRestRequest(Builder builder) {
        this.url = builder.url;
    }

    public String url()
    {
        return this.url;
    }


    public static class Builder {
        private String url;

        public Builder url(String url) {

            this.url = url;
            return this;
        }

        public HillaRestRequest build()
        {
            return new HillaRestRequest(this);
        }

    }


}
