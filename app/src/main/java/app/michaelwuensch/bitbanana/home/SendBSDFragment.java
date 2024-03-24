package app.michaelwuensch.bitbanana.home;


import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

import com.github.lightningnetwork.lnd.lnrpc.EstimateFeeRequest;
import com.github.lightningnetwork.lnd.lnrpc.SendCoinsRequest;
import com.google.android.material.snackbar.Snackbar;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfigsManager;
import app.michaelwuensch.bitbanana.backends.lnd.connection.LndConnection;
import app.michaelwuensch.bitbanana.baseClasses.BaseBSDFragment;
import app.michaelwuensch.bitbanana.contacts.ContactsManager;
import app.michaelwuensch.bitbanana.customView.BSDProgressView;
import app.michaelwuensch.bitbanana.customView.BSDResultView;
import app.michaelwuensch.bitbanana.customView.BSDScrollableMainView;
import app.michaelwuensch.bitbanana.customView.ExpandableTextView;
import app.michaelwuensch.bitbanana.customView.LightningFeeView;
import app.michaelwuensch.bitbanana.customView.NumpadView;
import app.michaelwuensch.bitbanana.customView.OnChainFeeView;
import app.michaelwuensch.bitbanana.customView.PaymentCommentView;
import app.michaelwuensch.bitbanana.models.DecodedBolt11;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.DebounceHandler;
import app.michaelwuensch.bitbanana.util.MonetaryUtil;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.WalletUtil;
import app.michaelwuensch.bitbanana.wallet.Wallet_Balance;
import app.michaelwuensch.bitbanana.wallet.Wallet_TransactionHistory;


public class SendBSDFragment extends BaseBSDFragment {

    private static final String LOG_TAG = SendBSDFragment.class.getSimpleName();

    private BSDScrollableMainView mBSDScrollableMainView;
    private BSDProgressView mProgressScreen;
    private BSDResultView mResultView;
    private ConstraintLayout mContentTopLayout;
    private ConstraintLayout mInputLayout;
    private EditText mEtAmount;
    private ExpandableTextView mExtvMemo;
    private TextView mTvUnit;
    private OnChainFeeView mOnChainFeeView;
    private LightningFeeView mLightningFeeView;
    private NumpadView mNumpad;
    private Button mBtnSend;
    private Button mFallbackButton;
    private TextView mPayee;
    private PaymentCommentView mPcvComment;

    private DecodedBolt11 mDecodedBolt11;
    private String mFallbackOnChainInvoice;
    private String mMemo;
    private String mOnChainAddress;
    private boolean mOnChain;
    private long mFixedAmount;
    private Handler mHandler;
    private boolean mAmountValid = true;
    private boolean mSendButtonEnabled_input;
    private boolean mSendButtonEnabled_feeCalculate;
    long mCalculatedFee;
    double mCalculatedFeePercent;
    private String mKeysendPubkey;
    private boolean mIsKeysend;
    private long mSendAmountMsat;
    private boolean mBlockOnInputChanged;
    private View mRootView;
    private DebounceHandler mFeeCaclulationDebounceHandler = new DebounceHandler();

