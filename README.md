<h1 class="code-line" data-line-start=0 data-line-end=1 ><a id="HillaRest_0"></a>HillaRest</h1>
<p class="has-line-data" data-line-start="2" data-line-end="3"><a href="http://hillavas.com/"><img src="https://travis-ci.org/joemccann/dillinger.svg?branch=master" alt="Build Status"></a></p>
<p class="has-line-data" data-line-start="4" data-line-end="5">This library  best For basic libraries.</p>
<ul>
<li class="has-line-data" data-line-start="6" data-line-end="7">By HttpURLConnection, AsyncTask and Gson converter</li>
<li class="has-line-data" data-line-start="7" data-line-end="8">Post method</li>
<li class="has-line-data" data-line-start="8" data-line-end="9">Get method</li>
<li class="has-line-data" data-line-start="9" data-line-end="11">Small library</li>
</ul>
<h2 class="code-line" data-line-start=11 data-line-end=12 ><a id="How_to_use_11"></a>How to use</h2>
<h4 class="code-line" data-line-start=13 data-line-end=14 ><a id="1_Add_dependencies_13"></a>1. Add dependencies</h4>
<pre><code class="has-line-data" data-line-start="15" data-line-end="17" class="language-sh"> implementation <span class="hljs-string">'androidx.appcompat:appcompat:1.0.2'</span>
</code></pre>
<h4 class="code-line" data-line-start=18 data-line-end=19 ><a id="2_Add_api_class_18"></a>2. Add api class</h4>
<pre><code class="has-line-data" data-line-start="20" data-line-end="51" class="language-sh"> public class HillaSampleApi extends HillaRestBaseCallApi {


    public <span class="hljs-function"><span class="hljs-title">HillaSampleApi</span></span>() {
        super(new HillaRestHttpConnection.Builder()
                .header(new HillaSampleHeadersApi(<span class="hljs-string">"h1"</span>, <span class="hljs-string">"h2"</span>, <span class="hljs-string">"h3"</span>))
                .baseUrl(SampleRestConfig.getBaseUrl())
                .readTimeout(<span class="hljs-number">10000</span>)
                .connectionTimeout(<span class="hljs-number">15000</span>)
                .addConverter(HillaSampleGsonConverterFactory.create(
                        new HillaGsonBuilder().create()))
                .build());


    }

    public void getPostModel(
            HillaRestCallback&lt;PostSampleModel&gt; callback) {

        SampleBodyModel sampleBodyModel = new SampleBodyModel(<span class="hljs-string">"b1"</span>, <span class="hljs-string">"b2"</span>);
        List&lt;HillaRestParamModel&gt; paramModels = new ArrayList&lt;&gt;();
        paramModels.add(new HillaRestParamModel(<span class="hljs-string">"param1"</span>, <span class="hljs-string">"p1"</span>));
        paramModels.add(new HillaRestParamModel(<span class="hljs-string">"param2"</span>, <span class="hljs-string">"p2"</span>));

        post(SampleRestConfig.getPostUrl(), PostSampleModel.class, sampleBodyModel, paramModels, callback);

    }


}
</code></pre>
<h4 class="code-line" data-line-start=51 data-line-end=52 ><a id="3_Add_factoryconverter_class_51"></a>3. Add factoryconverter class</h4>
<pre><code class="has-line-data" data-line-start="53" data-line-end="91" class="language-sh">public final class HillaSampleGsonConverterFactory extends HillaBaseGsonConverterFactory {

    private static final Charset UTF_8 = Charset.forName(<span class="hljs-string">"UTF-8"</span>);

    public static HillaBaseGsonConverterFactory <span class="hljs-function"><span class="hljs-title">create</span></span>() {
        <span class="hljs-built_in">return</span> create(new HillaGson());
    }
    public static HillaBaseGsonConverterFactory create(HillaGson gson) {
        <span class="hljs-keyword">if</span> (gson == null) throw new NullPointerException(<span class="hljs-string">"gson == null"</span>);
        <span class="hljs-built_in">return</span> new HillaSampleGsonConverterFactory(gson);
    }

    private final HillaGson gson;

    private HillaSampleGsonConverterFactory(HillaGson gson) {
        this.gson = gson;
    }

    @Override
    public &lt;T&gt; void modelToJsonConverter(T model, OutputStream outputStream) throws IOException {

        final HillaJsonWriter writer = new HillaJsonWriter(new OutputStreamWriter(outputStream, UTF_8));
        gson.toJson(model, model.getClass(), writer);
        writer.flush();
        writer.close();

    }

    @Override
    public &lt;T&gt; T jsonToModelConverter(String json, Class&lt;T&gt; clazz) {
        <span class="hljs-built_in">return</span> gson.fromJson(json, clazz);

    }


}

