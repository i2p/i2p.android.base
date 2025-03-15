package net.i2p.android.i2ptunnel;

import android.content.Context;
import android.os.Build;
//import android.support.v4.util.Pair;
import androidx.core.util.Pair;
//import android.support.v4.view.ViewCompat;
import androidx.core.view.ViewCompat;
//import android.support.v7.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.i2p.android.router.R;
import net.i2p.android.util.FragmentUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *  Contains the List of TunnelEntries.
 *  There's two of these, one for client tunnels and
 *  one for server tunnels.
 *  Created by the TunnelListFragment.
 */
public class TunnelEntryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context mCtx;
    private final boolean mClientTunnels;
    private final TunnelListFragment.OnTunnelSelectedListener mListener;
    private final FragmentUtils.TwoPaneProvider mTwoPane;
    private List<TunnelEntry> mTunnels;
    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = -1;

    public static class SimpleViewHolder extends RecyclerView.ViewHolder {
        public SimpleViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class TunnelViewHolder extends RecyclerView.ViewHolder {
        public final ImageView status;
        public final TextView name;
        public final TextView description;
        public final TextView interfacePort;

        public TunnelViewHolder(View itemView) {
            super(itemView);

            status = (ImageView) itemView.findViewById(R.id.tunnel_status);
            name = (TextView) itemView.findViewById(R.id.tunnel_name);
            description = (TextView) itemView.findViewById(R.id.tunnel_description);
            interfacePort = (TextView) itemView.findViewById(R.id.tunnel_interface_port);
        }
    }

    public TunnelEntryAdapter(Context context, boolean clientTunnels,
                              TunnelListFragment.OnTunnelSelectedListener listener,
                              FragmentUtils.TwoPaneProvider twoPane) {
        super();
        mCtx = context;
        mClientTunnels = clientTunnels;
        mListener = listener;
        mTwoPane = twoPane;
    }

    public void setTunnels(List<TunnelEntry> tunnels) {
        mTunnels = tunnels;
        Log.d("TunnelEntryAdapter", "setTunnels: size=" + (tunnels != null ? tunnels.size() : "null"));
        notifyDataSetChanged();
    }

    public void addTunnel(TunnelEntry tunnel) {
        if (mTunnels == null)
            mTunnels = new ArrayList<TunnelEntry>();
        boolean wasEmpty = mTunnels.isEmpty();
        mTunnels.add(tunnel);
        if (wasEmpty) {
            notifyDataSetChanged();
        } else {
            notifyItemInserted(mTunnels.size() - 1);
        }
    }

    public TunnelEntry getTunnel(int position) {
        if (position < 0)
            return null;

        return mTunnels.get(position);
    }

    public void setActivatedPosition(int position) {
        mActivatedPosition = position;
    }

    public int getActivatedPosition() {
        return mActivatedPosition;
    }

    public void clearActivatedPosition() {
        mActivatedPosition = -1;
    }

    @Override
    public int getItemViewType(int position) {
        if (mTunnels == null)
            return R.string.router_not_running;
        else if (mTunnels.isEmpty())
            return R.layout.listitem_empty;
        else
            return R.layout.listitem_i2ptunnel;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int vt = viewType;
        if (viewType == R.string.router_not_running)
            vt = R.layout.listitem_empty;

        View v = LayoutInflater.from(parent.getContext())
                .inflate(vt, parent, false);
        switch (viewType) {
            case R.layout.listitem_i2ptunnel:
                return new TunnelViewHolder(v);
            default:
                return new SimpleViewHolder(v);
        }
    }

    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        Log.d("TunnelEntryAdapter", "onBindViewHolder: position=" + position +
              " viewType=" + holder.getItemViewType());
        switch (holder.getItemViewType()) {
            case R.string.router_not_running:
                ((TextView) holder.itemView).setText(
                        mCtx.getString(R.string.i2ptunnel_not_initialized));
                break;

            case R.layout.listitem_empty:
                ((TextView) holder.itemView).setText(mClientTunnels ?
                        R.string.no_configured_client_tunnels :
                        R.string.no_configured_server_tunnels);
                break;

            case R.layout.listitem_i2ptunnel:
                final TunnelViewHolder tvh = (TunnelViewHolder) holder;
                final TunnelEntry tunnel = getTunnel(position);

                tvh.status.setImageDrawable(tunnel.getStatusIcon());
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
                    tvh.status.setBackgroundDrawable(tunnel.getStatusBackground());
                else
                    tvh.status.setBackground(tunnel.getStatusBackground());
                ViewCompat.setTransitionName(tvh.status,
                        "status" + tunnel.getId());

                tvh.name.setText(tunnel.getName());
                tvh.description.setText(tunnel.getDescription());
                tvh.interfacePort.setText(tunnel.getTunnelLink(false));

                tvh.itemView.setSelected(mTwoPane.isTwoPane() && position == mActivatedPosition);
                tvh.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // TODO
                        // lint priority 8/10
                        // lint: Do not treat position as fixed; only use immediately and call holder.getAdapterPosition() to look it up later
                        // javadocs: Note that unlike ListView, RecyclerView will not call this method again
                        // if the position of the item changes in the data set unless the item itself is invalidated
                        // or the new position cannot be determined.
                        // For this reason, you should only use the position parameter while acquiring
                        // the related data item inside this method and should not keep a copy of it.
                        // If you need the position of an item later on (e.g. in a click listener),
                        // use RecyclerView.ViewHolder.getAdapterPosition() which will have the updated adapter position.
                        int oldPosition = mActivatedPosition;
                        mActivatedPosition = position;
                        notifyItemChanged(oldPosition);
                        notifyItemChanged(position);
                        Pair<View, String> statusPair = Pair.create(
                                (View)tvh.status,
                                ViewCompat.getTransitionName(tvh.status));
                        Pair<View, String>[] pairs = new Pair[]{ statusPair};
                        mListener.onTunnelSelected(tunnel.getId(), pairs);
                        view.invalidate();
                    }
                });
                tvh.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                    //@Override
                    public boolean onLongClick(View view) {
                        setClipboard(mCtx, tunnel.getDestHashBase32());
                        Toast clipboardMessage = Toast.makeText(mCtx, R.string.copied_base32_system_notification_title, Toast. LENGTH_LONG);
                        clipboardMessage.setGravity(Gravity.TOP, 0, 0); //optional
                        clipboardMessage.show();
                        view.invalidate();
                        return true;

                    }
                });
                break;

            default:
                break;
        }

    }

    // Return the size of the dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        if (mTunnels == null || mTunnels.isEmpty()) {
            Log.d("TunnelEntryAdapter", "getItemCount: returning 1 for empty/null state");
            return 1;
        }
        Log.d("TunnelEntryAdapter", "getItemCount: returning " + mTunnels.size());
        return mTunnels.size();
    }
}
