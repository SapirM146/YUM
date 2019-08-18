package com.itaisapir.yum.Logic;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.itaisapir.yum.R;
import com.itaisapir.yum.utils.InnerIds;

import java.util.List;

public class CategoryRecipeAdapter extends RecyclerView.Adapter<CategoryRecipeAdapter.MyViewHolder> {

    private List<CategoryOrRecipe> dataset;
    private OnItemClicked onClick;
    private OnItemClicked onLongClick;

    private int lastClickedPosition;
    private Context context;

    public interface OnItemClicked {
        void onItemClick(int position, View v);
        void onItemLongClick(int position, View v);
    }

    public CategoryRecipeAdapter(List<CategoryOrRecipe>  dataset) {
        this.dataset = dataset;
        lastClickedPosition = -1;
    }

    @Override
    public CategoryRecipeAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,
                                                                 int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View contactView = inflater.inflate(R.layout.item_of_categories_and_reciepies_view, parent, false);

        MyViewHolder vh = new MyViewHolder(contactView);
        return vh;
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, final int position) {
        holder.mTextView.setText(dataset.get(position).getName());

        holder.mTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onClick.onItemClick(position, v);
            }
        });

        holder.mTextView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onLongClick.onItemLongClick(position, v);
                return true;
            }
        });

        if(dataset.get(position) instanceof Recipe)
            holder.image.setImageResource(R.drawable.recipe9);
        else if(dataset.get(position).getName().equals(context.getResources().getString(R.string.world_category)))
            holder.image.setImageResource(R.drawable.world_icon);
        else
            holder.image.setImageResource(R.drawable.category3);

        String contextClassName = context.getClass().getSimpleName();
        if(InnerIds.CATEGORY_ACTIVITY.equals(contextClassName) || InnerIds.SUBCATEGORY_ACTIVITY.equals(contextClassName)){
            if(dataset.get(position) instanceof Recipe)
                holder.mTextView.setBackgroundColor(context.getResources().getColor(R.color.categoryRecyclerRecipeBackground));
            else
                holder.mTextView.setBackgroundColor(context.getResources().getColor(R.color.categoryRecyclerCategoryBackground));

        } else if (InnerIds.WORLD_CATEGORY_SCREEN.equals(contextClassName)) {
            if (lastClickedPosition == position) {
                holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.worldCategoryRecyclerClick));
                holder.mTextView.setBackgroundColor(ContextCompat.getColor(context, R.color.worldCategoryRecyclerClick));
            } else {
                if(dataset.get(position) instanceof Recipe) {
                    holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.worldCategoryRecipeBackground));
                    holder.mTextView.setBackgroundColor(context.getResources().getColor(R.color.worldCategoryRecipeBackground));
                } else {
                    holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.worldCategoryCategoryBackground));
                    holder.mTextView.setBackgroundColor(context.getResources().getColor(R.color.worldCategoryCategoryBackground));
                }
            }
        }
    }

    public void setLastClickedPosition(int position){
        lastClickedPosition = position;
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ImageView image;

        public MyViewHolder(View parent) {
            super(parent);
            mTextView = parent.findViewById(R.id.list_view_item_text);
            image = parent.findViewById(R.id.image);
        }
    }

    public void setOnClick(OnItemClicked onClick)
    {
        this.onClick=onClick;
    }

    public void setOnLongClick(OnItemClicked onLongClick)
    {
        this.onLongClick=onLongClick;
    }

}
