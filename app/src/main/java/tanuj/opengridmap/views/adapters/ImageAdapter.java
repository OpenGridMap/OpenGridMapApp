package tanuj.opengridmap.views.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;

/**
 * Created by Tanuj on 26/6/2015.
 */
public class ImageAdapter extends BaseAdapter {

    private Context context = null;
    private List<Image> images = null;

    private static LayoutInflater layoutInflater = null;

    public ImageAdapter(Context context, List<Image> images) {
        this.context = context;
        this.images = images;
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return images.size();
    }

    @Override
    public Image getItem(int position) {
        return images.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @SuppressLint("NewApi")
    @Override
    public View getView(int position, View view, ViewGroup parent) {
        ImageView imageView;

        if (view == null) {
            int size = ((GridView) parent).getColumnWidth();

            imageView = new ImageView(context);
//            imageView.setLayoutParams(new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setLayoutParams(new GridView.LayoutParams(size, size));

            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
        } else {
            imageView = (ImageView) view;
        }

//        Bitmap bitmap = getItem(position).getGridThumbnailBitmap();
        Bitmap bitmap = getItem(position).getThumbnailBitmap("grid", context);

        if(bitmap != null){
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.camera_shutter);
        }

//        imageView.setImageResource(i);

        return imageView;
    }
}
