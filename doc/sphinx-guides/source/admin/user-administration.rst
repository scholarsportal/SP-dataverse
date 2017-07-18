User Administration
===================

This section focuses on user administration tools and tasks. 

.. contents:: Contents:
	:local:

Manage Users Table
------------------

The Manage Users table gives the network administrator a list of all user accounts in table form. It lists username, full name, email address, and whether or not the user has Superuser status.

Usernames are listed alphabetically and clicking on a username takes you to the account page that contains detailed information on that account.

You can access the Manage Users table by clicking the "Manage Users" button on the Dashboard, which is linked from the header of all Dataverse pages (if you're loggied in as an administrator).

Confirm Email
-------------

Dataverse encourages builtin/local users to verify their email address upon signup or email change so that sysadmins can be assured that users can be contacted.

The app will send a standard welcome email with a URL the user can click, which, when activated, will store a ``lastconfirmed`` timestamp in the ``authenticateduser`` table of the database. Any time this is "null" for a user (immediately after signup and/or changing of their Dataverse email address), their current email on file is considered to not be verified. The link that is sent expires after a time (the default is 24 hours), but this is configurable by a superuser via the ``:MinutesUntilConfirmEmailTokenExpires`` config option.

Should users' URL token expire, they will see a "Verify Email" button on the account information page to send another URL.

Sysadmins can determine which users have verified their email addresses by looking for the presence of the value ``emailLastConfirmed`` in the JSON output from listing users (see the "Admin" section of the :doc:`/api/native-api`). As mentioned in the :doc:`/user/account` section of the User Guide, the email addresses for Shibboleth users are re-confirmed on every login.

Deleting an API Token
---------------------

If an API token is compromised it should be deleted. Users can generate a new one for themselves as explained in the :doc:`/user/account` section of the User Guide, but you may want to preemptively delete tokens from the database.

Using the API token 7ae33670-be21-491d-a244-008149856437 as an example:

``delete from apitoken where tokenstring = '7ae33670-be21-491d-a244-008149856437';``

You should expect the output ``DELETE 1`` after issuing the command above.

