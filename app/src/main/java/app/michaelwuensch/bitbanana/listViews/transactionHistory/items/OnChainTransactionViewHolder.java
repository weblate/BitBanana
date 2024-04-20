package app.michaelwuensch.bitbanana.listViews.transactionHistory.items;

import android.view.View;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.WalletUtil;


public class OnChainTransactionViewHolder extends TransactionViewHolder {

    private OnChainTransactionItem mOnChainTransactionItem;

    public OnChainTransactionViewHolder(View v) {
        super(v);
    }

    public void bindOnChainTransactionItem(OnChainTransactionItem onChainTransactionItem) {
        mOnChainTransactionItem = onChainTransactionItem;
        OnChainTransaction transaction = onChainTransactionItem.getOnChainTransaction();

        // Standard state. This prevents list entries to get mixed states because of recycling of the ViewHolder.
        setTranslucent(false);

        if (!transaction.isConfirmed()) {
            setTranslucent(true);
        }

        // Get amounts
        Long amount = transaction.getAmount();
        long fee = transaction.getFee();

        setTimeOfDay(onChainTransactionItem.mCreationDate);

        // is internal?
        if (WalletUtil.isChannelTransaction(transaction)) {

            setIcon(TransactionIcon.INTERNAL);
            setFee(fee, false);

            // Internal transactions are a mess in LND. Some transaction values are not populated, sometimes value and fee is switched.
            // There are transactions for force closes that never get confirmations and get deleted on restarting LND ...

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.force_closed_channel));
                    String pubkeyForceClose = WalletUtil.getNodePubKeyFromChannelTransaction(transaction);
                    String aliasForceClose = AliasManager.getInstance().getAlias(pubkeyForceClose);
                    setSecondaryDescription(aliasForceClose, true);
                    break;
                case 1:
                    // amount > 0 (Channel closed)
                    setAmount(amount, false);
                    setPrimaryDescription(mContext.getString(R.string.closed_channel));
                    String pubkeyClosed = WalletUtil.getNodePubKeyFromChannelTransaction(transaction);
                    String aliasClosed = AliasManager.getInstance().getAlias(pubkeyClosed);
                    setSecondaryDescription(aliasClosed, true);
                    break;
                case -1:
                    if (transaction.hasLabel() && transaction.getLabel().toLowerCase().contains("sweep")) {
                        // in some rare cases for sweep transactions the value is actually the fee payed for the sweep.
                        setAmount(amount, true);
                        setPrimaryDescription(mContext.getString(R.string.closed_channel));
                        String aliasClose = AliasManager.getInstance().getAlias(WalletUtil.getNodePubKeyFromChannelTransaction(transaction));
                        setSecondaryDescription(aliasClose, true);
                    } else {
                        // amount < 0 (Channel opened)
                        // Here we use the fee for the amount, as this is what we actually have to pay.
                        // Doing it this way looks nicer than having 0 for amount and the fee in small.
                        setAmount(fee * -1, true);
                        setPrimaryDescription(mContext.getString(R.string.opened_channel));
                        String aliasOpened = AliasManager.getInstance().getAlias(WalletUtil.getNodePubKeyFromChannelTransaction(transaction));
                        setSecondaryDescription(aliasOpened, true);
                    }
                    break;
            }
        } else {
            // It is a normal transaction
            setIcon(TransactionIcon.ONCHAIN);
            setSecondaryDescription("", false);

            switch (amount.compareTo(0L)) {
                case 0:
                    // amount = 0 (should actually not happen)
                    setFee(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.internal));
                    break;
                case 1:
                    // amount > 0 (received on-chain)
                    setAmount(amount, true);
                    setFee(fee, false);
                    setPrimaryDescription(mContext.getString(R.string.received));
                    break;
                case -1:
                    // amount < 0 (sent on-chain)
                    setAmount(amount + fee, true);
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
