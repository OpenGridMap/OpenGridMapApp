package tanuj.opengridmap.views.activities;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Locale;

import tanuj.opengridmap.R;
import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.Image;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.adapters.ImageAdapter;
import tanuj.opengridmap.views.adapters.PowerElementTagsAdapter;


public class SubmissionActivity extends AppCompatActivity implements ActionBar.TabListener {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submission);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_submission, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a SubmissionFragment (defined as a static inner class below).
            return SubmissionFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1);
                case 1:
                    return getString(R.string.title_section2);
                case 2:
                    return getString(R.string.title_section3);
                case 3:
                    return getString(R.string.title_section4);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class SubmissionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private Submission submission;

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static SubmissionFragment newInstance(int sectionNumber) {
            SubmissionFragment fragment = new SubmissionFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public SubmissionFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = null;
            int tabIndex = getArguments().getInt(ARG_SECTION_NUMBER);

            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());

            long submissionId = getActivity().getIntent().getLongExtra(getString(
                    R.string.key_submission_id), -1);

            if (submissionId > -1) {
                submission = dbHelper.getSubmission(submissionId);
            }

            dbHelper.close();

            switch (tabIndex) {
                case 1: {
                    final Context context = getActivity();
                    view = inflater.inflate(R.layout.fragment_submission_tab_1, container, false);

                    Image bestImage = submission.getBestImageByLocationAccuracy();
                    Image worstImage = submission.getBestImageByLocationAccuracy();
                    String[] coordinates = bestImage.getLocationInDegrees(context);

                    TextView submissionStatusTextView = (TextView) view.findViewById(
                            R.id.submission_status);
                    TextView noImagesTextView = (TextView) view.findViewById(R.id.
                            submission_no_of_images);
                    TextView submissionLatitudeTextView = (TextView) view.findViewById(
                            R.id.submission_latitude);
                    TextView submissionLongitudeTextView = (TextView) view.findViewById(
                            R.id.submission_longitude);
                    TextView submissionAccuracyTextView = (TextView) view.findViewById(
                            R.id.submission_accuracy);
                    TextView submissionWorstAccuracyTextView = (TextView) view.findViewById(
                            R.id.submission_worst_accuracy);
                    TextView submissionMeanAccuracyTextView = (TextView) view.findViewById(
                            R.id.submission_mean_accuracy);
                    TextView submissionBestAccuracyTextView = (TextView) view.findViewById(
                            R.id.submission_best_accuracy);
                    TextView submissionMeanDistanceTextView = (TextView) view.findViewById(
                            R.id.submission_mean_distance);

                    submissionStatusTextView.setText(submission.getSubmissionStatus(context));
                    noImagesTextView.setText(Integer.toString(submission.getNoOfImages()));
                    submissionLatitudeTextView.setText(coordinates[0]);
                    submissionLongitudeTextView.setText(coordinates[1]);
                    submissionAccuracyTextView.setText(String.format("%.2f", submission
                            .getBestAccuracy()));
                    submissionWorstAccuracyTextView.setText(String.format("%.2f", submission
                            .getWorstAccuracy()));
                    submissionMeanAccuracyTextView.setText(String.format("%.2f", submission
                            .getMeanAccuracy()));
                    submissionBestAccuracyTextView.setText(String.format("%.2f", submission
                            .getBestAccuracy()));
                    submissionMeanDistanceTextView.setText(String.format("%.2f", submission
                            .getMeanDistanceBetweenImages()));

                    break;
                }
                case 2: {
                    view = inflater.inflate(R.layout.fragment_submission_tab_2, container, false);
                    final Context context = getActivity();
                    GridView gridView = (GridView) view.findViewById(R.id.submission_images_grid);

                    gridView.setAdapter(new ImageAdapter(context, submission.getImages()));
                    break;
                }
                case 3: {
                    view = inflater.inflate(R.layout.fragment_submission_tab_3, container, false);
                    final Context context = getActivity();

                    ListView listView = (ListView) view.findViewById(
                            R.id.submission_power_elements_list);
                    listView.setAdapter(new PowerElementTagsAdapter(context,
                            submission.getPowerElements(), PowerElementTagsAdapter.TAGGED));
                    break;
                }
                case 4: {
                    view = inflater.inflate(R.layout.fragment_submission_tab_4, container, false);
                    WebView mapWebView = (WebView) view.findViewById(R.id.map_web_view);
                    String url = "http://vmjacobsen39.informatik.tu-muenchen.de/";

                    mapWebView.getSettings().setJavaScriptEnabled(true);
                    mapWebView.getSettings().setSupportZoom(true);
                    mapWebView.getSettings().setBuiltInZoomControls(true);
                    mapWebView.loadUrl(url);
                    break;
                }
            }

            return view;
        }
    }

}
