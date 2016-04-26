package in.codehex.shopit;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import in.codehex.shopit.util.AppController;
import in.codehex.shopit.util.Config;

public class FeedbackActivity extends AppCompatActivity {

    Toolbar toolbar;
    RatingBar ratingBar;
    EditText inputFeedback;
    float userRating;
    String feedback;
    Button submit;
    ProgressDialog progressDialog;
    int productId;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(Config.pref, MODE_PRIVATE);

        productId = sharedPreferences.getInt("productId", 0);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Submitting feedback..");
        progressDialog.setCancelable(false);

        ratingBar = (RatingBar) findViewById(R.id.rating);
        ratingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener() {
            @Override
            public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
                userRating = rating;
            }
        });

        inputFeedback = (EditText) findViewById(R.id.feedback);

        submit = (Button) findViewById(R.id.submit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                feedback = inputFeedback.getText().toString();
                showProgressDialog();
                submitFeedback();
            }
        });
    }

    private void submitFeedback() {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                // parsing json response data
                try {
                    JSONObject jObj = new JSONObject(response);
                    int error = jObj.getInt("error");
                    String message = jObj.getString("message");

                    Toast.makeText(getApplicationContext(),
                            message, Toast.LENGTH_SHORT).show();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(),
                        "Network error! Check your internet connection!",
                        Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "feedback");
                params.put("product_id", String.valueOf(productId));
                params.put("rating", String.valueOf(userRating));
                params.put("feedback", feedback);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq);
    }

    private void showProgressDialog() {
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void hideProgressDialog() {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
