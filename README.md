# What
This is an application that alternative to EZProxy.

# History
This first came as an entrepreneur idea to replace the EZproxy. It was developed back in 2013, and tested fine with some content vendors including EBSCO, Primo and etc.

It covers some key features that EZproxy had: URL rewrite, cookie rewrite and etc. It worked very well for the core feature and also during the performance test and stress test. Since the development of EZProxy was almost dead, we thought we could quickly eat EZproxy's market share and launch it as a web service for a lot of libraries.

### However, things did not go as we planned.
* A lot of libraries had already adopted EZProxy, and this application did not have significant benefits over it. Few libraries were willing to move over.
* The initial idea was to host the application server in the cloud, and a lot of libraries can share it. However, we did see resistant from content vendors as requests come from a single IP which was hard for them to charge the content usage; If we had one application per library, the maintenance/hosting cost would be too pricey; If we shipped the application and let libraries to host by themselves, we would face the same maintenance/support issues as EZproxy.
* Since it was a difficult sell, it lost support and went sideways.
* More and more libraries/universities are adopting Single-Sign-On, and the proxy approach became legacy.

Since a few librarians told me that it would be great to give it free for libraries. I decided to open source it. I believe the solid application core would help someone who wants to continue the journey.

# Main Feature
It is influenced a lot by EZProxy, so that patron can share same IP to access restricted content, since the easiest way for content vendor to restrict the access is through IP address.

If this application running under hostname proxy4eresource.org. Then patron could access http://www.b.c through http://www.b.c.proxy4eresource.org . All hyperlink that in the HTML that sends to the patron are re-written to http://www.b.c.proxy4eresource.org, so that patrons would never leave the proxy. In addition, if there are hyperlinks in the HTML that links to http://x.b.c or http://x.y.z.b.c, the proxy would also smart enough to rewrite them to http://x.b.c.proxy4eresource.org and http://x.y.z.b.c.proxy4eresource.org (Top level domain rewrite)

Furthermore, the proxy is also smart enough to rewrite cookies. For instance, cookies from host a.b.c would be rewritten to a.b.c.proxy4eresource.org and send to the patron which is exactly the expected behavior if no proxy in between.

The application has been tested around 30 E-Resource sites that commonly used by libraries at that time, and all of the tested sites had green lights.

# Limitation
 * The proxy does not work well with HTTPs or non-standard port (other than port 80/443). The proxy can fetch data from the content vendor with https without a problem, but it needs its own certificate to serve https.  This is a universal problem, EZproxy also does not support HTTPS or non-standard port very well.
 * The proxy does not have good authentication integration. Currently, it can forward patrons to a login page. The idea is after login, the other application set up a cookie or send an HTTP request to proxy with a token to continue.
 * It doesn't support modern websites which use a lot of Javascript and etc. Since this is mainly targeting E-Resource sites.
 * Statstics is limited. It currently write to a log file. One way is that you can add a reverse proxy, e.g. nginx on top of it, which not only cache static data like images or css, but also can be benefit by the nginx log, which can be analzed by a lot of tools.

# Techinial Information
It is developed with Java and designed to run under Linux.

# How to run it
* take a look at the src/main/resources/config.properties file
* If you leave it unchanged, you need to change your DNS or host files (/etc/hosts in Linux) like

<code>
127.0.1.2	www.msvu.ca.proxy4eresource.org sites.stfx.ca.proxy4eresource.org
</code>

Note, you might choose sites other than www.msvu.ca or sites.stfx.ca . But make sure those sites support HTTP. More and more sites only support https nowadays.

* Setup JAVA_HOME, PATH, and Maven
using root (requires permission of port 80) and run

<code>
mvn compile exec:java -Dexec.mainClass="proxy4eresource.proxy.Launcher"
</code>

* You should be able to open your browser to
http://sites.stfx.ca.proxy4eresource.org or http://www.statcan.gc.ca.proxy4eresource.org
to see the result.

For production, you might change the log4j.properties to be less verbose and point the *.proxy.yourdomain.org to the application you are running.

# License
GNU GPL v3

Same as Linux, Druple, WordPress and etc that are used by many universities, libraries and non-profit organizations.
