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

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.views.activities.CameraActivity;
import tanuj.opengridmap.views.adapters.PowerElementsGridAdapter;

public class MainActivityFragment extends Fragment {
    public static final String TAG = MainActivityFragment.class.getSimpleName();

    public MainActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Context context = getActivity();

        GridView gridView = (GridView) rootView.findViewById(R.id.main_gridview);
        gridView.setAdapter(new PowerElementsGridAdapter(context,
                PowerElementsSeedData.powerElements));
        gridView.setOnItemClickListener(onItemClickListener);

        return rootView;
    }

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            final Context context = getActivity();
            Intent intent = new Intent(context, CameraActivity.class);

            intent.putExtra(getString(R.string.key_power_element_id),
                    PowerElementsSeedData.powerElements.get(position).getId());
            startActivity(intent);
        }
    };
}