package app.michaelwuensch.bitbanana.transactionHistory;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.github.lightningnetwork.lnd.lnrpc.Hop;
import com.github.lightningnetwork.lnd.lnrpc.Invoice;
import com.github.lightningnetwork.lnd.lnrpc.Payment;
import com.github.lightningnetwork.lnd.lnrpc.Transaction;
import com.google.protobuf.ByteString;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import app.michaelwuensch.bitbanana.HomeActivity;
import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.connection.manageNodeConfigs.NodeConfigsManager;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.DateItem;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.HistoryItemViewHolder;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.HistoryListItem;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.LnInvoiceItem;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.LnPaymentItem;
import app.michaelwuensch.bitbanana.transactionHistory.listItems.OnChainTransactionItem;
import app.michaelwuensch.bitbanana.transactionHistory.transactionDetails.InvoiceDetailBSDFragment;
import app.michaelwuensch.bitbanana.transactionHistory.transactionDetails.LnPaymentDetailBSDFragment;
import app.michaelwuensch.bitbanana.transactionHistory.transactionDetails.OnChainTransactionDetailBSDFragment;
import app.michaelwuensch.bitbanana.util.AliasManager;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.OnSingleClickListener;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.Wallet;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * A simple {@link Fragment} subclass.
 */
public class TransactionHistoryFragment extends Fragment implements Wallet.HistoryListener, Wallet.InvoiceSubscriptionListener, Wallet.ChannelsUpdatedSubscriptionListener, SwipeRefreshLayout.OnRefreshListener, TransactionSelectListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String LOG_TAG = TransactionHistoryFragment.class.getSimpleName();

    private ImageView mListOptions;
    private RecyclerView mRecyclerView;
    private HistoryItemAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private TextView mEmptyListText;
    private TextView mTitle;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private CompositeDisposable mCompositeDisposable;
    private SearchView mSearchView;
    private ImageView mSearchButton;
    private String mCurrentSearchString = "";

    private List<HistoryListItem> mHistoryItems;


    public TransactionHistoryFragment() {
        // Required empty public constructor
    }

    public SearchView getSearchView() {
        return mSearchView;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_history, container, false);

        mRecyclerView = view.findViewById(R.id.historyList);
        mListOptions = view.findViewById(R.id.listOptions);
        mEmptyListText = view.findViewById(R.id.listEmpty);
        mTitle = view.findViewById(R.id.heading);
        mSearchView = view.findViewById(R.id.searchView);
        mSearchButton = view.findViewById(R.id.searchButton);

        ImageView backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(new OnSingleClickListener() {
            @Override
            public void onSingleClick(View v) {
                ((HomeActivity) getActivity()).mViewPager.setCurrentItem(0);
            }
        });


        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTitle.setVisibility(View.GONE);
                mSearchView.setVisibility(View.VISIBLE);
                mSearchButton.setVisibility(View.GONE);
                mSearchView.setFocusable(true);
                mSearchView.setIconified(false);
            }
        });

        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                mCurrentSearchString = newText;
                final List<HistoryListItem> filteredHistoryList = filter(mHistoryItems, newText);
                mAdapter.replaceAll(filteredHistoryList);
                mRecyclerView.scrollToPosition(0);
                return true;
            }
        });

        mSearchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                mTitle.setVisibility(View.VISIBLE);
                mSearchView.setVisibility(View.GONE);
                mSearchButton.setVisibility(View.VISIBLE);
                return false;
            }
        });

        mHistoryItems = new ArrayList<>();
        mCompositeDisposable = new CompositeDisposable();

        // Register listeners
        Wallet.getInstance().registerHistoryListener(this);
        Wallet.getInstance().registerInvoiceSubscriptionListener(this);
        Wallet.getInstance().registerChannelsUpdatedSubscriptionListener(this);
        PrefsUtil.getPrefs().registerOnSharedPreferenceChangeListener(this);


        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(getActivity());
        mRecyclerView.setLayoutManager(mLayoutManager);

        // create and set adapter
        mAdapter = new HistoryItemAdapter(this, mCompositeDisposable);
        mRecyclerView.setAdapter(mAdapter);


        // SwipeRefreshLayout
        mSwipeRefreshLayout = view.findViewById(R.id.swiperefresh);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.sea_blue_gradient));
        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.white));


        updateHistoryDisplayList();


        mListOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder adb = new AlertDialog.Builder(getActivity());
                LayoutInflater adbInflater = LayoutInflater.from(getActivity());
                View DialogLayout = adbInflater.inflate(R.layout.dialog_history_list_settings, null);
                Switch normalSwitch = DialogLayout.findViewById(R.id.switchNormal);
                Switch expiredSwitch = DialogLayout.findViewById(R.id.switchExpired);
                Switch internalSwitch = DialogLayout.findViewById(R.id.switchInternal);
                normalSwitch.setChecked(PrefsUtil.getPrefs().getBoolean("showNormalTransactions", true));
                expiredSwitch.setChecked(PrefsUtil.getPrefs().getBoolean("showExpiredRequests", false));
                internalSwitch.setChecked(PrefsUtil.getPrefs().getBoolean("showInternalTransactions", true));
                normalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        PrefsUtil.editPrefs().putBoolean("showNormalTransactions", isChecked).commit();
                        updateHistoryDisplayList();
                    }
                });
                expiredSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        PrefsUtil.editPrefs().putBoolean("showExpiredRequests", isChecked).commit();
                        updateHistoryDisplayList();
                    }
                });
                internalSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        PrefsUtil.editPrefs().putBoolean("showInternalTransactions", isChecked).commit();
                        updateHistoryDisplayList();
                    }
                });
                adb.setView(DialogLayout);
                adb.setTitle(R.string.filter_transactions);
                adb.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                Dialog dlg = adb.create();
                // Apply FLAG_SECURE to dialog to prevent screen recording
                if (PrefsUtil.isScreenRecordingPrevented()) {
                    dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
                }
                dlg.show();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        mCompositeDisposable.dispose();
        super.onDestroyView();
    }

    // This method is used if the data set of the list changed. E.g. new transactions appeared, etc.
    public void updateHistoryDisplayList() {

        // Save state, we want to keep the scroll offset after the update.
        Parcelable recyclerViewState;
        recyclerViewState = mRecyclerView.getLayoutManager().onSaveInstanceState();


        mHistoryItems.clear();

        List<HistoryListItem> normalPayments = new LinkedList<>();
        List<HistoryListItem> expiredRequest = new LinkedList<>();
        List<HistoryListItem> internalTransactions = new LinkedList<>();
        Set<HistoryListItem> dateLines = new HashSet<>();

        if (NodeConfigsManager.getInstance().hasAnyConfigs()) {

            // Add all payment relevant items to one of the lists above

            if (Wallet.getInstance().mOnChainTransactionList != null) {
                for (Transaction t : Wallet.getInstance().mOnChainTransactionList) {
                    OnChainTransactionItem onChainTransactionItem = new OnChainTransactionItem(t);

                    if (Wallet.getInstance().isTransactionInternal(t)) {
                        internalTransactions.add(onChainTransactionItem);
                    } else {
                        if (t.getAmount() != 0) {
                            normalPayments.add(onChainTransactionItem);
                        }
                    }
                }
            }

            if (Wallet.getInstance().mInvoiceList != null) {
                for (Invoice i : Wallet.getInstance().mInvoiceList) {

                    LnInvoiceItem lnInvoiceItem = new LnInvoiceItem(i);

                    // add to list according to current state of the invoice
                    if (Wallet.getInstance().isInvoicePayed(i)) {
                        normalPayments.add(lnInvoiceItem);
                    } else {
                        if (Wallet.getInstance().isInvoiceExpired(i)) {
                            expiredRequest.add(lnInvoiceItem);
                        } else {
                            normalPayments.add(lnInvoiceItem);
                        }
                    }
                }
            }

            if (Wallet.getInstance().mPaymentsList != null) {
                for (Payment p : Wallet.getInstance().mPaymentsList) {
                    LnPaymentItem lnPaymentItem = new LnPaymentItem(p);
                    normalPayments.add(lnPaymentItem);
                }
            }

        }

        // Apply filters

        if (PrefsUtil.getPrefs().getBoolean("showNormalTransactions", true)) {
            mHistoryItems.addAll(normalPayments);
        }

        if (PrefsUtil.getPrefs().getBoolean("showExpiredRequests", false)) {
            mHistoryItems.addAll(expiredRequest);
        }

        if (PrefsUtil.getPrefs().getBoolean("showInternalTransactions", true)) {
            mHistoryItems.addAll(internalTransactions);
        }


        // Add the Date Lines
        for (HistoryListItem item : mHistoryItems) {
            DateItem dateItem = new DateItem(item.mCreationDate);
            dateLines.add(dateItem);
        }
        mHistoryItems.addAll(dateLines);


        // Show "No transactions" if the list is empty

        if (mHistoryItems.size() == 0) {
            mEmptyListText.setVisibility(View.VISIBLE);
        } else {
            mEmptyListText.setVisibility(View.GONE);
        }


        // Update the list view
        if (mCurrentSearchString.isEmpty()) {
            mAdapter.replaceAll(mHistoryItems);
        } else {
            final List<HistoryListItem> filteredHistoryList = filter(mHistoryItems, mCurrentSearchString);
            mAdapter.replaceAll(filteredHistoryList);
        }

        // Restore state (e.g. scroll offset)
        mRecyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
    }

    // The redraw method is used to redraw the view holders on an unchanged data set
    // This is for example important if we want to change the displayed currency or when we have new information about node names that we didn't have before.
    public void redrawHistoryList() {

        // Redraw all view Holders visible on the screen.
        for (int i = 0; i < mRecyclerView.getAdapter().getItemCount(); i++) {
            HistoryItemViewHolder holder = (HistoryItemViewHolder) mRecyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                holder.refreshViewHolder();
            }
        }

        // Flush the cached items that are not currently visible on the screen as these were not redrawn by the function above.
        // The cache is activated again to keep good performance.
        mRecyclerView.setItemViewCacheSize(-1); // setting to 0 still left one not redrawn. -1 actually works.
        mRecyclerView.setItemViewCacheSize(2); // 2 is the default value
    }

    public void resetHistoryFragment() {
        mHistoryItems.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onHistoryUpdated() {
        updateHistoryDisplayList();
        mSwipeRefreshLayout.setRefreshing(false);
        BBLog.d(LOG_TAG, "History updated!");
    }

    @Override
    public void onResume() {
        super.onResume();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Unregister listeners
        Wallet.getInstance().unregisterHistoryListener(this);
        Wallet.getInstance().unregisterInvoiceSubscriptionListener(this);
        Wallet.getInstance().unregisterChannelsUpdatedSubscriptionListener(this);
        PrefsUtil.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onRefresh() {
        if (NodeConfigsManager.getInstance().hasAnyConfigs() && Wallet.getInstance().isInfoFetched()) {
            Wallet.getInstance().fetchLNDTransactionHistory();
            redrawHistoryList();
        } else {
            mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onNewInvoiceAdded(Invoice invoice) {
        LnInvoiceItem item = new LnInvoiceItem(invoice);
        mHistoryItems.add(item);
        mAdapter.add(item);

        // Add dateline if necessary
        DateItem dateItem = new DateItem(item.mCreationDate);
        mAdapter.add(dateItem);

        mRecyclerView.scrollToPosition(0);
    }

    @Override
    public void onExistingInvoiceUpdated(Invoice invoice) {
        LnInvoiceItem item = new LnInvoiceItem(invoice);
        // Add updates the view if it already exists.
        mHistoryItems.add(item);
        mAdapter.add(item);

        // Add dateline if necessary
        DateItem dateItem = new DateItem(item.mCreationDate);
        mAdapter.add(dateItem);
    }

    @Override
    public void onTransactionSelect(ByteString transaction, int type) {
        Bundle bundle = new Bundle();

        if (transaction != null) {
            switch (type) {
                case HistoryListItem.TYPE_ON_CHAIN_TRANSACTION:
                    OnChainTransactionDetailBSDFragment transactionDetailBSDFragment = new OnChainTransactionDetailBSDFragment();
                    bundle.putSerializable(OnChainTransactionDetailBSDFragment.ARGS_TRANSACTION, transaction);
                    transactionDetailBSDFragment.setArguments(bundle);
                    transactionDetailBSDFragment.show(getActivity().getSupportFragmentManager(), OnChainTransactionDetailBSDFragment.TAG);
                    break;
                case HistoryListItem.TYPE_LN_INVOICE:
                    InvoiceDetailBSDFragment invoiceDetailBSDFragment = new InvoiceDetailBSDFragment();
                    bundle.putSerializable(InvoiceDetailBSDFragment.ARGS_TRANSACTION, transaction);
                    invoiceDetailBSDFragment.setArguments(bundle);
                    invoiceDetailBSDFragment.show(getActivity().getSupportFragmentManager(), InvoiceDetailBSDFragment.TAG);
                    break;
                case HistoryListItem.TYPE_LN_PAYMENT:
                    LnPaymentDetailBSDFragment lnPaymentDetailBSDFragment = new LnPaymentDetailBSDFragment();
                    bundle.putSerializable(LnPaymentDetailBSDFragment.ARGS_TRANSACTION, transaction);
                    lnPaymentDetailBSDFragment.setArguments(bundle);
                    lnPaymentDetailBSDFragment.show(getActivity().getSupportFragmentManager(), LnPaymentDetailBSDFragment.TAG);
                    break;
                default:
                    throw new IllegalStateException("Unknown history list item type: " + type);
            }

        } else {
            Toast.makeText(getActivity(), R.string.coming_soon, Toast.LENGTH_SHORT).show();
        }

    }

    private List<HistoryListItem> filter(List<HistoryListItem> items, String query) {
        final String lowerCaseQuery = query.toLowerCase();

        if (lowerCaseQuery.isEmpty()) {
            return items;
        }

        final List<HistoryListItem> filteredItemList = new ArrayList<>();

        for (HistoryListItem item : items) {
            String text;
            switch (item.getType()) {
                case HistoryListItem.TYPE_LN_INVOICE:
                    String invoiceMemo = ((LnInvoiceItem) item).getInvoice().getMemo();
                    String invoiceAmount = MonetaryUtil.getInstance().getPrimaryDisplayAmount(((LnInvoiceItem) item).getInvoice().getValue());
                    text = invoiceMemo + invoiceAmount;
                    break;
                case HistoryListItem.TYPE_LN_PAYMENT:
                    String paymentMemo = ((LnPaymentItem) item).getMemo();
                    String paymentAmount = MonetaryUtil.getInstance().getPrimaryDisplayAmount(((LnPaymentItem) item).getPayment().getValueSat());

                    Hop lastHop = ((LnPaymentItem) item).getPayment().getHtlcs(0).getRoute().getHops(((LnPaymentItem) item).getPayment().getHtlcs(0).getRoute().getHopsCount() - 1);
                    String payeePubKey = lastHop.getPubKey();
                    String payeeName = "";
                    if (ContactsManager.getInstance().doesContactDataExist(payeePubKey)) {
                        payeeName = ContactsManager.getInstance().getNameByContactData(payeePubKey);
                    }
                    text = paymentMemo + paymentAmount + payeeName;
                    break;
                case HistoryListItem.TYPE_ON_CHAIN_TRANSACTION:
                    String transactionAmount = MonetaryUtil.getInstance().getPrimaryDisplayAmount(((OnChainTransactionItem) item).getOnChainTransaction().getAmount());
                    // Searching for the nodeNames will probably have bad performance when there are a lot of Channels, Contacts & Transactions.
                    String nodePubKey = Wallet.getInstance().getNodePubKeyFromChannelTransaction(((OnChainTransactionItem) item).getOnChainTransaction());
                    String nodeName = AliasManager.getInstance().getAlias(nodePubKey);
                    text = transactionAmount + nodeName;
                    break;
                default:
                    text = "";
            }
            if (text != null && text.toLowerCase().contains(lowerCaseQuery)) {
                filteredItemList.add(item);
            }
        }

        // Add the Date Lines
        Set<HistoryListItem> dateLines = new HashSet<>();
        for (HistoryListItem item : filteredItemList) {
            DateItem dateItem = new DateItem(item.mCreationDate);
            dateLines.add(dateItem);
        }
        filteredItemList.addAll(dateLines);

        return filteredItemList;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key != null) {
            if (key.equals("firstCurrencyIsPrimary")) {
                redrawHistoryList();
            }
        }
    }

    @Override
    public void onChannelsUpdated() {
        redrawHistoryList();
    }
}
