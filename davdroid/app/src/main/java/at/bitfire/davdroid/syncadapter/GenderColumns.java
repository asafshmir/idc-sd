package at.bitfire.davdroid.syncadapter;

import android.provider.ContactsContract;

public final class GenderColumns {

    public final static String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.at.bitfire.davdroid.contacts.gender";

    public final static String
            SUMMARY = ContactsContract.RawContacts.Data.DATA1,
            SEX = ContactsContract.RawContacts.Data.DATA2,
            GENDER_IDENTITY = ContactsContract.RawContacts.Data.DATA3;

    public final static int
            SEX_MALE = 0,
            SEX_FEMALE = 1,
            SEX_OTHER = 2,
            SEX_NOT_APPLICABLE = 3,
            SEX_UNKNOWN = 4;

}
