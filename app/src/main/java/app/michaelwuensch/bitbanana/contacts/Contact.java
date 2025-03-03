package app.michaelwuensch.bitbanana.contacts;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.UUID;

import app.michaelwuensch.bitbanana.R;
import app.michaelwuensch.bitbanana.lightning.LNAddress;
import app.michaelwuensch.bitbanana.lightning.LightningNodeUri;
import app.michaelwuensch.bitbanana.lightning.LightningParser;

public class Contact implements Comparable<Contact>, Serializable {

    private String id;
    private String contactData;
    private String alias;
    private ContactType contactType;

    public Contact(String id, ContactType contactType, String contactData, String alias) {
        this.id = id;
        this.contactType = contactType;
        this.contactData = contactData;
        this.alias = alias;
    }

    public String getId() {
        return this.id;
    }

    public String getContactData() {
        return this.contactData;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public ContactType getContactType() {
        return this.contactType;
    }

    public LightningNodeUri getAsNodeUri() {
        return LightningParser.parseNodeUri(contactData);
    }

    public LNAddress getLightningAddress() {
        return new LNAddress(this.contactData);
    }

    // Used for item adapter
    public String getContent () {return this.alias + this.contactData.toLowerCase();}


    @Override
    public int compareTo(Contact contact) {
        Contact other = contact;
        return this.getAlias().toLowerCase().compareTo(other.getAlias().toLowerCase());
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Contact contact = (Contact) obj;
        return contact.getContactData().equalsIgnoreCase(this.getContactData());
    }

    @Override
    public int hashCode() {
        if (this.id == null) {
            // Create the UUID for the new config
            this.id = UUID.randomUUID().toString();
        }
        return this.id.hashCode();
    }

    public enum ContactType {
        NODEPUBKEY,
        LNADDRESS;

        public static Contact.ContactType parseFromString(String enumAsString) {
            try {
                return valueOf(enumAsString);
            } catch (Exception ex) {
                return NODEPUBKEY;
            }
        }

        public int getTitle() {
            switch (this) {
                case NODEPUBKEY:
                    return R.string.node;
                case LNADDRESS:
                    return R.string.ln_address;
                default:
                    return R.string.node;
            }
        }
    }
}
