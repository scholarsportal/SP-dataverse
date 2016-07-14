/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.harvard.iq.dataverse;

import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClient;
import edu.harvard.iq.dataverse.harvest.client.HarvestingClientServiceBean;
import static edu.harvard.iq.dataverse.util.JsfHelper.JH;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author Leonid Andreev
 */
@ViewScoped
@Named
public class DashboardPage implements java.io.Serializable {

    private static final Logger logger = Logger.getLogger(DashboardPage.class.getCanonicalName());

    @Inject
    DataverseSession session;
    @Inject
    NavigationWrapper navigationWrapper;

    /*
     in breadcrumbs the dashboard page always appears as if it belongs to the 
     root dataverse ("Root Dataverse -> Dashboard") - because it is for the 
     top-level, site-wide controls only available to the site admin. 
     but it should still be possible to pass the id of the dataverse that was 
     current when the admin chose to go to the dashboard. This way certain values
     can be pre-selected, etc. -- L.A. 4.5
    */
    private Dataverse dataverse;
    private Long dataverseId = null;

    public String init() {
        if (!isSessionUserAuthenticated()) {
            return "/loginpage.xhtml" + navigationWrapper.getRedirectPage();
        } else if (!isSuperUser()) {
            return navigationWrapper.notAuthorized();
        }

        /* 
            use this to add some kind of a tooltip/info message to the top of the page:
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, JH.localize("dashboard.title"), JH.localize("dashboard.toptip")));
            - the values for "dashboard.title" and "dashboard.toptip" would need to be added to the resource bundle.
         */
        return null;
    }

    public Dataverse getDataverse() {
        return dataverse;
    }

    public void setDataverse(Dataverse dataverse) {
        this.dataverse = dataverse;
    }

    public Long getDataverseId() {
        return dataverseId;
    }

    public void setDataverseId(Long dataverseId) {
        this.dataverseId = dataverseId;
    }
    
    public boolean isSessionUserAuthenticated() {

        if (session == null) {
            return false;
        }

        if (session.getUser() == null) {
            return false;
        }

        if (session.getUser().isAuthenticated()) {
            return true;
        }

        return false;
    }

    public boolean isSuperUser() {
        return session.getUser().isSuperuser();
    }

}
    