package tanuj.opengridmap.views.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.views.activities.CameraActivity;
import tanuj.opengridmap.views.adapters.PowerElementsGridAdapter;
import tanuj.opengridmap.models.PowerElement;

public class MainActivityFragment extends Fragment {
//    private PowerElement[] powerElements = {
//            new PowerElement("Transformer", 0, R.drawable.transformer),
//            new PowerElement("Substation", 1, R.drawable.substation),
//            new PowerElement("Generator", 2, R.drawable.power_station),
//            new PowerElement("PV or Wind Farm", 3, R.drawable.pv_wind),
//            new PowerElement("Other", 4, R.drawable.lightening_logo)
//    };

    List<PowerElement> powerElements = null;

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        final Context context = getActivity();

        OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());
        powerElements = dbHelper.getPowerElements();
        dbHelper.close();

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