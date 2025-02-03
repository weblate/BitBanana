package app.michaelwuensch.bitbanana.backends.coreLightning;

import com.github.ElementsProject.lightning.cln.Amount;
import com.github.ElementsProject.lightning.cln.AmountOrAll;
import com.github.ElementsProject.lightning.cln.AmountOrAny;
import com.github.ElementsProject.lightning.cln.BkprlistincomeIncome_events;
import com.github.ElementsProject.lightning.cln.BkprlistincomeRequest;
import com.github.ElementsProject.lightning.cln.BkprlistincomeResponse;
import com.github.ElementsProject.lightning.cln.ChannelSide;
import com.github.ElementsProject.lightning.cln.CheckmessageRequest;
import com.github.ElementsProject.lightning.cln.CloseRequest;
import com.github.ElementsProject.lightning.cln.ConnectRequest;
import com.github.ElementsProject.lightning.cln.DisableofferRequest;
import com.github.ElementsProject.lightning.cln.DisconnectRequest;
import com.github.ElementsProject.lightning.cln.EnableofferRequest;
import com.github.ElementsProject.lightning.cln.Feerate;
import com.github.ElementsProject.lightning.cln.FetchinvoiceRequest;
import com.github.ElementsProject.lightning.cln.FundchannelRequest;
import com.github.ElementsProject.lightning.cln.GetinfoRequest;
import com.github.ElementsProject.lightning.cln.InvoiceRequest;
import com.github.ElementsProject.lightning.cln.KeysendRequest;
import com.github.ElementsProject.lightning.cln.ListchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsClosedchannels;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListclosedchannelsResponse;
import com.github.ElementsProject.lightning.cln.ListforwardsForwards;
import com.github.ElementsProject.lightning.cln.ListforwardsRequest;
import com.github.ElementsProject.lightning.cln.ListfundsChannels;
import com.github.ElementsProject.lightning.cln.ListfundsOutputs;
import com.github.ElementsProject.lightning.cln.ListfundsRequest;
import com.github.ElementsProject.lightning.cln.ListinvoicesInvoices;
import com.github.ElementsProject.lightning.cln.ListinvoicesRequest;
import com.github.ElementsProject.lightning.cln.ListnodesNodesAddresses;
import com.github.ElementsProject.lightning.cln.ListnodesRequest;
import com.github.ElementsProject.lightning.cln.ListoffersOffers;
import com.github.ElementsProject.lightning.cln.ListoffersRequest;
import com.github.ElementsProject.lightning.cln.ListpaysPays;
import com.github.ElementsProject.lightning.cln.ListpaysRequest;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsChannels;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsRequest;
import com.github.ElementsProject.lightning.cln.ListpeerchannelsResponse;
import com.github.ElementsProject.lightning.cln.ListpeersPeers;
import com.github.ElementsProject.lightning.cln.ListpeersPeersLog;
import com.github.ElementsProject.lightning.cln.ListpeersRequest;
import com.github.ElementsProject.lightning.cln.ListtransactionsRequest;
import com.github.ElementsProject.lightning.cln.ListtransactionsResponse;
import com.github.ElementsProject.lightning.cln.ListtransactionsTransactions;
import com.github.ElementsProject.lightning.cln.NewaddrRequest;
import com.github.ElementsProject.lightning.cln.OfferRequest;
import com.github.ElementsProject.lightning.cln.PayRequest;
import com.github.ElementsProject.lightning.cln.SetchannelRequest;
import com.github.ElementsProject.lightning.cln.SignmessageRequest;
import com.github.ElementsProject.lightning.cln.TlvEntry;
import com.github.ElementsProject.lightning.cln.TlvStream;
import com.github.ElementsProject.lightning.cln.WithdrawRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.backendConfigs.BackendConfig;
import app.michaelwuensch.bitbanana.backends.Api;
import app.michaelwuensch.bitbanana.backends.coreLightning.connection.CoreLightningConnection;
import app.michaelwuensch.bitbanana.backends.coreLightning.services.CoreLightningNodeService;
import app.michaelwuensch.bitbanana.baseClasses.App;
import app.michaelwuensch.bitbanana.connection.tor.TorManager;
import app.michaelwuensch.bitbanana.models.Balances;
import app.michaelwuensch.bitbanana.models.Bolt12Offer;
import app.michaelwuensch.bitbanana.models.Channels.ChannelConstraints;
import app.michaelwuensch.bitbanana.models.Channels.CloseChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.ClosedChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannel;
import app.michaelwuensch.bitbanana.models.Channels.OpenChannelRequest;
import app.michaelwuensch.bitbanana.models.Channels.PendingChannel;
import app.michaelwuensch.bitbanana.models.Channels.PublicChannelInfo;
import app.michaelwuensch.bitbanana.models.Channels.RoutingPolicy;
import app.michaelwuensch.bitbanana.models.Channels.ShortChannelId;
import app.michaelwuensch.bitbanana.models.Channels.UpdateRoutingPolicyRequest;
import app.michaelwuensch.bitbanana.models.CreateBolt12OfferRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceRequest;
import app.michaelwuensch.bitbanana.models.CreateInvoiceResponse;
import app.michaelwuensch.bitbanana.models.CurrentNodeInfo;
import app.michaelwuensch.bitbanana.models.CustomRecord;
import app.michaelwuensch.bitbanana.models.FetchInvoiceFromOfferRequest;
import app.michaelwuensch.bitbanana.models.Forward;
import app.michaelwuensch.bitbanana.models.LightningNodeUri;
import app.michaelwuensch.bitbanana.models.LnInvoice;
import app.michaelwuensch.bitbanana.models.LnPayment;
import app.michaelwuensch.bitbanana.models.NewOnChainAddressRequest;
import app.michaelwuensch.bitbanana.models.NodeInfo;
import app.michaelwuensch.bitbanana.models.OnChainTransaction;
import app.michaelwuensch.bitbanana.models.Outpoint;
import app.michaelwuensch.bitbanana.models.PagedResponse;
import app.michaelwuensch.bitbanana.models.Peer;
import app.michaelwuensch.bitbanana.models.SendLnPaymentRequest;
import app.michaelwuensch.bitbanana.models.SendLnPaymentResponse;
import app.michaelwuensch.bitbanana.models.SendOnChainPaymentRequest;
import app.michaelwuensch.bitbanana.models.SignMessageResponse;
import app.michaelwuensch.bitbanana.models.TimestampedMessage;
import app.michaelwuensch.bitbanana.models.Utxo;
import app.michaelwuensch.bitbanana.models.VerifyMessageResponse;
import app.michaelwuensch.bitbanana.util.ApiUtil;
import app.michaelwuensch.bitbanana.util.BBLog;
import app.michaelwuensch.bitbanana.util.InvoiceUtil;
import app.michaelwuensch.bitbanana.util.LightningNodeUriParser;
import app.michaelwuensch.bitbanana.util.PaymentUtil;
import app.michaelwuensch.bitbanana.util.PrefsUtil;
import app.michaelwuensch.bitbanana.util.RefConstants;
import app.michaelwuensch.bitbanana.util.UtilFunctions;
import app.michaelwuensch.bitbanana.util.Version;
import app.michaelwuensch.bitbanana.wallet.Wallet;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * Class that translates BitBanana backend interactions into Core Lightning API specific interactions.
 * <p>
 * You can find the CoreLightning API documentation here:
 * https://docs.corelightning.org/reference/
 */
