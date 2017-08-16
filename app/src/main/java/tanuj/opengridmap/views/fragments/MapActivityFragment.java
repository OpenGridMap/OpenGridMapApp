package tanuj.opengridmap.views.fragments;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import tanuj.opengridmap.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapActivityFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapActivityFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapActivityFragment extends Fragment {
    private static final String TAG = MapActivityFragment.class.getSimpleName();

    private static final String url = "http://vmjacobsen39.informatik.tu-muenchen.de/";

    public MapActivityFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map_activity, container, false);

        WebView mapWebView = view.findViewById(R.id.map_web_view);

        mapWebView.getSettings().setJavaScriptEnabled(true);
        mapWebView.getSettings().setSupportZoom(true);
        mapWebView.getSettings().setBuiltInZoomControls(true);
        mapWebView.loadUrl(url);

        return view;
    }

}
