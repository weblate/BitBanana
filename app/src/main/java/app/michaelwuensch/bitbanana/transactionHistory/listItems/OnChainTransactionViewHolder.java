package app.michaelwuensch.bitbanana.transactionHistory.listItems;

import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.Wallet;


public class OnChainTransactionViewHolder extends TransactionViewHolder {

    private OnChainTransactionItem mOnChainTransactionItem;

    public OnChainTransactionViewHolder(View v) {
        super(v);
    }

    public void bindOnChainTransactionItem(OnChainTransactionItem onChainTransactionItem) {
        mOnChainTransactionItem = onChainTransactionItem;

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setDisplayMode(true);

        if (onChainTransactionItem.getOnChainTransaction().getNumConfirmations() == 0) {
            setDisplayMode(false);
        }

        // Get amounts
        Long amount = onChainTransactionItem.getOnChainTransaction().getAmount();
        long fee = onChainTransactionItem.getOnChainTransaction().getTotalFees();

        setTimeOfDay(onChainTransactionItem.mCreationDate);

        // is internal?
        if (Wallet.getInstance().isTransactionInternal(onChainTransactionItem.getOnChainTransaction())) {

            setIcon(TransactionIcon.INTERNAL);
            setFee(fee, false);

            // Internal transactions are a mess in LND. Some transaction values are not populated, sometimes value and fee is switched.
            // There are transactions for force closes that never get confirmations and get deleted on restarting LND ...

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.force_closed_channel));
                    String pubkeyForceClose = Wallet.getInstance().getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction());
                    String aliasForceClose = AliasManager.getInstance().getAlias(pubkeyForceClose);
                    setSecondaryDescription(aliasForceClose, true);
                    break;
                case 1:
                    // amount > 0 (Channel closed)
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.closed_channel));
                    String pubkeyClosed = Wallet.getInstance().getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction());
                    String aliasClosed = AliasManager.getInstance().getAlias(pubkeyClosed);
                    setSecondaryDescription(aliasClosed, true);
                    break;
                case -1:
                    if (onChainTransactionItem.getOnChainTransaction().getLabel().toLowerCase().contains("sweep")) {
                        // in some rare cases for sweep transactions the value is actually the fee payed for the sweep.
                        setAmount(amount, true);
                        setPrimaryDescription(mContext.getString(R.string.closed_channel));
                        String aliasClose = AliasManager.getInstance().getAlias(Wallet.getInstance().getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction()));
                        setSecondaryDescription(aliasClose, true);
                    } else {
                        // amount < 0 (Channel opened)
                        // Here we use the fee for the amount, as this is what we actually have to pay.
                        // Doing it this way looks nicer than having 0 for amount and the fee in small.
                        setAmount(fee * -1, true);
                        setPrimaryDescription(mContext.getString(R.string.opened_channel));
                        String aliasOpened = AliasManager.getInstance().getAlias(Wallet.getInstance().getNodePubKeyFromChannelTransaction(onChainTransactionItem.getOnChainTransaction()));
                        setSecondaryDescription(aliasOpened, true);
                    }
                    break;
            }
        } else {
            // It is a normal transaction
            setIcon(TransactionIcon.ONCHAIN);
            setAmount(amount, true);
            setSecondaryDescription("", false);

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0 (should actually not happen)
                    setFee(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.internal));
                    break;
                case 1:
                    // amount > 0 (received on-chain)
                    setFee(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.received));
                    break;
                case -1:
                    // amount < 0 (sent on-chain)
                    setFee(fee, true);
                    setPrimaryDescription(mContext.getString(R.string.sent));
                    break;
            }
        }

        // Set on click listener
        setOnRootViewClickListener(onChainTransactionItem, HistoryListItem.TYPE_ON_CHAIN_TRANSACTION);
    }

    @Override
    public void refreshViewHolder() {
        bindOnChainTransactionItem(mOnChainTransactionItem);
        super.refreshViewHolder();
    }
}
