package tanuj.opengridmap.views.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.models.PowerElement;

/**
 * Created by Tanuj on 10/6/2016.
 */
public class PowerElementsRecyclerViewAdapter extends
        RecyclerView.Adapter<PowerElementsRecyclerViewAdapter.ViewHolder> {
    private Context context;
    private OnItemClickListener itemClickListener;

    public PowerElementsRecyclerViewAdapter(Context context) {
        this.context = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.grid_item, parent,
                false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PowerElement powerElement = PowerElementsSeedData.powerElements.get(position);

        holder.powerElementName.setText(powerElement.getName());
        Picasso.with(context).load(powerElement.getImageId()).into(holder.powerElementImage);
//        Bitmap photo = BitmapFactory.decodeResource(context.getResources(),
//                powerElement.getImageId());

//        Palette.generateAsync(photo, new Palette.PaletteAsyncListener() {
//            @Override
//            public void onGenerated(Palette palette) {
//                int mutedLight = palette.getLightMutedColor(
//                        context.getResources().getColor(R.color.black));
//                holder.powerElementNameHolder.setBackgroundColor(mutedLight);
//            }
//        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            holder.powerElementNameHolder.setBackgroundColor(context.getColor(R.color.teal_400));
        } else {
            holder.powerElementNameHolder.setBackgroundColor(context.getResources().
                    getColor(R.color.teal_400));
        }


        if (powerElement.getId() == 6) {
            holder.infoButton.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return PowerElementsSeedData.powerElements.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public LinearLayout powerElementHolder;
        public LinearLayout powerElementNameHolder;
        public TextView powerElementName;
        public ImageView powerElementImage;
        public ImageButton infoButton;

        public ViewHolder(View itemView) {
            super(itemView);

            powerElementHolder = (LinearLayout) itemView.findViewById(R.id.mainHolder);
            powerElementName = (TextView) itemView.findViewById(R.id.powerElementName);
            powerElementNameHolder = (LinearLayout) itemView.findViewById(
                    R.id.powerElementNameHolder);
            powerElementImage = (ImageView) itemView.findViewById(R.id.powerElementImage);
            infoButton = (ImageButton) itemView.findViewById(R.id.btn_info);

            powerElementHolder.setOnClickListener(this);
            infoButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (itemClickListener != null) {
                if (v.getClass().getSimpleName().equals(LinearLayout.class.getSimpleName())) {
                    itemClickListener.onItemClick(itemView, getAdapterPosition());
                } else {
                    itemClickListener.onInfoClick(itemView, getAdapterPosition());
                }
            }
        }
    }

    public interface OnItemClickListener {
//        void onItemClick(View view, View itemView, int position);
        void onItemClick(View view, int position);
        void onInfoClick(View view, int position);
    }

    public void setOnItemClickListener(final OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }
}
