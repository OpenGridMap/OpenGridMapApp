package tanuj.opengridmap.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import java.util.Arrays;
import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.PowerElement;
import tanuj.opengridmap.views.activities.CameraActivity;
import tanuj.opengridmap.views.adapters.PowerElementsGridAdapter;

public class MainActivityFragment extends Fragment {
    public static final String TAG = MainActivityFragment.class.getSimpleName();

    private static final List<PowerElement> powerElements = Arrays.asList(
            new PowerElement(0, "Transformer", R.drawable.transformer),
            new PowerElement(1, "Substation", R.drawable.substation),
            new PowerElement(2, "Generator", R.drawable.power_station),
            new PowerElement(3, "PV or Wind Farm", R.drawable.pv_wind),
            new PowerElement(4, "Other", R.drawable.lightening_logo));


    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Context context = getActivity();

//        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);
//        powerElements = dbHelper.getPowerElements();
//        dbHelper.close();

        GridView gridView = (GridView) rootView.findViewById(R.id.main_gridview);
        gridView.setAdapter(new PowerElementsGridAdapter(context, powerElements));
        gridView.setOnItemClickListener(onItemClickListener);

        return rootView;
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Context context = getActivity();
            Intent intent = new Intent(context, CameraActivity.class);

            intent.putExtra(getString(R.string.key_power_element_id), powerElements
                    .get(position).getId());
            startActivity(intent);
        }
    };
}