</code></pre>
<h4 class="code-line" data-line-start=93 data-line-end=94 ><a id="4_Add_header_class_93"></a>4. Add header class</h4>
<pre><code class="has-line-data" data-line-start="95" data-line-end="132" class="language-sh">public class HillaSampleHeadersApi extends HillaRestBaseHeader {

    private final String header1;
    private final String header2;
    private final String header3;

    public HillaSampleHeadersApi(String header1, String header2, String header3) {
        this.header1 = header1;
        this.header2 = header2;
        this.header3 = header3;
    }


    @Override
    public List&lt;HillaRestHeaderModel&gt; <span class="hljs-function"><span class="hljs-title">getHeaders</span></span>() {
        List&lt;HillaRestHeaderModel&gt; headers = new ArrayList&lt;&gt;();


        headers.add(new HillaRestHeaderModel(
                <span class="hljs-string">"header1"</span>,
                header1
        ));
        headers.add(new HillaRestHeaderModel(
                <span class="hljs-string">"header2"</span>,
                header2
        ));
        headers.add(new HillaRestHeaderModel(
                <span class="hljs-string">"header3"</span>,
                header3
        ));


        <span class="hljs-built_in">return</span> headers;
    }
}

</code></pre>
<h4 class="code-line" data-line-start=133 data-line-end=134 ><a id="4_And_Use_in_project_133"></a>4. And Use in project</h4>
<pre><code class="has-line-data" data-line-start="136" data-line-end="155" class="language-sh">HillaSampleApi hillaSampleApi = new HillaSampleApi();

        hillaSampleApi.getPostModel(new HillaRestCallback&lt;PostSampleModel&gt;() {
            @Override
            public void onResponse(HillaRestResponse&lt;PostSampleModel&gt; response) {
                <span class="hljs-keyword">if</span> (response != null &amp;&amp; response.code() == <span class="hljs-number">200</span> &amp;&amp; response.body() != null)
                    postSampleModel = response.body();

            }

            @Override
            public void onFailure(Throwable t) {

                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();

            }
        });

</code></pre>


<h2><a id="user-content-license" class="anchor" aria-hidden="true" href="#license"><svg class="octicon octicon-link" viewBox="0 0 16 16" version="1.1" width="16" height="16" aria-hidden="true"><path fill-rule="evenodd" d="M4 9h1v1H4c-1.5 0-3-1.69-3-3.5S2.55 3 4 3h4c1.45 0 3 1.69 3 3.5 0 1.41-.91 2.72-2 3.25V8.59c.58-.45 1-1.27 1-2.09C10 5.22 8.98 4 8 4H4c-.98 0-2 1.22-2 2.5S3 9 4 9zm9-3h-1v1h1c1 0 2 1.22 2 2.5S13.98 12 13 12H9c-.98 0-2-1.22-2-2.5 0-.83.42-1.64 1-2.09V6.25c-1.09.53-2 1.84-2 3.25C6 11.31 7.55 13 9 13h4c1.45 0 3-1.69 3-3.5S14.5 6 13 6z"></path></svg></a>License</h2>
<pre><code>Hillarest library for Android

Copyright (c) 2018 Hilla vas (create by Mehran Jafari) (https://github.com/mehrtarh/Hillarest).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0


Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</code></pre>
</article>
  </body>
</html>

