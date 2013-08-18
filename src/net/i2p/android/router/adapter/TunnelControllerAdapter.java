package net.i2p.android.router.adapter;

import java.util.List;

import net.i2p.android.router.R;
import net.i2p.i2ptunnel.TunnelController;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TunnelControllerAdapter extends ArrayAdapter<TunnelController> {
    private final LayoutInflater mInflater;

    public TunnelControllerAdapter(Context context) {
        super(context, android.R.layout.simple_list_item_2);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setData(List<TunnelController> controllers) {
        clear();
        if (controllers != null) {
            for (TunnelController controller : controllers) {
                add(controller);
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = mInflater.inflate(R.layout.listitem_i2ptunnel, parent, false);
        TunnelController controller = getItem(position);

        TextView name = (TextView) v.findViewById(R.id.row_tunnel_name);
        name.setText(controller.getName());

        return v;
    }
}
