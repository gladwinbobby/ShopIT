package in.codehex.shopit;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import in.codehex.shopit.model.FavoriteItem;
import in.codehex.shopit.util.Config;
import in.codehex.shopit.util.ItemClickListener;

public class UserFavoriteActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;
    List<FavoriteItem> favoriteItemList;
    RecyclerViewAdapter adapter;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_favorite);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences(Config.favorite, MODE_PRIVATE);

        favoriteItemList = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.favorite_list);
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        linearLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(linearLayoutManager);

        adapter = new RecyclerViewAdapter(getApplicationContext(), favoriteItemList);
        recyclerView.setAdapter(adapter);

        getFavoriteList();
    }

    private void getFavoriteList() {
        Map<String, ?> keys = sharedPreferences.getAll();

        for (Map.Entry<String, ?> entry : keys.entrySet()) {
            favoriteItemList.clear();
            favoriteItemList.add(new FavoriteItem(entry.getValue().toString()));
            adapter.notifyDataSetChanged();
        }

        if (favoriteItemList.isEmpty()) {
            Toast.makeText(getApplicationContext(),
                    "Add products to favorites", Toast.LENGTH_SHORT).show();
        }
    }

    private class RecyclerViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener {
        public TextView textProductName;
        private ItemClickListener itemClickListener;

        public RecyclerViewHolder(View view) {
            super(view);
            textProductName = (TextView) view.findViewById(R.id.product_name);
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

        private List<FavoriteItem> favoriteItemList;
        private Context context;

        public RecyclerViewAdapter(Context context, List<FavoriteItem> favoriteItemList) {
            this.favoriteItemList = favoriteItemList;
            this.context = context;
        }

        @Override
        public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int view) {
            View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_favorite, parent, false);
            RecyclerViewHolder holder = new RecyclerViewHolder(layoutView);
            return holder;
        }

        @Override
        public void onBindViewHolder(RecyclerViewHolder holder, int position) {
            final FavoriteItem favoriteItem = favoriteItemList.get(position);
            holder.textProductName.setText(favoriteItem.getProductName());

            holder.setClickListener(new ItemClickListener() {
                @Override
                public void onClick(View view, final int position, boolean isLongClick) {
                    FavoriteItem item = favoriteItemList.get(position);
                    final String productName = item.getProductName();

                    final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(UserFavoriteActivity.this);
                    alertDialogBuilder.setMessage("Do you want to remove this product?");

                    alertDialogBuilder.setPositiveButton("yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove(productName);
                            editor.apply();
                            Toast.makeText(getApplicationContext(),
                                    productName + " is removed from the favorites", Toast.LENGTH_SHORT).show();
                            favoriteItemList.remove(position);
                            adapter.notifyItemRemoved(position);
                            getFavoriteList();
                        }
                    });

                    alertDialogBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog alertDialog = alertDialogBuilder.create();
                    alertDialog.show();
                }
            });
        }

        @Override
        public int getItemCount() {
            return this.favoriteItemList.size();
        }
    }
}
