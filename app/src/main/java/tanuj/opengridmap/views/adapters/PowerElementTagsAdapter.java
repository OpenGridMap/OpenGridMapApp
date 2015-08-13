package tanuj.opengridmap.views.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.PowerElement;

/**
 * Created by Tanuj on 8/7/2015.
 */
public class PowerElementTagsAdapter extends BaseAdapter {
    public static final int NOT_TAGGED = 0;

    public static final int TAGGED = 1;

    private ArrayList<PowerElement> powerElements;

    private Context context = null;

    private LayoutInflater layoutInflater = null;

    private int tagged;

    public PowerElementTagsAdapter(Context context, ArrayList<PowerElement> powerElements) {
        this.powerElements = powerElements;
        this.context = context;
    }

    public PowerElementTagsAdapter(Context context, ArrayList<PowerElement> powerElements, int tagged) {
        this(context, powerElements);
        this.tagged = tagged;
    }

    public ArrayList<PowerElement> getPowerElements() {
        return powerElements;
    }

    @Override
    public int getCount() {
        return powerElements.size();
    }

    @Override
    public PowerElement getItem(int position) {
        return powerElements.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
//        return powerElements.get(position).getId();
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        layoutInflater = ((Activity) context).getLayoutInflater();

        if (view == null) {
            view = layoutInflater.inflate(R.layout.power_element_tags_item, parent, false);
        }
        LinearLayout linearLayout = (LinearLayout) view.findViewById(R.id.container);

        if (tagged == TAGGED) {
            linearLayout.setBackgroundResource(R.color.teal_300);
        } else if (tagged == NOT_TAGGED) {
            linearLayout.setBackgroundResource(R.color.amber_a400);
        }

        TextView textView = (TextView) view.findViewById(R.id.power_element_tag);

        textView.setText(getItem(position).getName());

        return view;
    }

    public PowerElement removeTag(int position) {
        PowerElement p = powerElements.remove(position);
        notifyDataSetChanged();
        return p;
    }

    public void addTag(PowerElement powerElement) {
        powerElements.add(powerElement);
        notifyDataSetChanged();
    }
}
