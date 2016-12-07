package de.wdwelab.camunda.unittesting.incidenthandler;

import de.wdwelab.camunda.unittesting.Constants;
import org.camunda.bpm.engine.impl.incident.DefaultIncidentHandler;
import org.camunda.bpm.engine.impl.incident.IncidentContext;

import java.util.logging.Logger;

/**
 * @author Martin Schimak <martin.schimak@plexiti.com>
 */
public class CustomIncidentHandler extends DefaultIncidentHandler {

    private final Logger logger = Logger.getLogger(Constants.LOGGER);

    public CustomIncidentHandler() {
        super("failedExternalTask");
    }

    public void handleIncident(final IncidentContext context, final String message) {
        logger.info("handleIncident");
        super.handleIncident(context, message);
    }

    public void resolveIncident(IncidentContext context) {
        logger.info("resolveIncident");
        super.resolveIncident(context);
    }

    public void deleteIncident(IncidentContext context) {
        logger.info("deleteIncident");
        super.deleteIncident(context);
    }

}
