package tanuj.opengridmap.views.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class AboutActivityFragment extends Fragment{

    public AboutActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        return rootView;
    }
}
