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
            new PowerElement(1, "Transformer", R.drawable.transformer, "power=transformer", R.string.desc_transformer),
            new PowerElement(2, "Substation", R.drawable.substation, "power=substation", R.string.desc_substation),
            new PowerElement(3, "Generator", R.drawable.power_station, "power=plant", R.string.desc_generator),
            new PowerElement(4, "Wind Farm", R.drawable.wind_farm, "power=generator;generator:method=wind_turbine;generator:source=wind", R.string.desc_wind_farm),
            new PowerElement(5, "Solar PV", R.drawable.solar_panels, "power=generator;generator:method=photovoltaic;generator:source=solar", R.string.desc_solar_panels),
            new PowerElement(6, "Other", R.drawable.lightening_logo, "other")
    );
}