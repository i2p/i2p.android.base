package net.i2p.android.util;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eowise.recyclerview.stickyheaders.StickyHeadersAdapter;

import net.i2p.android.router.R;

public class AlphanumericHeaderAdapter implements StickyHeadersAdapter<AlphanumericHeaderAdapter.ViewHolder> {
    public interface SortedAdapter {
        @NonNull
        String getSortString(int position);
    }

    private static final String NUMBERS = "0123456789";

    private SortedAdapter mAdapter;
    private boolean mCombineNumeric;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView character;

        public ViewHolder(View itemView) {
            super(itemView);
            character = (TextView) itemView.findViewById(R.id.character);
        }
    }

    public AlphanumericHeaderAdapter(SortedAdapter adapter) {
        this(adapter, true);
    }

    public AlphanumericHeaderAdapter(SortedAdapter adapter, boolean combineNumeric) {
        this.mAdapter = adapter;
        this.mCombineNumeric = combineNumeric;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.header_alphanumeric, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder headerViewHolder, int position) {
        String sortString = mAdapter.getSortString(position).toUpperCase();
        if (sortString.isEmpty())
            headerViewHolder.itemView.setVisibility(View.GONE);
        else {
            CharSequence character = sortString.subSequence(0, 1);
            if (mCombineNumeric && NUMBERS.contains(character))
                character = "0-9";
            headerViewHolder.character.setText(character);
            headerViewHolder.itemView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public long getHeaderId(int position) {
        String sortString = mAdapter.getSortString(position).toUpperCase();
        if (sortString.isEmpty())
            return Integer.MAX_VALUE;

        CharSequence character = sortString.subSequence(0, 1);
        if (mCombineNumeric && NUMBERS.contains(character))
            return "0-9".hashCode();

        return sortString.charAt(0);
    }
}
