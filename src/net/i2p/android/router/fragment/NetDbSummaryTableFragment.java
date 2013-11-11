package net.i2p.android.router.fragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.i2p.android.router.R;
import net.i2p.util.ObjectCounter;
import net.i2p.util.VersionComparator;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class NetDbSummaryTableFragment extends Fragment {
    private static final String CATEGORY = "category";
    private static final String COUNTS = "counts";

    private int mCategory;
    private ObjectCounter<String> mCounts;
    private TableLayout mTable;

    public static NetDbSummaryTableFragment newInstance(int category,
            ObjectCounter<String> counts) {
        NetDbSummaryTableFragment f = new NetDbSummaryTableFragment();
        Bundle args = new Bundle();
        args.putInt(CATEGORY, category);
        args.putSerializable(COUNTS, counts);
        f.setArguments(args);
        return f;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_table, container, false);

        mCategory = getArguments().getInt(CATEGORY);
        mCounts = (ObjectCounter<String>) getArguments().getSerializable(COUNTS);

        mTable = (TableLayout) v.findViewById(R.id.table);

        List<String> objects = new ArrayList<String>(mCounts.objects());
        if (!objects.isEmpty()) {
            createTableTitle();

            switch (mCategory) {
            case 1:
            case 2:
                Collections.sort(objects);
                break;
            default:
                Collections.sort(objects,
                        Collections.reverseOrder(new VersionComparator()));
                break;
            }

            for (String object : objects) {
                int num = mCounts.count(object);
                addTableRow(object, ""+num);
            }
        }

        return v;
    }

    private void createTableTitle() {
        TableRow titleRow;
        TextView tl1, tl2;

        titleRow = new TableRow(getActivity());
        titleRow.setPadding(10, 0, 0, 0);

        tl1 = new TextView(getActivity());
        tl1.setTextSize(20);
        tl2 = new TextView(getActivity());
        tl2.setTextSize(20);

        switch (mCategory) {
        case 1:
            tl1.setText("Transports");
            break;
        case 2:
            tl1.setText("Country");
            break;
        default:
            tl1.setText("Version");
            break;
        }
        tl2.setText("Count");

        titleRow.addView(tl1);
        titleRow.addView(tl2);

        mTable.addView(titleRow);
    }

    private void addTableRow(String name, String count) {
        TableRow row;
        TextView tl1, tl2;

        row = new TableRow(getActivity());
        row.setPadding(10, 0, 0, 0);

        tl1 = new TextView(getActivity());
        tl2 = new TextView(getActivity());

        tl1.setText(name);
        tl2.setText(count);

        row.addView(tl1);
        row.addView(tl2);

        mTable.addView(row);
    }
}
