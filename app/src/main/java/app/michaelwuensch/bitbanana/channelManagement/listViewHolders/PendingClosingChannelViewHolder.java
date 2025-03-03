package app.michaelwuensch.bitbanana.channelManagement.listViewHolders;

import android.view.View;

import androidx.annotation.NonNull;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.channelManagement.listItems.ChannelListItem;
import app.michaelwuensch.bitbanana.channelManagement.listItems.PendingClosingChannelItem;

public class PendingClosingChannelViewHolder extends PendingChannelViewHolder {

    public PendingClosingChannelViewHolder(@NonNull View itemView) {
        super(itemView);
    }

    @Override
    int getStatusColor() {
        return R.color.red;
    }

    @Override
    int getStatusText() {
        return R.string.channel_state_pending_closing;
    }

    public void bindPendingClosingChannelItem(PendingClosingChannelItem pendingClosingChannelItem) {
        bindPendingChannelItem(pendingClosingChannelItem.getChannel().getChannel());

        setOnRootViewClickListener(pendingClosingChannelItem, ChannelListItem.TYPE_PENDING_CLOSING_CHANNEL);
    }

}
