package tanuj.opengridmap.data;

import java.util.Arrays;
import java.util.List;

import tanuj.opengridmap.R;
import tanuj.opengridmap.models.PowerElement;

/**
 * Created by Tanuj on 21/10/2015.
 */
public class PowerElementsSeedData {
    public static final List<PowerElement> powerElements = Arrays.asList(
            new PowerElement(1, "Transformer", R.drawable.transformer),
            new PowerElement(2, "Substation", R.drawable.substation),
            new PowerElement(3, "Generator", R.drawable.power_station),
            new PowerElement(4, "PV or Wind Farm", R.drawable.pv_wind),
            new PowerElement(5, "Other", R.drawable.lightening_logo));
}