public class CoreLightningApi extends Api {
    private static final String LOG_TAG = CoreLightningApi.class.getSimpleName();

    public CoreLightningApi() {

    }

    private CoreLightningNodeService CoreLightningNodeService() {
        return CoreLightningConnection.getInstance().getCoreLightningNodeService();
    }

    @Override
    public Single<CurrentNodeInfo> getCurrentNodeInfo() {
        return CoreLightningNodeService().getinfo(GetinfoRequest.newBuilder().build())
                .map(response -> {
                    String pubkey = ApiUtil.StringFromHexByteString(response.getId());
                    LightningNodeUri[] lnUris;
                    if (response.getAddressCount() != 0) {
                        lnUris = new LightningNodeUri[response.getAddressCount()];
                        for (int i = 0; i < response.getAddressCount(); i++) {
                            lnUris[i] = LightningNodeUriParser.parseNodeUri(response.getAddress(i).getAddress());
                        }
                    } else {
                        lnUris = new LightningNodeUri[1];
                        lnUris[0] = LightningNodeUriParser.parseNodeUri(pubkey);
                    }
                    return CurrentNodeInfo.newBuilder()
                            .setAlias(String.valueOf(response.getAlias()))
                            .setVersion(new Version(response.getVersion().replace("v", "").split("-")[0]))
                            .setFullVersionString(response.getVersion())
                            .setPubKey(pubkey)
                            .setBlockHeight(response.getBlockheight())
                            .setLightningNodeUris(lnUris)
                            .setNetwork(BackendConfig.Network.parseFromString(response.getNetwork().replace("bitcoin", "mainnet")))  // mainnet is called "bitcoin" on cln
                            .setSynced(!(response.hasWarningBitcoindSync() || response.hasWarningLightningdSync()))
                            .setAvatarMaterial(pubkey)
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "CoreLightning getInfo failed! " + throwable.toString()));
    }

    @Override
    public Single<NodeInfo> getNodeInfo(String pubKey) {
        ListnodesRequest listnodesRequest = ListnodesRequest.newBuilder()
                .setId(ApiUtil.ByteStringFromHexString(pubKey))
                .build();

        return CoreLightningNodeService().listNodes(listnodesRequest)
                .map(response -> {
                    List<String> addresses = new ArrayList<>();
                    for (ListnodesNodesAddresses address : response.getNodes(0).getAddressesList())
                        addresses.add(address.getAddress());
                    return NodeInfo.newBuilder()
                            .setPubKey(ApiUtil.StringFromHexByteString(response.getNodes(0).getNodeid()))
                            .setAlias(response.getNodes(0).getAlias())
                            .setAddresses(addresses)
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "CoreLightning getNodeInfo failed! " + throwable.toString()));
    }

    @Override
    public Single<Balances> getBalances() {
        return CoreLightningNodeService().listFunds(ListfundsRequest.newBuilder().build())
                .map(response -> {
                    long onChainConfirmed = 0;
                    long onChainUnconfirmed = 0;
                    long channelBalance = 0;
                    long channelBalancePending = 0;
                    for (ListfundsOutputs output : response.getOutputsList()) {
                        switch (output.getStatus()) {
                            case CONFIRMED:
                                if (!output.getReserved())
                                    onChainConfirmed = onChainConfirmed + output.getAmountMsat().getMsat();
                                break;
                            case UNCONFIRMED:
                                onChainUnconfirmed = onChainUnconfirmed + output.getAmountMsat().getMsat();
                                break;
                        }
                    }
                    for (ListfundsChannels channel : response.getChannelsList()) {
                        switch (channel.getState()) {
                            case ChanneldNormal:
                                channelBalance = channelBalance + channel.getOurAmountMsat().getMsat();
                                break;
                            case ChanneldAwaitingLockin:
                            case DualopendAwaitingLockin:
                                channelBalancePending = channelBalancePending + channel.getOurAmountMsat().getMsat();
                                break;
                        }
                    }
                    Balances balances = Balances.newBuilder()
                            .setOnChainConfirmed(onChainConfirmed)
                            .setOnChainUnconfirmed(onChainUnconfirmed)
                            .setChannelBalance(channelBalance)
                            .setChannelBalancePendingOpen(channelBalancePending)
                            .build();
                    return balances;
                });
    }

