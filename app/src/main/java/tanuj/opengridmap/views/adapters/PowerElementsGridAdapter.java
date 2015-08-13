package tanuj.opengridmap.views.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.PowerElement;

/**
 * Created by tanuj on 08.05.15.
 */
public class PowerElementsGridAdapter extends BaseAdapter {
    private Context context = null;
    private List<PowerElement> powerElements;
    private static LayoutInflater layoutInflater = null;

    public PowerElementsGridAdapter(Context context, List<PowerElement> powerElements) {
        this.context = context;
        this.powerElements = powerElements;
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return powerElements.size();
    }

    @Override
    public Object getItem(int i) {
        return powerElements.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        View currentView;
        layoutInflater = ((Activity) context).getLayoutInflater();

        if (view == null) {
            view = layoutInflater.inflate(R.layout.power_element_grid_item, viewGroup, false);
        }

        ImageView imageView = (ImageView) view.findViewById(R.id.power_element_image);
        TextView textView = (TextView) view.findViewById(R.id.power_element_text);

        PowerElement powerElement = (PowerElement) getItem(i);

//        imageView.setBackgroundResource(powerElement.getImageId());
        imageView.setImageResource(powerElement.getImageId());
        textView.setText(powerElement.getName());

        return view;
//        currentView = (View) view;

//        return currentView;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }
}