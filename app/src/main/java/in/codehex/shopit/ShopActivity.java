package in.codehex.shopit;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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

import in.codehex.shopit.model.ShopItem;
import in.codehex.shopit.util.AppController;
import in.codehex.shopit.util.Config;
import in.codehex.shopit.util.ItemClickListener;

public class ShopActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    ProgressDialog progressDialog;
    List<ShopItem> shopItemList;
    Intent intent;
    RecyclerViewAdapter adapter;
    String searchTag;
    float shopDistance;
    double lat, lng;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(Config.pref, MODE_PRIVATE);

        searchTag = sharedPreferences.getString("searchTag", null);
        lat = Double.parseDouble(sharedPreferences.getString("lat", "12"));
        lng = Double.parseDouble(sharedPreferences.getString("lng", "80"));
        shopDistance = sharedPreferences.getFloat("distance", 1000);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading shops..");
        progressDialog.setCancelable(false);

        shopItemList = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.shop_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter(getApplicationContext(), shopItemList);
        recyclerView.setAdapter(adapter);

        showProgressDialog();
        getShopList();
    }

    private void getShopList() {
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

                        int shopId = object.getInt("shop_id");
                        String shopName = object.getString("shop_name");
                        double latitude = object.getDouble("latitude");
                        double longitude = object.getDouble("longitude");

                        processDistance(latitude, longitude, shopId, shopName);
                    }

                    if (shopItemList.isEmpty()) {
                        Toast.makeText(getApplicationContext(),
                                "Product is not available in any nearby shops", Toast.LENGTH_SHORT).show();
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
                        "Network error! Check your internet connection!",
                        Toast.LENGTH_SHORT).show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to the register url
                Map<String, String> params = new HashMap<String, String>();
                params.put("tag", "shop_list");
                params.put("search_tag", searchTag);

                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq);
    }

    private void processDistance(double latitude, double longitude,
                                 final int shopId, final String shopName) {
        Location source = new Location("source");
        source.setLatitude(lat);
        source.setLongitude(lng);

        Location destination = new Location("destination");
        destination.setLatitude(latitude);
        destination.setLongitude(longitude);

        float distance = source.distanceTo(destination);

        if (distance <= shopDistance) {
            shopItemList.add(new ShopItem(shopId, shopName, latitude, longitude));
            adapter.notifyDataSetChanged();
        }
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
        public TextView textShopName;
        public ImageView imgMap;
        private ItemClickListener itemClickListener;

        public RecyclerViewHolder(View view) {
            super(view);
            textShopName = (TextView) view.findViewById(R.id.shop_name);
            imgMap = (ImageView) view.findViewById(R.id.map);
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

        private List<ShopItem> shopItemList;
        private Context context;

        public RecyclerViewAdapter(Context context, List<ShopItem> shopItemList) {
            this.shopItemList = shopItemList;
            this.context = context;
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_shop, parent, false);
            RecyclerViewHolder holder = new RecyclerViewHolder(layoutView);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position) {
            final ShopItem shopItem = shopItemList.get(position);
            holder.textShopName.setText(shopItem.getShopName());

            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, int position, boolean isLongClick) {
                    ShopItem item = shopItemList.get(position);
                    int shopId = item.getShopId();

                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("shopId", shopId);
                    editor.apply();

                    intent = new Intent(getApplicationContext(), ProductActivity.class);
                    startActivity(intent);
                }
            });

            holder.imgMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String uri = "geo:" + shopItem.getLat() + "," + shopItem.getLng();
                    Uri gmmIntentUri = Uri.parse(uri);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null)
                        startActivity(mapIntent);
                    else Toast.makeText(getApplicationContext(),
                            "Please install Google Maps", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.shopItemList.size();
        }
    }
}
