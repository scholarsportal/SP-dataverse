
Dataverse SP_4,7 has been forked from dataverse 4.7 and enhanced with the following:

1.	Custom splash page
a.	Updated top ribbon and added French toggle link
b.	Added Banner
c.	Dynamically adjusted first institutional dataverse based on user’s IP
d.	Descriptive text for Tri-Agency Statements
e.	Updated footer
f.	Accounted for mobile experience with fluid design

2.	Sign-up
a.	Added Affiliate dropdown
i.	Auto selects the institution affiliated with user’s IP
ii.	Allows, ‘Other’ option triggering display of editable field, which is mandatory
b.	Incorporated Captcha widget bound to submit button

3.	Log-In redirect when a user logs in they are redirected to their institutional Dataverse

4.	Contact form - Incorporated Captcha widget bound to submit button

5.	Data Exploration
a.	Updated the “Explore” button associated with tab files to link to an application allowing:
i.	Variable searching
ii.	Charting
iii.	Comparing
iv.	Paging
v.	Sub setting
vi.	French support – connected to dataverse

6.	Datacite HTML submit bug fix

7.	Internationalization



Dataverse is an [open source][] web application for sharing, citing, analyzing, and preserving research data (developed by the [Data Science and Products team](http://www.iq.harvard.edu/people/people/data-science-products) at the [Institute for Quantitative Social Science](http://iq.harvard.edu/) and the [Dataverse community][]).

[dataverse.org][] is our home on the web and shows a map of Dataverse installations around the world, a list of [features][], [integrations][] that have been made possible through [REST APIs][], our development [roadmap][], and more.

We maintain a demo site at [demo.dataverse.org][] which you are welcome to use for testing and evaluating Dataverse.

To install Dataverse, please see our [Installation Guide][] which will prompt you to download our [latest release][].

To discuss Dataverse with the community, please join our [mailing list][], participate in a [community call][], chat with us at [chat.dataverse.org][], or attend our annual [Dataverse Community Meeting][].

We love contributors! Please see our [Contributing Guide][] for ways you can help.

Dataverse is a trademark of President and Fellows of Harvard College and is registered in the United States.

[![Dataverse Project logo](src/main/webapp/resources/images/dataverseproject_logo.jpg?raw=true "Dataverse Project")](http://dataverse.org)

[![Build Status](https://travis-ci.org/IQSS/dataverse.svg?branch=develop)](https://travis-ci.org/IQSS/dataverse) [![Coverage Status](https://coveralls.io/repos/IQSS/dataverse/badge.svg?branch=develop&service=github)](https://coveralls.io/github/IQSS/dataverse?branch=develop)

[dataverse.org]: https://dataverse.org
[demo.dataverse.org]: https://demo.dataverse.org
[Dataverse community]: https://dataverse.org/developers
[Installation Guide]: http://guides.dataverse.org/en/latest/installation/index.html
[latest release]: https://github.com/IQSS/dataverse/releases
[features]: https://dataverse.org/software-features
[roadmap]: https://dataverse.org/goals-roadmap-and-releases
[integrations]: https://dataverse.org/integrations
[REST APIs]: http://guides.dataverse.org/en/latest/api/index.html
[Contributing Guide]: CONTRIBUTING.md
[mailing list]: https://groups.google.com/group/dataverse-community
[community call]: https://dataverse.org/community-calls
[chat.dataverse.org]: http://chat.dataverse.org
[Dataverse Community Meeting]: https://dataverse.org/events
[open source]: LICENSE.md
