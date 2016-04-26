package in.codehex.shopit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.codehex.shopit.model.ReviewItem;
import in.codehex.shopit.util.AppController;
import in.codehex.shopit.util.Config;

public class ReviewActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView textProductName, textProductPrice;
    RatingBar ratingBar;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    ProgressDialog progressDialog;
    List<ReviewItem> reviewItemList;
    Intent intent;
    RecyclerViewAdapter adapter;
    SharedPreferences sharedPreferences, favorite;
    String productName, productPrice;
    int productId;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(Config.pref, MODE_PRIVATE);
        favorite = getSharedPreferences(Config.favorite, MODE_PRIVATE);

        productName = sharedPreferences.getString("productName", null);
        productPrice = "\u20B9 " + sharedPreferences.getString("productPrice", null);
        productId = sharedPreferences.getInt("productId", 0);

        textProductName = (TextView) findViewById(R.id.product_name);
        textProductName.setText(productName);

        textProductPrice = (TextView) findViewById(R.id.product_price);
        textProductPrice.setText(productPrice);

        ratingBar = (RatingBar) findViewById(R.id.rating);
        ratingBar.setRating(1.0f);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                intent = new Intent(getApplicationContext(), FeedbackActivity.class);
                startActivity(intent);
            }
        });

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading reviews..");
        progressDialog.setCancelable(false);

        reviewItemList = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.review_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter(getApplicationContext(), reviewItemList);
        recyclerView.setAdapter(adapter);

        showProgressDialog();
        getReviewList();
    }

    private void getReviewList() {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                // parsing json response data
                int ratingCount = 0;
                double totalRating = 0.0;
                try {
                    JSONArray array = new JSONArray(response);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);

                        double rating = object.getDouble("rating");
                        String feedback = object.getString("feedback");

                        totalRating += rating;
                        ratingCount++;

                        reviewItemList.add(new ReviewItem(rating, feedback));
                        adapter.notifyDataSetChanged();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                float rating = (float) (totalRating / ratingCount);
                ratingBar.setRating(rating);
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
                params.put("tag", "review_list");
                params.put("product_id", String.valueOf(productId));

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_review, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_favorite) {
            SharedPreferences.Editor editor = favorite.edit();
            editor.putString(productName, productName);
            editor.apply();
            Toast.makeText(getApplicationContext(),
                    "Added to favorites", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class RecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView textFeedback;

        public RecyclerViewHolder(View view) {
            super(view);
            textFeedback = (TextView) view.findViewById(R.id.feedback);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

        private List<ReviewItem> reviewItemList;
        private Context context;

        public RecyclerViewAdapter(Context context, List<ReviewItem> reviewItemList) {
            this.reviewItemList = reviewItemList;
            this.context = context;
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_review, parent, false);
            RecyclerViewHolder holder = new RecyclerViewHolder(layoutView);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position) {
            final ReviewItem reviewItem = reviewItemList.get(position);
            holder.textFeedback.setText(reviewItem.getFeedback());

        }

        @Override
        public int getItemCount() {
            return this.reviewItemList.size();
        }
    }
}
