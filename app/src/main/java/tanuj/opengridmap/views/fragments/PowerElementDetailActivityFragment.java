package tanuj.opengridmap.views.fragments;

import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.transition.Fade;
import android.transition.Transition;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.PowerElementsSeedData;
import tanuj.opengridmap.models.PowerElement;
import tanuj.opengridmap.views.activities.PowerElementDetailActivity;

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