    public static SendBSDFragment createLightningDialog(DecodedBolt11 decodedBolt11, String fallbackOnChainInvoice) {
        Intent intent = new Intent();
        intent.putExtra("keysend", false);
        intent.putExtra("onChain", false);
        intent.putExtra("lnPaymentRequest", decodedBolt11);
        intent.putExtra("fallbackOnChainInvoice", fallbackOnChainInvoice);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    public static SendBSDFragment createOnChainDialog(String address, long amount, String message) {
        Intent intent = new Intent();
        intent.putExtra("keysend", false);
        intent.putExtra("onChain", true);
        intent.putExtra("onChainAddress", address);
        intent.putExtra("onChainAmount", amount);
        intent.putExtra("onChainMessage", message);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    public static SendBSDFragment createKeysendDialog(String pubkey) {
        Intent intent = new Intent();
        intent.putExtra("onChain", false);
        intent.putExtra("keysend", true);
        intent.putExtra("keysendPubkey", pubkey);
        SendBSDFragment sendBottomSheetDialog = new SendBSDFragment();
        sendBottomSheetDialog.setArguments(intent.getExtras());
        return sendBottomSheetDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();
        mOnChain = args.getBoolean("onChain");
        mIsKeysend = args.getBoolean("keysend");

        if (mOnChain) {
            mFixedAmount = args.getLong("onChainAmount");
            mOnChainAddress = args.getString("onChainAddress");
            mMemo = args.getString("onChainMessage");
        } else {
            if (mIsKeysend) {
                mKeysendPubkey = args.getString("keysendPubkey");
            } else {
                mDecodedBolt11 = (DecodedBolt11) args.getSerializable("lnPaymentRequest");
                mFallbackOnChainInvoice = args.getString("fallbackOnChainInvoice");
            }
        }

        View view = inflater.inflate(R.layout.bsd_send, container);
        mRootView = view;

        mBSDScrollableMainView = view.findViewById(R.id.scrollableBottomSheet);
        mProgressScreen = view.findViewById(R.id.paymentProgressLayout);
        mResultView = view.findViewById(R.id.resultLayout);
        mContentTopLayout = view.findViewById(R.id.contentTopLayout);
        mInputLayout = view.findViewById(R.id.inputLayout);
        mEtAmount = view.findViewById(R.id.sendAmount);
        mTvUnit = view.findViewById(R.id.sendUnit);
        mExtvMemo = view.findViewById(R.id.sendMemo);
        mOnChainFeeView = view.findViewById(R.id.sendFeeOnChainLayout);
        mLightningFeeView = view.findViewById(R.id.sendFeeLightningLayout);
        mNumpad = view.findViewById(R.id.numpadView);
        mBtnSend = view.findViewById(R.id.sendButton);
        mFallbackButton = view.findViewById(R.id.fallbackButton);
        mPayee = view.findViewById(R.id.sendPayee);
        mPcvComment = view.findViewById(R.id.paymentComment);

        mBSDScrollableMainView.setOnCloseListener(this::dismiss);
        mBSDScrollableMainView.setTitleIconVisibility(true);
        mResultView.setOnOkListener(this::dismiss);

        mHandler = new Handler();

        mNumpad.bindEditText(mEtAmount);

        // deactivate default keyboard for number input.
        mEtAmount.setShowSoftInputOnFocus(false);

        // set unit to current primary unit
        mTvUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());

        // Input validation for the amount field.
        mEtAmount.addTextChangedListener(new TextWatcher() {

            @Override
            public void afterTextChanged(Editable arg0) {

                // remove the last inputted character if not valid
                if (!mAmountValid) {
                    mNumpad.removeOneDigit();
                }

                if (!mEtAmount.getText().toString().equals(".")) {
                    // make text red if input is too large
                    long maxSendableMsat;
                    if (mOnChain) {
                        maxSendableMsat = Wallet_Balance.getInstance().getBalances().onChainConfirmed() * 1000L;
                    } else {
                        maxSendableMsat = WalletUtil.getMaxLightningSendAmount();
                    }

                    if (mSendAmountMsat > maxSendableMsat) {
                        if (mOnChain) {
                            if (mSendAmountMsat < Wallet_Balance.getInstance().getBalances().onChainTotal() * 1000L) {
                                String message = getResources().getString(R.string.error_funds_not_confirmed_yet);
                                showError(message, 10000);
                            } else {
                                String message = getResources().getString(R.string.error_insufficient_on_chain_funds) + " " + MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(Wallet_Balance.getInstance().getBalances().onChainTotal(), false);
                                showError(message, 4000);
                            }
                        } else {
                            String message = getResources().getString(R.string.error_insufficient_lightning_sending_liquidity) + " " + MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(maxSendableMsat, true);
                            showError(message, 6000);
                        }
                        mEtAmount.setTextColor(getResources().getColor(R.color.red));
                        mSendButtonEnabled_input = false;
                    } else {
                        mEtAmount.setTextColor(getResources().getColor(R.color.white));
                        mSendButtonEnabled_input = true;
                    }
                    if (mSendAmountMsat == 0 && mFixedAmount == 0L) {
                        mSendButtonEnabled_input = false;
                    }
                    updateSendButtonState();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1,
                                          int arg2, int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onTextChanged(CharSequence arg0, int start, int before,
                                      int count) {
                if (arg0.length() == 0) {
                    // No entered text so will show hint
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                } else {
                    mEtAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
                }

                if (mBlockOnInputChanged)
                    return;

                // validate input
                mAmountValid = MonetaryUtil.getInstance().validateCurrencyInput(arg0.toString(), !mOnChain);

                if (mAmountValid) {
                    mSendAmountMsat = MonetaryUtil.getInstance().convertPrimaryTextInputToMsat(arg0.toString());

                    // calculate fees
                    if (mIsKeysend || (!mOnChain && mDecodedBolt11.hasNoAmountSpecified())) {
                        mSendButtonEnabled_feeCalculate = false;
                        mFeeCaclulationDebounceHandler.attempt(new Runnable() {
                            @Override
                            public void run() {
                                calculateFee();
                            }
                        }, 650);
                    } else {
                        calculateFee();
                    }
                } else {
                    setFeeFailure();
                }
            }
        });


        if (mOnChain) {
            mPayee.setText(mOnChainAddress);
            mOnChainFeeView.initialSetup();

            mOnChainFeeView.setVisibility(View.VISIBLE);
            mOnChainFeeView.setFeeTierChangedListener(onChainFeeTier -> calculateFee());
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_on_chain);
            mResultView.setTypeIcon(R.drawable.ic_onchain_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_onchain_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_onChainPayment);
            mPcvComment.setVisibility(View.GONE);

            if (mMemo == null) {
                mExtvMemo.setVisibility(View.GONE);
            } else {
                mExtvMemo.setVisibility(View.VISIBLE);
                mExtvMemo.setContent(R.string.memo, mMemo);
            }

            if (mFixedAmount != 0L) {
                // A specific amount was requested. We are not allowed to change the amount.
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToPrimaryTextInputString(mFixedAmount, !mOnChain));
                mEtAmount.clearFocus();
                mEtAmount.setFocusable(false);
                mEtAmount.setEnabled(false);
            } else {
                // No specific amount was requested. Let User input an amount.
                mNumpad.setVisibility(View.VISIBLE);
                mSendButtonEnabled_input = false;
                updateSendButtonState();
                setFeeFailure();

                mHandler.postDelayed(() -> {
                    // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                    mEtAmount.requestFocus();
                }, 600);
            }

            // Action when clicked on "Send payment"
            mBtnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    BBLog.d(LOG_TAG, "Trying to send on-chain payment...");
                    // Send on-chain payment

                    long sendAmount = 0L;
                    if (mFixedAmount != 0L) {
                        sendAmount = mFixedAmount;
                    } else {
                        sendAmount = mSendAmountMsat;
                    }

                    if (sendAmount != 0L) {

                        mResultView.setDetailsText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(sendAmount, false));

                        switchToSendProgressScreen();

                        SendCoinsRequest sendRequest = SendCoinsRequest.newBuilder()
                                .setAddr(mOnChainAddress)
                                .setAmount(sendAmount / 1000)
                                .setTargetConf(mOnChainFeeView.getFeeTier().getConfirmationBlockTarget())
                                .build();

                        getCompositeDisposable().add(LndConnection.getInstance().getLightningService().sendCoins(sendRequest)
                                .subscribe(sendCoinsResponse -> {
                                    // updated the history, so it is shown the next time the user views it
                                    Wallet_TransactionHistory.getInstance().updateOnChainTransactionHistory();

                                    BBLog.v(LOG_TAG, sendCoinsResponse.toString());

                                    // show success animation
                                    mHandler.postDelayed(() -> switchToSuccessScreen(), 500);
                                }, throwable -> {
                                    BBLog.e(LOG_TAG, "Exception in send coins request task.");
                                    BBLog.e(LOG_TAG, throwable.getMessage());

                                    String errorPrefix = getResources().getString(R.string.error).toUpperCase() + ":";
                                    String errormessage = throwable.getMessage().replace("UNKNOWN:", errorPrefix);
                                    mHandler.postDelayed(() -> switchToFailedScreen(errormessage), 300);
                                }));
                    } else {
                        // Send amount == 0
                        Toast.makeText(getActivity(), "Send amount is to small.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {

            // Lightning Payment

            mLightningFeeView.setVisibility(View.VISIBLE);
            mBSDScrollableMainView.setTitleIcon(R.drawable.ic_icon_modal_lightning);
            mResultView.setTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mProgressScreen.setProgressTypeIcon(R.drawable.ic_nav_wallet_black_24dp);
            mBSDScrollableMainView.setTitle(R.string.send_lightningPayment);

            if (mIsKeysend) {
                mPayee.setText(ContactsManager.getInstance().getNameByContactData(mKeysendPubkey));
                mPcvComment.setupCharLimit(200);
                mPcvComment.setVisibility(View.VISIBLE);
            } else {
                mPayee.setText(ContactsManager.getInstance().getNameByContactData(mDecodedBolt11.getDestinationPubKey()));
                mPcvComment.setVisibility(View.GONE);
            }

            // Scroll to comment when focused
            mPcvComment.setOnFocusChangedListener(new PaymentCommentView.onCommentFocusChangedListener() {
                @Override
                public void onFocusChanged(View view, boolean b) {
                    if (b)
                        mBSDScrollableMainView.focusOnView(mPcvComment, 0);
                }
            });

            if (mIsKeysend || mDecodedBolt11.hasNoDescription()) {
                mExtvMemo.setVisibility(View.GONE);
            } else {
                mExtvMemo.setVisibility(View.VISIBLE);
                mExtvMemo.setContent(R.string.memo, mDecodedBolt11.getDescription());
            }

            if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
                // No specific amount was requested. Let User input an amount.
                mNumpad.setVisibility(View.VISIBLE);
                mSendButtonEnabled_input = false;
                updateSendButtonState();

                mHandler.postDelayed(() -> {
                    // We have to call this delayed, as otherwise it will still bring up the softKeyboard
                    mEtAmount.requestFocus();
                }, 600);
            } else {
                // A specific amount was requested. We are not allowed to change the amount
                mFixedAmount = mDecodedBolt11.getAmountRequested();
                mEtAmount.setText(MonetaryUtil.getInstance().msatsToPrimaryTextInputString(mFixedAmount, !mOnChain));
                mEtAmount.clearFocus();
                mEtAmount.setFocusable(false);
            }


            // Action when clicked on "Send payment"
            mBtnSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {

                        long paymentAmount = getLightningPaymentAmountMSat();

                        // sanity check for fees
                        if (mCalculatedFee != -1) {
                            if (mCalculatedFeePercent >= 1f) {
                                // fee higher or equal to payment amount
                                String feeLimitString = getString(R.string.fee_limit_exceeded_payment, mCalculatedFee, paymentAmount);
                                showFeeAlertDialog(feeLimitString);
                                return;
                            }
                            if (paymentAmount > RefConstants.LN_PAYMENT_FEE_THRESHOLD)
                                if (mCalculatedFeePercent > PaymentUtil.getRelativeSettingsFeeLimit()) {
                                    // fee higher than allowed in settings
                                    String feeLimitString = getString(R.string.fee_limit_exceeded, mCalculatedFeePercent * 100, PaymentUtil.getRelativeSettingsFeeLimit() * 100);
                                    showFeeAlertDialog(feeLimitString);
                                    return;
                                }
                        }
                        sendLightningPayment();
                    } else {
                        // Demo Mode
                        Toast.makeText(getActivity(), R.string.demo_setupNodeFirst, Toast.LENGTH_SHORT).show();
                    }

                }
            });
        }


        // Action when clicked on receive unit
        LinearLayout llUnit = view.findViewById(R.id.sendUnitLayout);
        llUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBlockOnInputChanged = true;
                MonetaryUtil.getInstance().switchCurrencies();
                if (mFixedAmount == 0L) {
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToPrimaryTextInputString(mSendAmountMsat, !mOnChain));
                } else {
                    mEtAmount.setText(MonetaryUtil.getInstance().msatsToPrimaryTextInputString(mFixedAmount, !mOnChain));
                }
                mTvUnit.setText(MonetaryUtil.getInstance().getPrimaryDisplayUnit());
                mBlockOnInputChanged = false;
            }
        });

        mFallbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HomeActivity) getActivity()).analyzeString(mFallbackOnChainInvoice);
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacksAndMessages(null);

        super.onDestroyView();
    }

    private void showFeeAlertDialog(String message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(getContext())
                .setTitle(R.string.fee_limit_title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> sendLightningPayment())
                .setNegativeButton(R.string.no, (dialog, whichButton) -> {
                });
        Dialog dlg = adb.create();
        // Apply FLAG_SECURE to dialog to prevent screen recording
        if (PrefsUtil.isScreenRecordingPrevented()) {
            dlg.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        dlg.show();
    }

    private void sendLightningPayment() {
        switchToSendProgressScreen();

        if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
            mResultView.setDetailsText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(mSendAmountMsat, true));
        } else {
            mResultView.setDetailsText(MonetaryUtil.getInstance().getPrimaryDisplayStringFromMSats(mDecodedBolt11.getAmountRequested(), true));
        }

        SendLnPaymentRequest sendLnPaymentRequest = null;
        if (mIsKeysend)
            sendLnPaymentRequest = PaymentUtil.prepareKeysendPayment(mKeysendPubkey, mSendAmountMsat, mPcvComment.getData());
        else {
            long amount = mDecodedBolt11.hasAmountSpecified() ? mDecodedBolt11.getAmountRequested() : mSendAmountMsat;
            sendLnPaymentRequest = PaymentUtil.prepareBolt11InvoicePayment(mDecodedBolt11, amount);
        }

        PaymentUtil.sendLnPayment(sendLnPaymentRequest, getCompositeDisposable(), new PaymentUtil.OnPaymentResult() {
            @Override
            public void onSuccess(SendLnPaymentResponse sendLnPaymentResponse) {
                mHandler.postDelayed(() -> switchToSuccessScreen(), 300);
            }

            @SuppressLint("StringFormatInvalid")
            @Override
            public void onError(String error, SendLnPaymentResponse sendLnPaymentResponse, int duration) {
                String errorPrefix = getResources().getString(R.string.error).toUpperCase() + ":";
                String errorMessage;
                switch (sendLnPaymentResponse.getFailureReason()) {
                    case TIMEOUT:
                        errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_timeout);
                        break;
                    case NO_ROUTE:
                        if (sendLnPaymentResponse.getAmount() > 0 && sendLnPaymentResponse.getAmount() < 1000L)
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_no_route_small_amount);
                        else
                            errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_no_route, PaymentUtil.getRelativeSettingsFeeLimit() * 100);
                        break;
                    case INSUFFICIENT_FUNDS:
                        errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_insufficient_balance);
                        break;
                    case INCORRECT_PAYMENT_DETAILS:
                        errorMessage = errorPrefix + "\n\n" + getResources().getString(R.string.error_payment_keysend_not_enabled_on_remote);
                        break;
                    default:
                        errorMessage = errorPrefix + "\n\n" + error;
                        break;
                }
                mHandler.postDelayed(() -> switchToFailedScreen(errorMessage), 300);
            }
        });
    }

    private void switchToSendProgressScreen() {
        mProgressScreen.setVisibility(View.VISIBLE);
        mInputLayout.setVisibility(View.INVISIBLE);
        mProgressScreen.startSpinning();
        mBSDScrollableMainView.animateTitleOut();
    }

    private void switchToSuccessScreen() {
        mProgressScreen.spinningFinished(true);
        TransitionManager.beginDelayedTransition((ViewGroup) mContentTopLayout.getRootView());
        mInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);
        mResultView.setHeading(R.string.send_success, true);
    }

    private void switchToFailedScreen(String error) {
        mProgressScreen.spinningFinished(false);
        TransitionManager.beginDelayedTransition(mContentTopLayout);
        mInputLayout.setVisibility(View.GONE);
        mResultView.setVisibility(View.VISIBLE);

        // Set failed states
        mResultView.setHeading(R.string.send_fail, false);
        mResultView.setDetailsText(error);

        if (!mOnChain && mFallbackOnChainInvoice != null) {
            mFallbackButton.setVisibility(View.VISIBLE);
        }
    }

    private void calculateFee() {
        if (BackendConfigsManager.getInstance().hasAnyBackendConfigs()) {
            setCalculatingFee();

            if (mOnChain) {
                long sendAmountSat = 0L;
                if (mFixedAmount != 0L) {
                    sendAmountSat = mFixedAmount / 1000L;
                } else {
                    sendAmountSat = mSendAmountMsat / 1000L;
                }

                estimateOnChainFee(mOnChainAddress, sendAmountSat, mOnChainFeeView.getFeeTier().getConfirmationBlockTarget());
            } else {
                setFeeFailure();
            }
        } else {
            setFeeFailure();
        }
    }

    /**
     * Show progress while calculating fee
     */
    private void setCalculatingFee() {
        mSendButtonEnabled_feeCalculate = false;
        updateSendButtonState();
        if (mOnChain) {
            mOnChainFeeView.onCalculating();
        } else {
            mLightningFeeView.onCalculating();
        }
    }

    /**
     * Show the calculated fee
     */
    private void setCalculatedFeeAmountOnChain(long sats) {
        mSendButtonEnabled_feeCalculate = true;
        updateSendButtonState();
        mOnChainFeeView.onFeeSuccess(sats);
    }

    private void setCalculatedFeeAmountLightning(long sats, String percentString, boolean showMax) {
        mSendButtonEnabled_feeCalculate = true;
        updateSendButtonState();
        mLightningFeeView.setAmountMsat(sats, percentString, showMax);
    }

    /**
     * Show fee calculation failure
     */
    private void setFeeFailure() {
        mSendButtonEnabled_feeCalculate = true;
        updateSendButtonState();
        if (mOnChain) {
            mOnChainFeeView.onFeeFailure();
        } else {
            mLightningFeeView.onFeeFailure();
        }
    }

    private void updateSendButtonState() {
        if (mSendButtonEnabled_feeCalculate && mSendButtonEnabled_input) {
            mBtnSend.setEnabled(true);
            mBtnSend.setTextColor(getResources().getColor(R.color.banana_yellow));
        } else {
            mBtnSend.setEnabled(false);
            mBtnSend.setTextColor(getResources().getColor(R.color.gray));
        }
    }

    /**
     * This function is used to calculate the expected on chain fee.
     */
    private void estimateOnChainFee(String address, long amount, int targetConf) {
        // let LND estimate fee
        EstimateFeeRequest asyncEstimateFeeRequest = EstimateFeeRequest.newBuilder()
                .putAddrToAmount(address, amount)
                .setTargetConf(targetConf)
                .build();

        getCompositeDisposable().add(LndConnection.getInstance().getLightningService().estimateFee(asyncEstimateFeeRequest)
                .subscribe(estimateFeeResponse -> setCalculatedFeeAmountOnChain(estimateFeeResponse.getFeeSat()),
                        throwable -> {
                            BBLog.w(LOG_TAG, "Exception in fee estimation request task.");
                            BBLog.w(LOG_TAG, throwable.getMessage());
                            setFeeFailure();
                        }));
    }

    private long getLightningPaymentAmountMSat() {
        if (mIsKeysend || mDecodedBolt11.hasNoAmountSpecified()) {
            return mSendAmountMsat;
        } else {
            return mDecodedBolt11.getAmountRequested();
        }
    }

    private void showError(String message, int duration) {
        Snackbar msg = Snackbar.make(mRootView.findViewById(R.id.coordinator), message, duration);
        View sbView = msg.getView();
        sbView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.red));
        msg.show();
    }
}