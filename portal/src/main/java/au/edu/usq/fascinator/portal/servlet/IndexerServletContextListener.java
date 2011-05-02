package au.edu.usq.fascinator.portal.servlet;

import au.edu.usq.fascinator.common.JsonSimpleConfig;

import java.io.IOException;
import javax.jms.ConnectionFactory;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.web.WebClient;

/**
 * Context listener to dynamically set the embedded broker URL from
 * system-config.json instead of web.xml
 * 
 * @author Oliver Lucido
 */
public class IndexerServletContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        JsonSimpleConfig config = null;
        String brokerUrl = ActiveMQConnectionFactory.DEFAULT_BROKER_BIND_URL;
        try {
            config = new JsonSimpleConfig();
            brokerUrl = config.getString(brokerUrl, "messaging", "url");
        } catch (IOException e) {
            // use default broker url
        }
        ConnectionFactory factory = new ActiveMQConnectionFactory(brokerUrl);
        sce.getServletContext().setAttribute(
                WebClient.CONNECTION_FACTORY_ATTRIBUTE, factory);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
