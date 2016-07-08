package tanuj.opengridmap;

import android.graphics.drawable.Animatable;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.transition.Fade;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.models.PowerElement;

/**
 * A placeholder fragment containing a simple view.
 */
public class PowerElementDetailActivityFragment extends Fragment {
    private ImageView imageView;
    private TextView title;
    private LinearLayout titleHolder;
    private TextView descriptionTextView;
    private PowerElement powerElement;
    int defaultColorForRipple;

    public PowerElementDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_power_element_detail, container, false);

        powerElement = PowerElementsSeedData.powerElements.
                get(getActivity().getIntent().getIntExtra(
                        PowerElementDetailActivity.EXTRA_PARAM_ID, 0));

        imageView = (ImageView) view.findViewById(R.id.powerElementImage);
        title = (TextView) view.findViewById(R.id.textView);
        titleHolder = (LinearLayout) view.findViewById(R.id.powerElementNameHolder);
        descriptionTextView = (TextView) view.findViewById(R.id.description_text_view);

        if (powerElement != null) {
            title.setText(powerElement.getName());
            descriptionTextView.setText(powerElement.getDeviceDescription(getActivity()));

            Picasso.with(getActivity()).
                    load(powerElement.getImageId()).
                    into(imageView);

            Transition fade = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                fade = new Fade();
                fade.excludeTarget(android.R.id.navigationBarBackground, true);
                fade.excludeTarget(android.R.id.statusBarBackground, true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                titleHolder.setBackground(new ColorDrawable(getResources().getColor(R.color.teal_400)));
            }
        }

        return view;
    }
}
