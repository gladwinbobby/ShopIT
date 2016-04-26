package in.codehex.shopit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import in.codehex.shopit.model.ProductItem;
import in.codehex.shopit.util.AppController;
import in.codehex.shopit.util.Config;
import in.codehex.shopit.util.ItemClickListener;

public class ProductActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    ProgressDialog progressDialog;
    List<ProductItem> productItemList;
    Intent intent;
    RecyclerViewAdapter adapter;
    String searchTag;
    int shopId;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(Config.pref, MODE_PRIVATE);

        searchTag = sharedPreferences.getString("searchTag", null);
        shopId = sharedPreferences.getInt("shopId", 0);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading products..");
        progressDialog.setCancelable(false);

        productItemList = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.product_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter(getApplicationContext(), productItemList);
        recyclerView.setAdapter(adapter);

        showProgressDialog();
        getProductList();
    }

    private void getProductList() {
        // volley string request to server with POST parameters
        StringRequest strReq = new StringRequest(Request.Method.POST,
                Config.url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                hideProgressDialog();
                // parsing json response data
                try {
                    JSONArray array = new JSONArray(response);

                    for (int i = 0; i < array.length(); i++) {
                        JSONObject object = array.getJSONObject(i);

                        int productId = object.getInt("product_id");
                        String price = object.getString("price");
                        String productName = object.getString("product_name");

                        productItemList.add(new ProductItem(productId, price, productName));
                        adapter.notifyDataSetChanged();
                    }

                    if (productItemList.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "No product is available", Toast.LENGTH_SHORT).show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                hideProgressDialog();
                Toast.makeText(getApplicationContext(),
                        "Network error! Check your internet connection!"
                        , Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "product_list");
                params.put("search_tag", searchTag);
                params.put("shop_id", String.valueOf(shopId));

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

    private class RecyclerViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {
        public TextView textProductName, textProductPrice;
        private ItemClickListener itemClickListener;

        public RecyclerViewHolder(View view) {
            super(view);
            textProductName = (TextView) view.findViewById(R.id.product_name);
            textProductPrice = (TextView) view.findViewById(R.id.product_price);
            view.setOnClickListener(this);
        }

        public void setClickListener(ItemClickListener itemClickListener) {
            this.itemClickListener = itemClickListener;
        }

        @Override
        public void onClick(View view) {
            itemClickListener.onClick(view, getAdapterPosition(), false);
        }
    }

    private class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {

        private List<ProductItem> productItemList;
        private Context context;

        public RecyclerViewAdapter(Context context, List<ProductItem> productItemList) {
            this.productItemList = productItemList;
            this.context = context;
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_product, parent, false);
            RecyclerViewHolder holder = new RecyclerViewHolder(layoutView);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position) {
            final ProductItem productItem = productItemList.get(position);
            String productPrice = "\u20B9 " + productItem.getPrice();
            holder.textProductName.setText(productItem.getProductName());
            holder.textProductPrice.setText(productPrice);

            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    ProductItem item = productItemList.get(position);
                    int productId = item.getProductId();
                    String productName = item.getProductName();
                    String productPrice = item.getPrice();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("productName", productName);
                    editor.putString("productPrice", productPrice);
                    editor.putInt("productId", productId);
                    editor.apply();

                    intent = new Intent(getApplicationContext(), ReviewActivity.class);
                    startActivity(intent);
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.productItemList.size();
        }
    }
}
