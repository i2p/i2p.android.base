package net.i2p.android.router.fragment;

import net.i2p.android.router.R;
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

    private int mCategory;
    private TableLayout mTable;

    public static NetDbSummaryTableFragment newInstance(int category) {
        NetDbSummaryTableFragment f = new NetDbSummaryTableFragment();
        Bundle args = new Bundle();
        args.putInt(CATEGORY, category);
        f.setArguments(args);
        return f;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_table, container, false);

        mCategory = getArguments().getInt(CATEGORY);

        mTable = (TableLayout) v.findViewById(R.id.table);
        createTableTitle();
        addTableRow("foo", "123");
        addTableRow("bar", "45");
        if (mCategory == 2)
            addTableRow("bing", "67");

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
