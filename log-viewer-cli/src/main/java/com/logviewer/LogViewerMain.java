package com.logviewer;

import com.logviewer.config.LogViewerServerConfig;
import com.logviewer.data2.LogContextHolder;
import com.logviewer.web.LogViewerServlet;
import com.logviewer.web.LogViewerWebsocket;
import com.typesafe.config.*;
import org.eclipse.jetty.jaas.JAASLoginService;
import org.eclipse.jetty.jaas.spi.LdapLoginModule;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.BasicAuthenticator;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.security.Constraint;
import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.jsr356.server.ServerContainer;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class LogViewerMain {

    private static final Logger LOG = LoggerFactory.getLogger(LogViewerMain.class);

    public static final String PASSWORD_MD_5 = "password-md5";
    public static final String PASSWORD = "password";
    public static final String REALM_NAME = "log-viewer-realm";
    public static final String USER_ROLE = "user";

    private static final String PROP_AUTHENTICATION_ENABLED = "authentication.enabled";

    private static final String PROP_AUTHENTICATION_LDAP_ENABLED = "authentication.ldap.enabled";

    private static final String CONTEXT_FACTORY = "com.sun.jndi.ldap.LdapCtxFactory";

    @Value("${log-viewer.server.port:8111}")
    private int port;
    @Value("${log-viewer.server.context-path:/}")
    private String contextPath;
    @Value("${log-viewer.server.servlet-path:/*}")
    private String servletPath;
    @Value("${log-viewer.server.interface:}")
    private String serverInterface;
    @Value("${log-viewer.server.enabled:true}")
    private boolean enabled;
    @Value("${log-viewer.use-web-socket:true}")
    private boolean useWebSocket;
    @Value("${" + PROP_AUTHENTICATION_ENABLED + ":false}")
    private boolean authenticationEnabled;

    @Value("${" + PROP_AUTHENTICATION_LDAP_ENABLED + ":false}")
    private boolean authenticationLdapEnabled;

    public Server startup() throws Exception {
        boolean closeAppContext = false;

        ApplicationContext appCtx = LogContextHolder.getInstance();
        if (appCtx == null) {
            appCtx = new AnnotationConfigApplicationContext(LvStandaloneConfig.class, LogViewerServerConfig.class);
            LogContextHolder.setInstance(appCtx);
            closeAppContext = true;
        }

        appCtx.getAutowireCapableBeanFactory().autowireBeanProperties(this, AutowireCapableBeanFactory.AUTOWIRE_NO, false);

        if (!enabled)
            return null;

        boolean started = false;

        try {
            Server srv = new Server();
            ServerConnector connector=new ServerConnector(srv);
            connector.setPort(port);
            if (!serverInterface.isEmpty()) {
                InetAddress e = InetAddress.getByName(serverInterface);
                connector.setHost(e.getHostAddress());
            }
            srv.setConnectors(new Connector[]{connector});

            WebAppContext webAppCtx = new WebAppContext();

            webAppCtx.setContextPath(contextPath);

            String webXmlStr = LogViewerMain.class.getClassLoader().getResource("log-viewer-web/WEB-INF/web.xml").toString();
            URL webAppUrl = new URL(webXmlStr.substring(0, webXmlStr.length() - "WEB-INF/web.xml".length()));

            webAppCtx.setBaseResource(Resource.newResource(webAppUrl));
            webAppCtx.setAttribute(LogViewerServlet.SPRING_CONTEXT_PROPERTY, appCtx);

            if (authenticationEnabled) {
                if (authenticationLdapEnabled) {
                    webAppCtx.setSecurityHandler(createLdapSecurityHandler());
                } else {
                    webAppCtx.setSecurityHandler(createSimpleSecurityHandler());
                }
            }

            srv.setHandler(webAppCtx);

            ServletHolder lvServlet = webAppCtx.addServlet(LogViewerServlet.class, servletPath);
            if (useWebSocket) {
                ServerContainer websocketCtx = WebSocketServerContainerInitializer.configureContext(webAppCtx);
                String wsPath = servletPath.replaceAll("/+\\**$", "") + "/ws";
                websocketCtx.addEndpoint(ServerEndpointConfig.Builder.create(LogViewerWebsocket.class, wsPath).build());

                lvServlet.setInitParameter("web-socket-path", "ws");
            } else {
                lvServlet.setAsyncSupported(true);
            }

            srv.start();

            started = true;

            LOG.info("Web interface started: http://localhost:{}{} ({}ms)", port,
                    contextPath.equals("/") ? "" : contextPath,
                    ManagementFactory.getRuntimeMXBean().getUptime());

            return srv;
        }
        finally {
            if (!started) {
                if (closeAppContext) {
                    ((ConfigurableApplicationContext)appCtx).close();
                    LogContextHolder.setInstance(null);
                }
            }
        }
    }

    private static SecurityHandler createSimpleSecurityHandler() {
        ConstraintSecurityHandler res = new ConstraintSecurityHandler();

        res.setAuthenticator(new BasicAuthenticator());
        
        res.setRealmName(res.getRealmName());

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        Constraint constraint = new Constraint(Constraint.__BASIC_AUTH, USER_ROLE);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        res.addConstraintMapping(mapping);

        res.setRoles(Collections.singleton(USER_ROLE));

        res.setLoginService(createRealm(TypesafePropertySourceFactory.getHoconConfig()));
        return res;
    }

    private static SecurityHandler createLdapSecurityHandler() {
        Config config = TypesafePropertySourceFactory.getHoconConfig();

        if (!config.hasPath("ldap-config")) {
            throw new IllegalArgumentException("Invalid configuration: `ldap-config = { ... }` sections is not defined. " +
                    "List of ldap must be defined when `" + PROP_AUTHENTICATION_LDAP_ENABLED + "=true`");
        }

        ConfigObject ldap = config.getObject("ldap-config");

        if (ldap.get("roles") == null) {
            throw new IllegalArgumentException("Invalid configuration [line=" + ldap.origin().lineNumber() + "] \"roles\" property is not specified for the ldap");
        }

        String[] roles = ldap.toConfig().getStringList("roles").toArray(new String[0]);

        if (roles.length == 0) {
            throw new IllegalArgumentException("Invalid configuration [line=" + ldap.origin().lineNumber() + "] \"roles\" property is not specified for the ldap");
        }

        ConstraintSecurityHandler res = new ConstraintSecurityHandler();

        res.setAuthenticator(new BasicAuthenticator());

        res.setRealmName(res.getRealmName());

        ConstraintMapping mapping = new ConstraintMapping();
        mapping.setPathSpec("/*");
        Constraint constraint = new Constraint();
        constraint.setName(Constraint.__BASIC_AUTH);
        constraint.setRoles(roles);
        constraint.setAuthenticate(true);
        mapping.setConstraint(constraint);
        res.addConstraintMapping(mapping);

        Configuration ldapConfiguration = createLdapConfig(ldap.toConfig());

        res.setLoginService(ldapLoginService(ldapConfiguration));
        return res;
    }

    private static String extractString(ConfigValue value, String name) {
        if (value.valueType() != ConfigValueType.STRING) {
            throw new IllegalArgumentException("Invalid configuration [line=" + value.origin().lineNumber() + "] \""
                    + name +"\" must be a string");
        }

        String res = ((String) value.unwrapped()).trim();

        if (res.isEmpty()) {
            throw new IllegalArgumentException("Invalid configuration [line=" + value.origin().lineNumber() + "] \""
                    + name + "\" must not be empty");
        }

        return res;
    }

    @Nullable
    private static LoginService createRealm(@NonNull Config config) {
        if (!config.hasPath("users")) {
            throw new IllegalArgumentException("Invalid configuration: `users = [ ... ]` sections is not defined. " +
                    "List of users must be defined when `" + PROP_AUTHENTICATION_ENABLED + "=true`");
        }

        UserStore userStore = new UserStore();

        List<? extends ConfigObject> users = config.getObjectList("users");

        for (ConfigObject user : users) {
            ConfigValue name = user.get("name");
            if (name == null)
                throw new IllegalArgumentException("Invalid configuration [line=" + user.origin().lineNumber() + "] \"name\" property is not specified for the user");

            String sName = extractString(name, "name");

            if (userStore.getUserIdentity(sName) != null)
                throw new IllegalArgumentException("Invalid configuration [line=" + user.origin().lineNumber() + "] duplicated user: \"" + sName + '"');

            userStore.addUser(sName, credential(user, sName), new String[]{USER_ROLE});
        }

        HashLoginService loginService = new HashLoginService(REALM_NAME);

        loginService.setUserStore(userStore);

        return loginService;
    }

    private static Credential credential(ConfigObject user, String sName) {
        ConfigValue password = user.get(PASSWORD);
        ConfigValue passwordMd5 = user.get(PASSWORD_MD_5);

        if (password != null) {
            String sPassword = extractString(password, PASSWORD);

            if (passwordMd5 != null) {
                throw new IllegalArgumentException("Invalid configuration [line=" + user.origin().lineNumber() + "] user \"" + sName
                        + "\": \"password\" and \"password-md5\" properties cannot be specified at the same time");
            }

            return new Password(sPassword);
        } else {
            if (passwordMd5 == null) {
                throw new IllegalArgumentException("Invalid configuration [line=" + user.origin().lineNumber() + "] user \"" + sName
                        + "\": \"password\" or \"password-md5\" must be specified");
            }

            String md5 = extractString(passwordMd5, PASSWORD_MD_5);
            if (!md5.matches("[a-fA-F0-9]{32}")) {
                throw new IllegalStateException("Invalid configuration [line=" +passwordMd5.origin().lineNumber() + "] invalid MD5 value: " + md5);
            }

            return Credential.getCredential("MD5:" + md5);
        }
    }

    private static LoginService ldapLoginService(Configuration ldapConfiguration) throws IllegalArgumentException {
        try {
            JAASLoginService loginService = new JAASLoginService("ldaploginmodule");
            loginService.setConfiguration(ldapConfiguration);
            loginService.setIdentityService(new DefaultIdentityService());
            return loginService;
        } catch (Exception e) {
            throw new IllegalStateException("Invalid authenticationException", e);
        }
    }

    private static Configuration createLdapConfig(Config ldapConfig) {
        final Map<String, String> props = ldapConfig.entrySet()
                .stream()
                .filter(it -> !it.getKey().equals("roles"))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().unwrapped().toString()));

        props.put("contextFactory", LogViewerMain.CONTEXT_FACTORY);

        AppConfigurationEntry[] entries = {
                new AppConfigurationEntry(
                        LdapLoginModule.class.getCanonicalName(),
                        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                        props
                )
        };

        return new Configuration() {
            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                return entries;
            }
        };
    }

    public static void main(String[] args) throws Exception {
        TypesafePropertySourceFactory.getHoconConfig(); // Checks that config is exist and valid.

        LogViewerMain run = new LogViewerMain();

        if (run.startup() == null) {
            synchronized (LogViewerMain.class) {
                LogViewerMain.class.wait(); // Jetty was not started. Sleep forever to avoid closing the process.
            }
        }
    }

}
