package tanuj.opengridmap.views.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import tanuj.opengridmap.R;

/**
 * Created by Tanuj on 20/10/2015.
 */
public class AboutActivityFragment extends Fragment{

    public AboutActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_about, container, false);

        TextView aboutTextView = rootView.findViewById(R.id.text_view_about);
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        return rootView;
    }
}
