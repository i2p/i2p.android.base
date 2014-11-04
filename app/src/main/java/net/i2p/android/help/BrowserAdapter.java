package net.i2p.android.help;

import android.content.Context;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import net.i2p.android.router.R;

public class BrowserAdapter extends RecyclerView.Adapter<BrowserAdapter.ViewHolder> {
    private Context mCtx;
    private Browser[] mBrowsers;
    private OnBrowserSelectedListener mListener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView mIcon;
        public TextView mLabel;
        public ViewHolder(View v) {
            super(v);
            mIcon = (ImageView) v.findViewById(R.id.browser_icon);
            mLabel = (TextView) v.findViewById(R.id.browser_label);
        }
    }

    public static interface OnBrowserSelectedListener {
        public void onBrowserSelected(Browser browser);
    }

    public BrowserAdapter(Context ctx, OnBrowserSelectedListener listener) {
        mCtx = ctx;
        mListener = listener;
    }

    public void setBrowsers(Browser[] browsers) {
        mBrowsers = browsers;
        notifyDataSetChanged();
    }

    public void clear() {
        mBrowsers = null;
        notifyDataSetChanged();
    }

    // Create new views (invoked by the layout manager)
    @Override
    public BrowserAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                        int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.listitem_browser, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final Browser browser = mBrowsers[position];
        holder.mIcon.setImageDrawable(browser.icon);
        holder.mLabel.setText(browser.label);
        if (browser.isKnown && !browser.isSupported) {
            ColorMatrix matrix = new ColorMatrix();
            matrix.setSaturation(0);
            ColorMatrixColorFilter filter = new ColorMatrixColorFilter(matrix);
            holder.mIcon.setColorFilter(filter);
            holder.mLabel.setTextColor(mCtx.getResources().getColor(R.color.primary_text_disabled_material_dark));
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.onBrowserSelected(browser);
            }
        });
    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mBrowsers != null)
            return mBrowsers.length;
        return 0;
    }
}
