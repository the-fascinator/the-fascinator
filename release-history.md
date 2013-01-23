= Current Release v1.1.1 =
Bug fix release to support the ReDBox v1.5.1 release.

*Release Date:* 13 August 2012

 * Fixed resumption token issue in OAI-PMH feed that was causing the ANDS harvester to fail. issue #69, r3248
 * Various Apache Maven configuration changes to make the build more reliable 


----
= v1.1 =

Last release under the [http://www.redboxresearchdata.com.au/governance/eif048 EIF048 funding], the bug fixes and improvements from this version, combined with the last few versions were in support of the ReDBox v1.5 release.

*Release Date:* 23 May 2012

 * Caching configuration for 'off' profile fixed. issue #65, r3218
 * Some additional logging from the search script when purging the session of cached search filters. r3220
 * Added improved Solr field for sorting alpha numeric strings. r3222
 * The last page of result in the OAI-PMH feed no longer sends a resumption token. issue #11, r3224
 * Bug fix for property substitution when retrieving lists of Strings from Json Library. r3226

----
= v1.0.8 =

Yet another minor release primarily in support of projects deployed on the platform.

*Release Date:* 9 May 2012

 * A typo in drop-down widgets broke functionality. r3191
 * Fixed critical bug in OAI-PMH Harvester on 'delete' records. issue #63, r3194
 * More escaping of Strings used on screen.
 * Uploaded files can now provide a 'context', allowing identical _filenames_ to be uploaded without being considered identical _files_ (provided they have different contexts). r3198

----
= v1.0.7 =

This is another minor release primarily in support of projects deployed on the platform.

*Release Date:* 10 April 2012

 * Installation of Sonar on the build server has instigated wide spread standards checking and 'best practice' compliance across the codebase. This is only in its infancy, but you can always check out the [https://redbox-build.cqu.edu.au/sonar/ Sonar dashboard] as this will be an ongoing effort.
 * Drop down lists in Jaffa can now accept a 'description' field from a data source. r3163, r3165
 * Security Enhancements.

----
= v1.0.6 =

Minor release primarily in support of projects deployed on the platform.

*Release Date:* 11 January 2012

 * Fixed bug in OAI-PMH Harvester's sample rules file, related to issue #49. r3122
 * A new 'Delete by Query' action has been added to the portal, allowing admins to script mass deletions somewhat easier. It is coded to run in 'test' mode only until edited by an admin... for safety reasons. r3123
 * !JsonVelocity contrib plugin updated to provide access to system configuration inside templates. r3126

----
= v1.0.5 =

Major works on system messaging for robustness and improved functionality. The Solr Event Log Subscriber and the !ReIndex tool also got some long overdue tender loving care.

*Release Date:* 04 October 2011

*Release Notes:*

 * Stage 2 [BackupMigrationToolPlanning restore tool] implemented (issue #10), allowing harvest file OID remapping and inclusion of custom migration scripts. (issue #57) Migration from one host to another should now be possible.
 * Added new 'History' screen to display Solr log for object. r3071
 * Solr logging configuration fixed. (issue #58)
 * Numerous minor fixes to 'about' pages. (issue #54, issue #55, issue #56)
 * Object deletion is more robust when small sections error. r3064
 * Some timing issues and bugs resolved in !SubscriberQueueConsumer. ID generation now uses [http://download.oracle.com/javase/6/docs/api/java/util/UUID.html UUIDs] r3080
 * Significant work on messaging: r3083
   * Some core messaging features previously in the Portal having been centralised to the Core Library. It is now possible to start AMQ without a Portal from inside unit tests.
   * Message queue bootup delays at system start are no longer hardcoded. Servers with slow Solr boot times can accommodate.
   * Some new experimental work on transaction management provides alternative to traditional tool chains. A new '`TransactionManager`' plugin supports this.
   * '`MessagingServices`' should handle a lot of edge case errors to do with abnormally terminating sessions. It is also quite trivial now to send JMS messages to other systems as well.
   * Message statistics can now be routed by configuration, and disabled if desired.
 * Objects posting messages to the tool chain can now be configured regarding the specific queue to use. r3085

----
= v1.0.4 =

This release focuses on significant updates to the [LoggingFrameworks logging framework] 'plumbing' throughout the platform. There are also a number of bug fixes and the test release of a new restoration process.

*Release Date:* 22 August 2011

*Release Notes:*

 * Work on [LoggingFrameworks logging frameworks] to address bugs caused by Solr version upgrade.
 * Solr Indexer now binds access to the '`log`' object to the Jython engine, making it accessible inside of rules files. (issue #43)
 * Implement [BackupMigrationToolPlanning Stage 1 of re-index/restore] tool. (issue #46)
 * Minor improvement to File System Storage Plugin when it finds objects without an 'objectId' property. Needs further work (issue #48), this 'fix' is just a workaround.
 * !FileSystemHarvester's new database cache has been added to developer purge scripts. (issue #6)
 * Fixed an occasional number parsing error in House Keeping. (issue #8).
 * Some work-around fixes to OAI-PMH harvest problems added to sample rules file. More work is required in the harvester (issue #49).
 * Fixed threading bug in file lock unit test. (issue #28)
 * Improved several contrib plugins to allow the [https://redbox-build.cqu.edu.au/jenkins/ build server] to do automated documentation builds now that they are separated from the core Fascinator POMs.
 * Fixed Atom Feed bugs resulting from code being out-of-date. (issue #50)

----
= v1.0.3 =

This release was concerned with migrating to new build/release infrastructure. Upgrading to v3.3.0 of Solr was required as part of this and some associated changes resulted.

*Release Date:* 06 July 2011

*Release Notes:*

 * Several non-core plugins moved from trunk to 'contrib' branch.
 * Maven repository references to USQ infrastructure removed and linked to Sonatype.
 * Package names across the project altered to match Maven group IDs (using 'googlecode' domain).
 * Solr v3.3.0 version upgrade.
 * Updates to indexing and sorting is a few locations in response to Solr v3.3.0 not allowing sorting on multi-valued fields.
 * Fixed packaging bugs and improved logging on search and organiser screen.
 * Addressed some deprecated practices causing log bloat.

----
= v1.0.2 =

Aside from some minor bug fixes, this release was mostly to cover the code migration to Google Code and some related alterations to POMs.

*Release Date:* 05 May 2011

----
= Pre-migration =

There were numerous releases performed prior to our migration to Google Code. Those releases, and associated tickets are documented on USQ infrastructure (for now).

Links and a brief summary are provided below:

 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version1.0.1 v1.0.1]: *27 April 2011*
   * Overhaul and improvement of OAI feeds against a variety of less common scenarios.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version1.0.0 v1.0.0]: *14 April 2011*
   * Significant optimizations to Jython Caching in Portal.
   * Implement form based workflow to ingest metadata and data together.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.5 v0.7.5]: *03 March 2011*
   * Most areas of the codebase migrated to the new JSON Library.
   * Upgrade jQuery to v1.4.4.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.4 v0.7.4]: *07 February 2011*
   * New JSON Library implemented.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.3 v0.7.3]: *20 January 2011*
   * The Solr indexer now uses POST instead of GET to support long queries.
   * Added HTTP byte range support to enable HTML5 video streaming .
   * Improved Storage API to enable support for retrieving the last modified date and size of payloads.
   * Additional opens for third party security integration.
   * Added sorting options for search results page.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.2 v0.7.2]: *22 November 2010*
   * Several minor bug fixes and improvements.
   * This release was mainly to support downstream projects.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.1 v0.7.1]: *11 November 2010*
   * Fixed caching bug causing performance and memory issues.
   * Minor bug fixes and improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.7.0 v0.7.0]: *05 November 2010*
   * New display template framework implemented.
   * Solr indexing optimized.
   * New Subscriber Plugin API added with logging implementation as first example.
   * Third party security integration now possible with trust tokens. 
   * Embedded media player switched to FlowPlayer.
   * Security model now allows exceptions for individual users.
   * A new Tapestry service added offering generic database access.
   * Numerous bug fixes and minor improvements.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.6.0 v0.6.0]: *11 August 2010*
   * Optimizations to packaging and tool chains.
   * Overhaul of the branding to improve flexibility.
   * A new SSO interface and performance boost to security plugins.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.5.0 v0.5.0]: *14 May 2010*
   * New security model and workflow system implemented.
   * A new transformer for handling media files implemented.
   * Integration of a new annotation library for media fragment annotation and geospatial annotation.
   * Storage API re-factored and improved.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.4.0 v0.4.0]: *28 January 2010*
   * New integration with ePub, IMS and Sword.
   * Work on message queues for automated ingest.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.3.0 v0.3.0]: *19 November 2009*
   * Layout of Portal altered.
   * Maven POM structures improved.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.2.0 v0.2.0]: *23 October 2009*
   * Additional transformers developed.
 * [https://fascinator.usq.edu.au/trac/wiki/tf2/Version0.1.0 v0.1.0]: *25 August 2009*
   * Initial development build.
