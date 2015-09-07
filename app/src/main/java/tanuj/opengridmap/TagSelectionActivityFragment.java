package tanuj.opengridmap;

import android.content.Context;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.Toast;

import java.util.ArrayList;

import tanuj.opengridmap.data.OpenGridMapDbHelper;
import tanuj.opengridmap.models.PowerElement;
import tanuj.opengridmap.models.Submission;
import tanuj.opengridmap.views.adapters.PowerElementTagsAdapter;

public class TagSelectionActivityFragment extends Fragment {
    private Submission submission;

    private GridView taggedPowerElementsList;

    private GridView notTaggedPowerElementsList;

    private ArrayList<PowerElement> taggedPowerElements;

    private ArrayList<PowerElement> notTaggedPowerElements;

    private PowerElementTagsAdapter taggedPowerElementsListAdapter;
    private PowerElementTagsAdapter notTaggedPowerElementsListAdapter;

    private static final String TAGGED_POWER_ELEMENTS_KEY = "TAGGED_POWER_ELEMENTS";
    private static final String NOT_TAGGED_POWER_ELEMENTS_KEY = "NOT_TAGGED_POWER_ELEMENTS";

    public TagSelectionActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tag_selection, container, false);

        ImageButton confirmButton = (ImageButton) view.findViewById(R.id.confirm_button);

        if (savedInstanceState != null && savedInstanceState.containsKey(TAGGED_POWER_ELEMENTS_KEY)
                && savedInstanceState.containsKey(NOT_TAGGED_POWER_ELEMENTS_KEY)) {
            taggedPowerElements = savedInstanceState.getParcelableArrayList(TAGGED_POWER_ELEMENTS_KEY);
            notTaggedPowerElements = savedInstanceState.getParcelableArrayList(NOT_TAGGED_POWER_ELEMENTS_KEY);
        } else {
            OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(getActivity());

            long submissionId = getActivity().getIntent().getLongExtra("SubmissionId", -1);

            if (submissionId > -1) {
                submission = dbHelper.getSubmission(submissionId);
            }

            if (null != submission) {
                taggedPowerElements = submission.getPowerElements();
                notTaggedPowerElements = dbHelper.getNotTaggedPowerElements(submission);
            }
        }



        taggedPowerElementsListAdapter = new PowerElementTagsAdapter(getActivity(),taggedPowerElements, PowerElementTagsAdapter.TAGGED);
        taggedPowerElementsList = (GridView) view.findViewById(R.id.power_elements_tagged_list);
        taggedPowerElementsList.setAdapter(taggedPowerElementsListAdapter);


        notTaggedPowerElementsListAdapter = new PowerElementTagsAdapter(getActivity(), notTaggedPowerElements, PowerElementTagsAdapter.NOT_TAGGED);
        notTaggedPowerElementsList = (GridView) view.findViewById(R.id.power_elements_not_tagged_list);
        notTaggedPowerElementsList.setAdapter(notTaggedPowerElementsListAdapter);

        taggedPowerElementsList.setOnItemClickListener(taggedPowerElementClickListener);
        notTaggedPowerElementsList.setOnItemClickListener(notTaggedPowerElementClickListener);

        confirmButton.setOnClickListener(confirmButtonClickListener);

//        taggedPowerElementsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                if (taggedPowerElementsListAdapter.getCount() > 1) {
//                    PowerElement p = taggedPowerElementsListAdapter.removeTag(position);
//                    notTaggedPowerElementsListAdapter.addTag(p);
//                } else {
//                    Toast.makeText(getActivity(), "Atleast 1 tag required", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

//        notTaggedPowerElementsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                PowerElement p = notTaggedPowerElementsListAdapter.removeTag(position);
//                taggedPowerElementsListAdapter.addTag(p);
//            }
//        });



        return view;
    }

    private AdapterView.OnItemClickListener taggedPowerElementClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (taggedPowerElementsListAdapter.getCount() > 1) {
                PowerElement p = taggedPowerElementsListAdapter.removeTag(position);
                notTaggedPowerElementsListAdapter.addTag(p);
            } else {
                Toast.makeText(getActivity(), "Atleast 1 tag required", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private AdapterView.OnItemClickListener notTaggedPowerElementClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            PowerElement p = notTaggedPowerElementsListAdapter.removeTag(position);
            taggedPowerElementsListAdapter.addTag(p);
        }
    };

    private View.OnClickListener confirmButtonClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Context context = getActivity();
            if (submission != null && taggedPowerElements.size() > 0) {
                OpenGridMapDbHelper dbHelper = new OpenGridMapDbHelper(context);

                for (PowerElement powerElement : taggedPowerElements) {
                    dbHelper.addPowerElementToSubmission(powerElement, submission);
                }

                submission.addToUploadQueue(context);

                Intent serviceIntent = new Intent(context, ThumbnailGenerationService.class);
                serviceIntent.putExtra("SubmissionId", submission.getId());
                context.startService(serviceIntent);
            }
            getActivity().finish();
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(TAGGED_POWER_ELEMENTS_KEY, taggedPowerElementsListAdapter
                .getPowerElements());
        outState.putParcelableArrayList(NOT_TAGGED_POWER_ELEMENTS_KEY, notTaggedPowerElementsListAdapter
                .getPowerElements());

        super.onSaveInstanceState(outState);
    }
}
