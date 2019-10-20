package ir.hilla.hillarestsample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import ir.hilla.rest.rest.HillaRestCallback;
import ir.hilla.rest.rest.HillaRestResponse;

public class MainActivity extends AppCompatActivity {

    private PostSampleModel postSampleModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        HillaSampleApi hillaSampleApi = new HillaSampleApi();

        hillaSampleApi.getPostModel(new HillaRestCallback<PostSampleModel>() {
            @Override
            public void onResponse(HillaRestResponse<PostSampleModel> response) {
                if (response != null && response.code() == 200 && response.body() != null)
                    postSampleModel = response.body();

            }

            @Override
            public void onFailure(Throwable t) {

                Toast.makeText(MainActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();

            }
        });


    }
}