    @Override
    public Single<SignMessageResponse> signMessageWithNode(String message) {
        SignmessageRequest signMessageRequest = SignmessageRequest.newBuilder()
                .setMessage(message)
                .build();

        return CoreLightningNodeService().signMessage(signMessageRequest)
                .map(response -> {
                    return SignMessageResponse.newBuilder()
                            .setSignature(ApiUtil.StringFromHexByteString(response.getSignature()))
                            .setRecId(ApiUtil.StringFromHexByteString(response.getRecid()))
                            .setZBase(response.getZbase())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Sign message failed: " + throwable.fillInStackTrace()));
    }

    public Single<VerifyMessageResponse> verifyMessageWithNode(String message, String signature) {
        CheckmessageRequest checkMessageRequest = CheckmessageRequest.newBuilder()
                .setMessage(message)
                .setZbase(signature)
                .build();

        return CoreLightningNodeService().checkMessage(checkMessageRequest)
                .map(response -> {
                    return VerifyMessageResponse.newBuilder()
                            .setIsValid(response.getVerified())
                            .setPubKey(ApiUtil.StringFromHexByteString(response.getPubkey()))
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Verify message failed: " + throwable.fillInStackTrace()));
    }

    public Single<List<Utxo>> listUTXOs(long currentBlockHeight) {
        return CoreLightningNodeService().listFunds(ListfundsRequest.newBuilder().build())
                .map(response -> {
                    List<Utxo> utxoList = new ArrayList<>();
                    for (ListfundsOutputs output : response.getOutputsList()) {
                        if (output.getStatus() == ListfundsOutputs.ListfundsOutputsStatus.CONFIRMED || output.getStatus() == ListfundsOutputs.ListfundsOutputsStatus.UNCONFIRMED) {
                            Utxo.Builder builder = Utxo.newBuilder()
                                    .setAddress(output.getAddress())
                                    .setAmount(output.getAmountMsat().getMsat())
                                    .setOutpoint(Outpoint.newBuilder()
                                            .setTransactionID(ApiUtil.StringFromHexByteString(output.getTxid()))
                                            .setOutputIndex(output.getOutput())
                                            .build())
                                    .setBlockHeight(output.getBlockheight());
                            if (output.getBlockheight() == 0) {
                                builder.setConfirmations(0);
                            } else {
                                builder.setConfirmations(currentBlockHeight - output.getBlockheight() + 1);
                            }
                            utxoList.add(builder.build());
                        }
                    }

                    return utxoList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching utxo list failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<OpenChannel>> listOpenChannels() {
        return CoreLightningNodeService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build())
                .map(response -> {
                    List<OpenChannel> openChannelsList = new ArrayList<>();
                    for (ListpeerchannelsChannels channel : response.getChannelsList())
                        if (channel.getState() == ListpeerchannelsChannels.ListpeerchannelsChannelsState.CHANNELD_NORMAL)
                            openChannelsList.add(OpenChannel.newBuilder()
                                    .setActive(channel.getPeerConnected())
                                    .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                                    .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                                    //.setChannelType(???)
                                    .setInitiator(channel.getOpener() == ChannelSide.LOCAL)
                                    .setPrivate(channel.getPrivate())
                                    .setCapacity(channel.getTotalMsat().getMsat())
                                    .setLocalBalance(channel.getToUsMsat().getMsat())
                                    .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                                    .setLocalChannelConstraints(ChannelConstraints.newBuilder()
                                            .setSelfDelay(channel.getOurToSelfDelay())
                                            .setChannelReserve(channel.getOurReserveMsat().getMsat())
                                            .build())
                                    .setRemoteChannelConstraints(ChannelConstraints.newBuilder()
                                            .setSelfDelay(channel.getTheirToSelfDelay())
                                            .setChannelReserve(channel.getTheirReserveMsat().getMsat())
                                            .build())
                                    .setFundingOutpoint(Outpoint.newBuilder()
                                            .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                            .setOutputIndex(channel.getFundingOutnum())
                                            .build())
                                    //.setCommitFee(???)
                                    .setTotalSent(channel.getOutFulfilledMsat().getMsat())
                                    .setTotalReceived(channel.getInFulfilledMsat().getMsat())
                                    .build());
                    return openChannelsList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List open channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<PendingChannel>> listPendingChannels() {
        return CoreLightningNodeService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build())
                .map(response -> {
                            List<PendingChannel> pendingChannelsList = new ArrayList<>();
                            for (ListpeerchannelsChannels channel : response.getChannelsList())
                                if (channel.getState() != ListpeerchannelsChannels.ListpeerchannelsChannelsState.CHANNELD_NORMAL
                                        && channel.getState() != ListpeerchannelsChannels.ListpeerchannelsChannelsState.ONCHAIN) {
                                    PendingChannel.PendingType pendingType;
                                    switch (channel.getState()) {
                                        case OPENINGD:
                                        case CHANNELD_AWAITING_LOCKIN:
                                        case DUALOPEND_OPEN_INIT:
                                        case DUALOPEND_OPEN_COMMITTED:
                                        case DUALOPEND_AWAITING_LOCKIN:
                                            pendingType = PendingChannel.PendingType.PENDING_OPEN;
                                            break;
                                        case CHANNELD_SHUTTING_DOWN:
                                        case CLOSINGD_SIGEXCHANGE:
                                        case CLOSINGD_COMPLETE:
                                            pendingType = PendingChannel.PendingType.PENDING_CLOSE;
                                            break;
                                        case AWAITING_UNILATERAL:
                                        case FUNDING_SPEND_SEEN:
                                            pendingType = PendingChannel.PendingType.PENDING_FORCE_CLOSE;
                                            break;
                                        default:
                                            pendingType = PendingChannel.PendingType.UNKNOWN;
                                    }
                                    pendingChannelsList.add(PendingChannel.newBuilder()
                                            .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                                            //.setShortChannelId() NEVER AVAILABLE AT THIS STATE
                                            //.setChannelType(???)
                                            .setPendingType(pendingType)
                                            .setInitiator(channel.getOpener() == ChannelSide.LOCAL)
                                            .setPrivate(channel.getPrivate())
                                            .setCapacity(channel.getTotalMsat().getMsat())
                                            .setLocalBalance(channel.getToUsMsat().getMsat())
                                            .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                                            .setFundingOutpoint(Outpoint.newBuilder()
                                                    .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                                    .setOutputIndex(channel.getFundingOutnum())
                                                    .build())
                                            //.setCloseTransactionId(???)
                                            //.setBlocksTilMaturity(???)
                                            //.setCommitFee(???)
                                            .setTotalSent(channel.getOutFulfilledMsat().getMsat())
                                            .setTotalReceived(channel.getInFulfilledMsat().getMsat())
                                            .build());
                                }
                            return pendingChannelsList;
                        }
                )
                .doOnError(throwable -> BBLog.w(LOG_TAG, "List pending channels failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<ClosedChannel>> listClosedChannels() {
        // closed channels only returns channels that are closed for more than 100 blocks, therefore we also need to get channels from listpeerchannels with state ONCHAIN
        Single<ListclosedchannelsResponse> clnClosedChannelsList = CoreLightningNodeService().listClosedChannels(ListclosedchannelsRequest.newBuilder().build());
        Single<ListpeerchannelsResponse> clnPeerChannelsList = CoreLightningNodeService().listPeerChannels(ListpeerchannelsRequest.newBuilder().build());

        return Single.zip(clnClosedChannelsList, clnPeerChannelsList, (closedListResponse, peerListResponse) -> {
            List<ClosedChannel> closedChannelsList = new ArrayList<>();
            for (ListclosedchannelsClosedchannels channel : closedListResponse.getClosedchannelsList())
                closedChannelsList.add(ClosedChannel.newBuilder()
                        .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                        .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                        .setCloseTransactionId(ApiUtil.StringFromHexByteString(channel.getLastCommitmentTxid()))
                        //.setChannelType(???)
                        .setOpenInitiator(channel.getOpener() == ChannelSide.LOCAL)
                        .setCloseInitiator(channel.getCloser() == ChannelSide.LOCAL)
                        //.setCloseType(???)
                        //.setCloseHeight(???)
                        .setPrivate(channel.getPrivate())
                        .setCapacity(channel.getTotalMsat().getMsat())
                        .setLocalBalance(channel.getFinalToUsMsat().getMsat())
                        .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getFinalToUsMsat().getMsat())
                        .setFundingOutpoint(Outpoint.newBuilder()
                                .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                .setOutputIndex(channel.getFundingOutnum())
                                .build())
                        //.setSweepTransactionIds(???)
                        .build());
            for (ListpeerchannelsChannels channel : peerListResponse.getChannelsList())
                if (channel.getState() == ListpeerchannelsChannels.ListpeerchannelsChannelsState.ONCHAIN)
                    closedChannelsList.add(ClosedChannel.newBuilder()
                            .setRemotePubKey(ApiUtil.StringFromHexByteString(channel.getPeerId()))
                            .setShortChannelId(ApiUtil.ScidFromString(channel.getShortChannelId()))
                            //.setCloseTransactionId(ApiUtil.StringFromHexByteString(channel.getScratchTxid())) // correct ?
                            //.setChannelType(???)
                            .setOpenInitiator(channel.getOpener() == ChannelSide.LOCAL)
                            .setCloseInitiator(channel.getCloser() == ChannelSide.LOCAL)
                            //.setCloseType(???)
                            .setPrivate(channel.getPrivate())
                            .setCapacity(channel.getTotalMsat().getMsat())
                            .setLocalBalance(channel.getToUsMsat().getMsat())
                            .setRemoteBalance(channel.getTotalMsat().getMsat() - channel.getToUsMsat().getMsat())
                            .setFundingOutpoint(Outpoint.newBuilder()
                                    .setTransactionID(ApiUtil.StringFromHexByteString(channel.getFundingTxid()))
                                    .setOutputIndex(channel.getFundingOutnum())
                                    .build())
                            .build());
            return closedChannelsList;
        });
    }

    @Override
    public Single<PublicChannelInfo> getPublicChannelInfo(ShortChannelId shortChannelId) {
        ListchannelsRequest request = ListchannelsRequest.newBuilder()
                .setShortChannelId(shortChannelId.toString())
                .build();

        return CoreLightningNodeService().listChannels(request)
                .map(response -> {
                    return PublicChannelInfo.newBuilder()
                            .setShortChannelId(ApiUtil.ScidFromString(response.getChannels(0).getShortChannelId()))
                            //.setFundingOutpoint(???)
                            .setNode1PubKey(ApiUtil.StringFromHexByteString(response.getChannels(0).getSource()))
                            .setNode2PubKey(ApiUtil.StringFromHexByteString(response.getChannels(1).getSource()))
                            .setNode1RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getChannels(0).getBaseFeeMillisatoshi())
                                    .setFeeRate(response.getChannels(0).getFeePerMillionth())
                                    .setDelay(response.getChannels(0).getDelay())
                                    .setMinHTLC(response.getChannels(0).getHtlcMinimumMsat().getMsat())
                                    .setMaxHTLC(response.getChannels(0).getHtlcMaximumMsat().getMsat())
                                    .build())
                            .setNode2RoutingPolicy(RoutingPolicy.newBuilder()
                                    .setFeeBase(response.getChannels(1).getBaseFeeMillisatoshi())
                                    .setFeeRate(response.getChannels(1).getFeePerMillionth())
                                    .setDelay(response.getChannels(1).getDelay())
                                    .setMinHTLC(response.getChannels(1).getHtlcMinimumMsat().getMsat())
                                    .setMaxHTLC(response.getChannels(1).getHtlcMaximumMsat().getMsat())
                                    .build())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetch public channel info failed: " + throwable.fillInStackTrace()));
    }

    private LnInvoice getInvoiceFromCoreLightningInvoice(ListinvoicesInvoices invoice) {

        if (invoice.hasBolt11()) {
            long created_at = 0;
            if (invoice.getStatus() == ListinvoicesInvoices.ListinvoicesInvoicesStatus.PAID)
                created_at = invoice.getPaidAt();
            else {
                try {
                    created_at = InvoiceUtil.decodeBolt11(invoice.getBolt11()).getTimestamp();
                } catch (Exception e) {
                    created_at = System.currentTimeMillis() / 1000L;
                }
            }

            return LnInvoice.newBuilder()
                    .setType(LnInvoice.InvoiceType.BOLT11_INVOICE)
                    .setBolt11(invoice.getBolt11())
                    .setPaymentHash(ApiUtil.StringFromHexByteString(invoice.getPaymentHash()))
                    .setAmountRequested(invoice.getAmountMsat().getMsat())
                    .setAmountPaid(invoice.getAmountReceivedMsat().getMsat())
                    .setCreatedAt(created_at) // ToDo: Simplify once the api exposes created_at
                    .setPaidAt(invoice.getPaidAt())
                    .setExpiresAt(invoice.getExpiresAt())
                    .setAddIndex(invoice.getCreatedIndex())
                    .setMemo(invoice.getDescription())
                    //.setKeysendMessage(???)
                    .build();
        } else {
            return LnInvoice.newBuilder()
                    .setType(LnInvoice.InvoiceType.BOLT12_INVOICE)
                    .setBolt12(invoice.getBolt12())
                    .setPaymentHash(ApiUtil.StringFromHexByteString(invoice.getPaymentHash()))
                    .setAmountRequested(invoice.getAmountMsat().getMsat())
                    .setAmountPaid(invoice.getAmountReceivedMsat().getMsat())
                    .setCreatedAt(invoice.getPaidAt()) // Bolt12 invoices normally get paid immediately after they are created, so that is fine.
                    .setPaidAt(invoice.getPaidAt())
                    .setExpiresAt(invoice.getExpiresAt())
                    .setAddIndex(invoice.getCreatedIndex())
                    .setMemo(invoice.getDescription()) // description of bolt 12 offer
                    .setBolt12PayerNote(invoice.getInvreqPayerNote())
                    //.setKeysendMessage(???)
                    .build();
        }
    }

    private Single<PagedResponse<LnInvoice>> getInvoicesPage(long firstIndexOffset, int pageSize) {
        BBLog.d(LOG_TAG, "Fetching invoices page, offset:  " + (firstIndexOffset + 1));
        ListinvoicesRequest invoiceRequest = ListinvoicesRequest.newBuilder()
                .setIndex(ListinvoicesRequest.ListinvoicesIndex.CREATED)
                .setLimit(pageSize)
                .setStart(firstIndexOffset + 1) //index is one based on this call
                .build();

        return CoreLightningNodeService().listInvoices(invoiceRequest)
                .map(response -> {
                    long lastIndexOffset = firstIndexOffset;
                    List<LnInvoice> invoicesList = new ArrayList<>();
                    for (ListinvoicesInvoices invoice : response.getInvoicesList()) {
                        invoicesList.add(getInvoiceFromCoreLightningInvoice(invoice));
                        lastIndexOffset = invoice.getCreatedIndex();
                    }
                    PagedResponse<LnInvoice> page = PagedResponse.<LnInvoice>newBuilder()
                            .setPage(invoicesList)
                            .setPageSize(response.getInvoicesCount())
                            .setLastIndexOffset(lastIndexOffset)
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching Invoice page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnInvoice>> listInvoices(long firstIndexOffset, int pageSize) {
        return getInvoicesPage(firstIndexOffset, pageSize)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listInvoices(data.getLastIndexOffset(), pageSize)
                                .map(nextPageData -> {
                                    List<LnInvoice> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    @Override
    public Single<LnInvoice> getInvoice(String paymentHash) {
        ListinvoicesRequest invoiceRequest = ListinvoicesRequest.newBuilder()
                .setPaymentHash(ApiUtil.ByteStringFromHexString(paymentHash))
                .build();

        return CoreLightningNodeService().listInvoices(invoiceRequest)
                .map(response -> {
                    if (response.getInvoicesCount() == 1)
                        return getInvoiceFromCoreLightningInvoice(response.getInvoices(0));
                    else
                        throw new RuntimeException("Invoice not found.");
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching Invoice page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<OnChainTransaction>> listOnChainTransactions() {
        if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("24.02.2")) <= 0)  // ToDo: remove when support for 24.02.2 is removed.
            return Single.just(new ArrayList<>());
        else {
            List<OnChainTransaction> tempFeeList = new ArrayList<>();
            BkprlistincomeRequest bkprRequest = BkprlistincomeRequest.newBuilder()
                    .build();

            Single<BkprlistincomeResponse> bkprIncomeRequest = CoreLightningNodeService().bkprListIncome(bkprRequest);
            Single<ListtransactionsResponse> transactionsRequest = CoreLightningNodeService().listTransactions(ListtransactionsRequest.newBuilder().build());

            return Single.zip(bkprIncomeRequest, transactionsRequest, (bkprResponse, transactionsResponse) -> {
                        List<OnChainTransaction> eventList = new ArrayList<>();
                        for (BkprlistincomeIncome_events incomeEvent : bkprResponse.getIncomeEventsList()) {
                            /*
                            BBLog.e(LOG_TAG, "account: " + incomeEvent.getAccount());
                            BBLog.e(LOG_TAG, "type: " + incomeEvent.getTag());
                            BBLog.e(LOG_TAG, "credit: " + incomeEvent.getCreditMsat().getMsat());
                            BBLog.e(LOG_TAG, "debit: " + incomeEvent.getDebitMsat().getMsat());
                            BBLog.e(LOG_TAG, "description: " + incomeEvent.getDescription());
                            BBLog.e(LOG_TAG, "TransactionID: " + ApiUtil.StringFromHexByteString(incomeEvent.getTxid()));
                            BBLog.e(LOG_TAG, "Outpoint: " + incomeEvent.getOutpoint());
                            BBLog.e(LOG_TAG, "______________");
                             */

                            if (incomeEvent.getAccount().equals("wallet") && incomeEvent.getTag().equals("deposit")) {
                                eventList.add(OnChainTransaction.newBuilder()
                                        .setTransactionId(incomeEvent.getOutpoint().split(":")[0])
                                        .setAmount(incomeEvent.getCreditMsat().getMsat())
                                        //.setBlockHeight(incomeEvent.getOutpoint())
                                        //.setConfirmations(WalletUtil.getBlockHeight() - transaction.getBlockheight())
                                        .setTimeStamp(incomeEvent.getTimestamp())
                                        //.setLabel(lndTransaction.getLabel())
                                        .setType(OnChainTransaction.TransactionType.WALLET_RECEIVE)
                                        .build());
                            }

                            if (incomeEvent.getAccount().equals("wallet") && incomeEvent.getTag().equals("withdrawal")) {
                                eventList.add(OnChainTransaction.newBuilder()
                                        .setTransactionId(incomeEvent.getOutpoint().split(":")[0])
                                        .setAmount(incomeEvent.getDebitMsat().getMsat() * -1)
                                        //.setBlockHeight(incomeEvent.getOutpoint())
                                        //.setConfirmations(WalletUtil.getBlockHeight() - transaction.getBlockheight())
                                        .setTimeStamp(incomeEvent.getTimestamp())
                                        //.setLabel(lndTransaction.getLabel())
                                        .setType(OnChainTransaction.TransactionType.WALLET_SEND)
                                        .build());
                            }

                            if (incomeEvent.getAccount().equals("wallet") && incomeEvent.getTag().equals("onchain_fee")) {
                                tempFeeList.add(OnChainTransaction.newBuilder()
                                        .setTransactionId(ApiUtil.StringFromHexByteString(incomeEvent.getTxid()))
                                        .setAmount(incomeEvent.getDebitMsat().getMsat())
                                        .build());
                            }

                        }

                        // Add blockHeight info
                        List<OnChainTransaction> transactionList = new ArrayList<>();
                        for (ListtransactionsTransactions transaction : transactionsResponse.getTransactionsList()) {
                            for (OnChainTransaction t : eventList) {
                                if (ApiUtil.StringFromHexByteString(transaction.getHash()).equals(t.getTransactionId())) {
                                    t.setBlockHeight(transaction.getBlockheight());
                                }
                            }
                        }

                        // Combine transaction fees with transactions
                        for (OnChainTransaction fee : tempFeeList) {
                            for (OnChainTransaction t : eventList) {
                                if (fee.getTransactionId().equals(t.getTransactionId()))
                                    t.setFee(fee.getAmount());
                            }
                        }

                        return eventList;
                    })
                    .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching on-chain transactions failed: " + throwable.fillInStackTrace()));
        }

    }

    private Single<PagedResponse<LnPayment>> getLnPaymentPage(long firstIndexOffset, int pageSize) {
        BBLog.d(LOG_TAG, "Fetching payments page, offset:  " + (firstIndexOffset + 1));
        ListpaysRequest request = ListpaysRequest.newBuilder()
                .setStatus(ListpaysRequest.ListpaysStatus.COMPLETE)
                .setIndex(ListpaysRequest.ListpaysIndex.CREATED)
                .setLimit(pageSize)
                .setStart(firstIndexOffset + 1) //index is one based on this call
                .build();

        return CoreLightningNodeService().listPays(request)
                .map(response -> {
                    long lastIndexOffset = firstIndexOffset;
                    List<LnPayment> paymentsList = new ArrayList<>();
                    for (ListpaysPays payment : response.getPaysList()) {
                        paymentsList.add(LnPayment.newBuilder()
                                .setPaymentHash(ApiUtil.StringFromHexByteString(payment.getPaymentHash()))
                                .setPaymentPreimage(ApiUtil.StringFromHexByteString(payment.getPreimage()))
                                .setDestinationPubKey(ApiUtil.StringFromHexByteString(payment.getDestination()))
                                .setStatus(LnPayment.Status.SUCCEEDED)
                                .setAmountPaid(payment.getAmountMsat().getMsat())
                                .setFee(payment.getAmountSentMsat().getMsat() - payment.getAmountMsat().getMsat())
                                .setCreatedAt(payment.getCreatedAt())
                                .setBolt11(payment.getBolt11())
                                .setBolt12(payment.getBolt12())
                                .setDescription(payment.getDescription())
                                //.setBolt12PayerNote()  This information is contained in the bolt12 string and will only be extracted when it needs to be displayed to improve performance.
                                //.setKeysendMessage(???)
                                .build());
                        lastIndexOffset = payment.getCreatedIndex();
                    }
                    PagedResponse<LnPayment> page = PagedResponse.<LnPayment>newBuilder()
                            .setPage(paymentsList)
                            .setPageSize(response.getPaysCount())
                            .setLastIndexOffset(lastIndexOffset)
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching payment page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<LnPayment>> listLnPayments(long firstIndexOffset, int pageSize) {
        return getLnPaymentPage(firstIndexOffset, pageSize)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listLnPayments(data.getLastIndexOffset(), pageSize)
                                .map(nextPageData -> {
                                    List<LnPayment> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    private Single<PagedResponse<Forward>> getForwardPage(long firstIndexOffset, int pageSize, long startTime) {
        BBLog.d(LOG_TAG, "Fetching forwards page, offset:  " + (firstIndexOffset + 1));
        ListforwardsRequest request = ListforwardsRequest.newBuilder()
                .setStatus(ListforwardsRequest.ListforwardsStatus.SETTLED)
                .setIndex(ListforwardsRequest.ListforwardsIndex.CREATED)
                .setLimit(pageSize)
                .setStart(firstIndexOffset + 1) //index is one based on this call
                .build();

        return CoreLightningNodeService().listForwards(request)
                .map(response -> {
                    long lastIndexOffset = firstIndexOffset;
                    List<Forward> forwardsList = new ArrayList<>();
                    for (ListforwardsForwards forwardingEvent : response.getForwardsList()) {
                        long timestampNS = (long) (forwardingEvent.getReceivedTime() * 1000000000L); // ResolvedTime should be correct, but missing.
                        if ((timestampNS / 1000000000L) > startTime)
                            forwardsList.add(Forward.newBuilder()
                                    .setAmountIn(forwardingEvent.getInMsat().getMsat())
                                    .setAmountOut(forwardingEvent.getOutMsat().getMsat())
                                    .setChannelIdIn(ApiUtil.ScidFromString(forwardingEvent.getInChannel()))
                                    .setChannelIdOut(ApiUtil.ScidFromString(forwardingEvent.getOutChannel()))
                                    .setFee(forwardingEvent.getFeeMsat().getMsat())
                                    .setTimestampNs(timestampNS)
                                    .build());
                        lastIndexOffset = forwardingEvent.getCreatedIndex();
                    }
                    PagedResponse<Forward> page = PagedResponse.<Forward>newBuilder()
                            .setPage(forwardsList)
                            .setPageSize(response.getForwardsCount())
                            .setLastIndexOffset(lastIndexOffset)
                            .build();
                    return page;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching forwarding page failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<Forward>> listForwards(long firstIndexOffset, int pageSize, long startTime) {
        return getForwardPage(firstIndexOffset, pageSize, startTime)
                .flatMap(data -> {
                    if (data == null || data.getPage().isEmpty()) {
                        // No more pages, return an empty list
                        return Single.just(Collections.emptyList());
                    } else if (data.getPageSize() < pageSize) {
                        // Current page has fewer items than pageSize, no more data to fetch
                        return Single.just(data.getPage());
                    } else {
                        // Fetch the next page and concatenate results
                        return listForwards(data.getLastIndexOffset(), pageSize, startTime)
                                .map(nextPageData -> {
                                    List<Forward> combinedList = new ArrayList<>(data.getPage());
                                    combinedList.addAll(nextPageData);
                                    return combinedList;
                                });
                    }
                });
    }

    @Override
    public Single<List<Peer>> listPeers() {
        ListpeersRequest request = ListpeersRequest.newBuilder()
                .build();

        return CoreLightningNodeService().listPeers(request)
                .map(response -> {
                    List<Peer> peerList = new ArrayList<>();
                    for (ListpeersPeers peer : response.getPeersList()) {
                        List<TimestampedMessage> errorMessages = new ArrayList<>();
                        for (ListpeersPeersLog log : peer.getLogList())
                            if (log.getItemType() == ListpeersPeersLog.ListpeersPeersLogType.BROKEN || log.getItemType() == ListpeersPeersLog.ListpeersPeersLogType.UNUSUAL)
                                errorMessages.add(TimestampedMessage.newBuilder()
                                        .setMessage(log.getLog())
                                        .setTimestamp(Long.parseLong(log.getTime()))
                                        .build());

                        if (peer.getConnected())
                            peerList.add(Peer.newBuilder()
                                    .setPubKey(ApiUtil.StringFromHexByteString(peer.getId()))
                                    .setAddress(peer.getNetaddr(0)) // show multiple?
                                    //.setPing(???) // There is an extra "ping" endpoint, but that would lead to many RPC calls for just that info.
                                    //.setFlapCount(???)
                                    //.setLastFlapTimestamp(???)
                                    .setErrorMessages(errorMessages)
                                    //.setFeatures(???)  // Features are available, but just the numbers without names.
                                    .build());
                    }
                    return peerList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching peers failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<List<Bolt12Offer>> listBolt12Offers() {
        ListoffersRequest request = ListoffersRequest.newBuilder()
                .build();

        return CoreLightningNodeService().listOffers(request)
                .map(response -> {
                    List<Bolt12Offer> bolt12OfferList = new ArrayList<>();
                    for (ListoffersOffers offer : response.getOffersList()) {
                        bolt12OfferList.add(Bolt12Offer.newBuilder()
                                .setDecodedBolt12(InvoiceUtil.decodeBolt12(offer.getBolt12()))
                                .setOfferId(ApiUtil.StringFromHexByteString(offer.getOfferId()))
                                .setLabel(offer.getLabel())
                                .setIsActive(offer.getActive())
                                .setIsSingleUse(offer.getSingleUse())
                                .setWasAlreadyUsed(offer.getUsed())
                                .build());
                    }
                    return bolt12OfferList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching bolt12 offers failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable connectPeer(LightningNodeUri lightningNodeUri) {
        ConnectRequest request = ConnectRequest.newBuilder()
                .setId(lightningNodeUri.getAsString())
                .build();
        return CoreLightningNodeService().connectPeer(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Connecting to peer failed: " + throwable.getMessage()));
    }

    @Override
    public Completable disconnectPeer(String pubKey) {
        DisconnectRequest request = DisconnectRequest.newBuilder()
                .setId(ApiUtil.ByteStringFromHexString(pubKey))
                .setForce(false)
                .build();
        return CoreLightningNodeService().disconnect(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Disconnecting from peer failed: " + throwable.getMessage()));
    }

    @Override
    public Single<CreateInvoiceResponse> createInvoice(CreateInvoiceRequest createInvoiceRequest) {
        AmountOrAny amountMsat = null;
        if (createInvoiceRequest.getAmount() == 0)
            amountMsat = AmountOrAny.newBuilder()
                    .setAny(true)
                    .build();
        else
            amountMsat = AmountOrAny.newBuilder()
                    .setAmount(Amount.newBuilder()
                            .setMsat(createInvoiceRequest.getAmount())
                            .build())
                    .build();

        InvoiceRequest request = InvoiceRequest.newBuilder()
                .setAmountMsat(amountMsat)
                .setDescription(createInvoiceRequest.getDescription())
                .setExpiry(createInvoiceRequest.getExpiry())
                // createInvoiceRequest.getIncludeRouteHints()  ???
                .setLabel(UUID.randomUUID().toString())
                .build();

        return CoreLightningNodeService().invoice(request)
                .map(response -> {
                    return CreateInvoiceResponse.newBuilder()
                            .setBolt11(response.getBolt11())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Creating invoice failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<String> getNewOnchainAddress(NewOnChainAddressRequest newOnChainAddressRequest) {
        NewaddrRequest.NewaddrAddresstype addressType = NewaddrRequest.NewaddrAddresstype.P2TR;
        switch (newOnChainAddressRequest.getType()) {
            case SEGWIT_COMPATIBILITY:  // Core Lightning cannot produce legacy segwit addresses.
            case SEGWIT:
                addressType = NewaddrRequest.NewaddrAddresstype.BECH32;
                break;
            case TAPROOT:
                // ToDo: remove this when support for 24.02.2 is removed. A bug in that version causes P2TR to no work.
                if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("24.02.2")) <= 0)
                    addressType = NewaddrRequest.NewaddrAddresstype.BECH32;
                else
                    addressType = NewaddrRequest.NewaddrAddresstype.P2TR;
        }
        NewaddrRequest request = NewaddrRequest.newBuilder()
                .setAddresstype(addressType)
                .build();

        return CoreLightningNodeService().newAddr(request)
                .map(response -> {
                    String addressTypeString = PrefsUtil.getPrefs().getString("btcAddressType", "bech32m");
                    switch (addressTypeString) {
                        case "bech32":
                            return response.getBech32();
                        case "bech32m":
                            if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("24.02.2")) <= 0) // ToDo: remove when support for 24.02.2 is removed. A bug in that version causes P2TR to no work.
                                return response.getBech32();
                            else
                                return response.getP2Tr();
                        default:
                            return response.getBech32();
                    }
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Creating new OnChainAddress failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Single<SendLnPaymentResponse> sendLnPayment(SendLnPaymentRequest sendLnPaymentRequest) {
        switch (sendLnPaymentRequest.getPaymentType()) {

            case BOLT11_INVOICE:
                PayRequest.Builder requestBuilder = PayRequest.newBuilder()
                        .setBolt11(sendLnPaymentRequest.getBolt11().getBolt11String())
                        .setMaxfee(Amount.newBuilder()
                                .setMsat(sendLnPaymentRequest.getMaxFee())
                                .build())
                        .setRetryFor(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier());

                if (sendLnPaymentRequest.getBolt11() != null) {
                    if (sendLnPaymentRequest.getBolt11().hasNoAmountSpecified())
                        requestBuilder.setAmountMsat(Amount.newBuilder()
                                .setMsat(sendLnPaymentRequest.getAmount()));
                }

                PayRequest request = requestBuilder.build();

                return CoreLightningNodeService().pay(request)
                        .map(response -> {
                            switch (response.getStatus()) {
                                case COMPLETE:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setPaymentPreimage(ApiUtil.StringFromHexByteString(response.getPaymentPreimage()))
                                            .build();
                                default:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setFailureReason(SendLnPaymentResponse.FailureReason.UNKNOWN)
                                            .setAmount(response.getAmountMsat().getMsat())
                                            .build();
                            }
                        })
                        .doOnError(throwable -> BBLog.w(LOG_TAG, "Error sending lightning payment: " + throwable.fillInStackTrace()));

            case BOLT12_INVOICE:
                PayRequest.Builder bolt12requestBuilder = PayRequest.newBuilder()
                        .setBolt11(sendLnPaymentRequest.getBolt12InvoiceString())
                        .setMaxfee(Amount.newBuilder()
                                .setMsat(sendLnPaymentRequest.getMaxFee())
                                .build())
                        .setRetryFor(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier());

                PayRequest bolt12request = bolt12requestBuilder.build();

                return CoreLightningNodeService().pay(bolt12request)
                        .map(response -> {
                            switch (response.getStatus()) {
                                case COMPLETE:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setPaymentPreimage(ApiUtil.StringFromHexByteString(response.getPaymentPreimage()))
                                            .build();
                                default:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setFailureReason(SendLnPaymentResponse.FailureReason.UNKNOWN)
                                            .setAmount(response.getAmountMsat().getMsat())
                                            .build();
                            }
                        })
                        .doOnError(throwable -> BBLog.w(LOG_TAG, "Error sending lightning payment: " + throwable.fillInStackTrace()));

            case KEYSEND:
                List<TlvEntry> tlvEntries = new ArrayList<>();
                for (CustomRecord cr : sendLnPaymentRequest.getCustomRecords()) {
                    if (cr.getFieldNumber() != PaymentUtil.KEYSEND_PREIMAGE_RECORD) // We have to filter the preimage record out, this is already handled by the keySend api endpoint. Otherwise it will throw an error.
                        tlvEntries.add(TlvEntry.newBuilder()
                                .setType(cr.getFieldNumber())
                                .setValue(ApiUtil.ByteStringFromHexString(cr.getValue()))
                                .build());
                }

                KeysendRequest keysendRequest;
                if (Wallet.getInstance().getCurrentNodeInfo().getVersion().compareTo(new Version("24.11")) >= 0) { // ToDo: remove when support for 24.08.2 is removed. Keysend did not support maxFee setting until 24.11.
                    keysendRequest = KeysendRequest.newBuilder()
                            .setDestination(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getDestinationPubKey()))
                            .setAmountMsat(Amount.newBuilder()
                                    .setMsat(sendLnPaymentRequest.getAmount())
                                    .build())
                            .setMaxfee(Amount.newBuilder()
                                    .setMsat(sendLnPaymentRequest.getMaxFee())
                                    .build())
                            .setRetryFor(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier())
                            .setExtratlvs(TlvStream.newBuilder()
                                    .addAllEntries(tlvEntries)
                                    .build())
                            .build();
                } else {
                    keysendRequest = KeysendRequest.newBuilder()
                            .setDestination(ApiUtil.ByteStringFromHexString(sendLnPaymentRequest.getDestinationPubKey()))
                            .setAmountMsat(Amount.newBuilder()
                                    .setMsat(sendLnPaymentRequest.getAmount())
                                    .build())
                            .setMaxfeepercent((double) sendLnPaymentRequest.getMaxFee() / (double) sendLnPaymentRequest.getAmount()) // ToDo: replace with maxfee once it is available
                            .setExemptfee(Amount.newBuilder()
                                    .setMsat(0)
                                    .build())
                            .setRetryFor(RefConstants.TIMEOUT_LONG * TorManager.getInstance().getTorTimeoutMultiplier())
                            .setExtratlvs(TlvStream.newBuilder()
                                    .addAllEntries(tlvEntries)
                                    .build())
                            .build();
                }

                return CoreLightningNodeService().keySend(keysendRequest)
                        .map(response -> {
                            switch (response.getStatus()) {
                                case COMPLETE:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setPaymentPreimage(ApiUtil.StringFromHexByteString(response.getPaymentPreimage()))
                                            .build();
                                default:
                                    return SendLnPaymentResponse.newBuilder()
                                            .setFailureReason(SendLnPaymentResponse.FailureReason.UNKNOWN)
                                            .setAmount(response.getAmountMsat().getMsat())
                                            .build();
                            }
                        })
                        .doOnError(throwable -> BBLog.w(LOG_TAG, "Error sending lightning payment: " + throwable.fillInStackTrace()));
            default:
                return Single.error(new IllegalStateException("Unknown payment type."));
        }
    }

    @Override
    public Completable sendOnChainPayment(SendOnChainPaymentRequest sendOnChainPaymentRequest) {
        WithdrawRequest.Builder requestBuilder = WithdrawRequest.newBuilder()
                .setDestination(sendOnChainPaymentRequest.getAddress())
                .setSatoshi(amountOrAllFromMsat(sendOnChainPaymentRequest.getAmount(), sendOnChainPaymentRequest.isSendAll(), false))
                .setFeerate(Feerate.newBuilder()
                        .setPerkw((int) UtilFunctions.satPerVByteToSatPerKw(sendOnChainPaymentRequest.getSatPerVByte()))
                        .build());

        if (sendOnChainPaymentRequest.hasUTXOs())
            for (Outpoint outpoint : sendOnChainPaymentRequest.getUTXOs()) {
                requestBuilder.addUtxos(com.github.ElementsProject.lightning.cln.Outpoint.newBuilder()
                        .setOutnum(outpoint.getOutputIndex())
                        .setTxid(ApiUtil.ByteStringFromHexString(outpoint.getTransactionID()))
                        .build());
            }

        WithdrawRequest request = requestBuilder.build();

        return CoreLightningNodeService().withdraw(request)
                .ignoreElement()  // This will convert a Single to a Completable, ignoring the result
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Sending on chain payment failed: " + throwable.getMessage()));
    }

    @Override
    public Single<List<String>> updateRoutingPolicy(UpdateRoutingPolicyRequest updateRoutingPolicyRequest) {
        String target = updateRoutingPolicyRequest.hasChannel() ? updateRoutingPolicyRequest.getChannel().getShortChannelId().toString() : "all";
        SetchannelRequest.Builder requestBuilder = SetchannelRequest.newBuilder()
                .setId(target);

        if (updateRoutingPolicyRequest.hasFeeBase())
            requestBuilder.setFeebase(amountFromMsat(updateRoutingPolicyRequest.getFeeBase()));
        if (updateRoutingPolicyRequest.hasFeeRate())
            requestBuilder.setFeeppm(((int) updateRoutingPolicyRequest.getFeeRate()));
        if (updateRoutingPolicyRequest.hasMinHTLC())
            requestBuilder.setHtlcmin(amountFromMsat(updateRoutingPolicyRequest.getMinHTLC()));
        if (updateRoutingPolicyRequest.hasMaxHTLC())
            requestBuilder.setHtlcmax(amountFromMsat(updateRoutingPolicyRequest.getMaxHTLC()));

        return CoreLightningNodeService().setChannel(requestBuilder.build())
                .map(response -> {
                    List<String> errorList = new ArrayList<>();
                    return errorList;
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Updating channel policy failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable openChannel(OpenChannelRequest openChannelRequest) {
        FundchannelRequest.Builder requestBuilder = FundchannelRequest.newBuilder()
                .setId(ApiUtil.ByteStringFromHexString(openChannelRequest.getNodePubKey()))
                .setAnnounce(!openChannelRequest.isPrivate())
                .setAmount(amountOrAllFromMsat(openChannelRequest.getAmount(), false, false))
                .setFeerate(Feerate.newBuilder()
                        .setPerkw((int) UtilFunctions.satPerVByteToSatPerKw(openChannelRequest.getSatPerVByte()))
                        .build());

        if (openChannelRequest.hasUTXOs())
            for (Outpoint outpoint : openChannelRequest.getUTXOs()) {
                requestBuilder.addUtxos(com.github.ElementsProject.lightning.cln.Outpoint.newBuilder()
                        .setOutnum(outpoint.getOutputIndex())
                        .setTxid(ApiUtil.ByteStringFromHexString(outpoint.getTransactionID()))
                        .build());
            }

        FundchannelRequest request = requestBuilder.build();

        return CoreLightningNodeService().fundChannel(request)
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error opening channel: " + throwable.getMessage()));
    }

    @Override
    public Completable closeChannel(CloseChannelRequest closeChannelRequest) {
        CloseRequest request = CloseRequest.newBuilder()
                .setId(closeChannelRequest.getShortChannelId().toString())
                .build();

        return CoreLightningNodeService().close(request)
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error closing channel: " + throwable.getMessage()));
    }

    @Override
    public Single<Bolt12Offer> createBolt12Offer(CreateBolt12OfferRequest createBolt12OfferRequest) {
        String amountMsat = "any";
        if (createBolt12OfferRequest.getAmount() != 0)
            amountMsat = String.valueOf(createBolt12OfferRequest.getAmount());


        OfferRequest.Builder requestBuilder = OfferRequest.newBuilder();
        requestBuilder.setAmount(amountMsat);
        requestBuilder.setSingleUse(createBolt12OfferRequest.getSingleUse());

        if (createBolt12OfferRequest.getInternalLabel() != null)
            requestBuilder.setLabel(createBolt12OfferRequest.getInternalLabel());

        if (createBolt12OfferRequest.getDescription() != null)
            requestBuilder.setDescription(createBolt12OfferRequest.getDescription());


        return CoreLightningNodeService().offer(requestBuilder.build())
                .map(response -> {
                    if (!response.getCreated())
                        throw new RuntimeException(App.getAppContext().getString(R.string.error_offer_already_exists));
                    return Bolt12Offer.newBuilder()
                            .setDecodedBolt12(InvoiceUtil.decodeBolt12(response.getBolt12()))
                            .setOfferId(ApiUtil.StringFromHexByteString(response.getOfferId()))
                            .setLabel(response.getLabel())
                            .setIsActive(response.getActive())
                            .setIsSingleUse(response.getSingleUse())
                            .setWasAlreadyUsed(response.getUsed())
                            .build();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Creating offer failed: " + throwable.fillInStackTrace()));
    }

    @Override
    public Completable disableBolt12Offer(String offerId) {
        DisableofferRequest request = DisableofferRequest.newBuilder()
                .setOfferId(ApiUtil.ByteStringFromHexString(offerId))
                .build();

        return CoreLightningNodeService().disableOffer(request)
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error disabling offer: " + throwable.getMessage()));
    }

    @Override
    public Completable enableBolt12Offer(String offerId) {
        EnableofferRequest request = EnableofferRequest.newBuilder()
                .setOfferId(ApiUtil.ByteStringFromHexString(offerId))
                .build();

        return CoreLightningNodeService().enableOffer(request)
                .ignoreElement()
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Error enabling offer: " + throwable.getMessage()));
    }

    @Override
    public Single<String> fetchInvoiceFromBolt12Offer(FetchInvoiceFromOfferRequest fetchInvoiceFromOfferRequest) {
        FetchinvoiceRequest.Builder requestBuilder = FetchinvoiceRequest.newBuilder()
                .setOffer(fetchInvoiceFromOfferRequest.getDecodedBolt12().getBolt12String())
                .setAmountMsat(Amount.newBuilder()
                        .setMsat(fetchInvoiceFromOfferRequest.getAmount())
                        .build());

        if (fetchInvoiceFromOfferRequest.getComment() != null)
            requestBuilder.setPayerNote(fetchInvoiceFromOfferRequest.getComment());

        return CoreLightningNodeService().fetchInvoice(requestBuilder.build())
                .map(response -> {
                    return response.getInvoice();
                })
                .doOnError(throwable -> BBLog.w(LOG_TAG, "Fetching invoice from offer failed: " + throwable.fillInStackTrace()));
    }

    private Amount amountFromMsat(long msat) {
        return Amount.newBuilder()
                .setMsat(msat)
                .build();
    }

    private AmountOrAll amountOrAllFromMsat(long msat, boolean all, boolean mSatPrecision) {
        if (all)
            return AmountOrAll.newBuilder()
                    .setAll(true)
                    .build();
        else {
            long mSatToUse = msat;
            if (!mSatPrecision)
                mSatToUse = (msat / 1000) * 1000L;

            return AmountOrAll.newBuilder()
                    .setAmount(amountFromMsat(mSatToUse))
                    .build();
        }
    }
}
