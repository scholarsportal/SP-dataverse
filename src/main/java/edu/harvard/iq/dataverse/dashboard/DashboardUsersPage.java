package edu.harvard.iq.dataverse.dashboard;

import edu.harvard.iq.dataverse.DataverseRequestServiceBean;
import edu.harvard.iq.dataverse.DataverseSession;
import edu.harvard.iq.dataverse.EjbDataverseEngine;
import edu.harvard.iq.dataverse.PermissionsWrapper;
import edu.harvard.iq.dataverse.UserServiceBean;
import edu.harvard.iq.dataverse.api.Admin;
import edu.harvard.iq.dataverse.authorization.AuthenticationProvider;
import edu.harvard.iq.dataverse.authorization.AuthenticationServiceBean;
import edu.harvard.iq.dataverse.authorization.users.AuthenticatedUser;
import edu.harvard.iq.dataverse.engine.command.impl.GrantSuperuserStatusCommand;
import edu.harvard.iq.dataverse.engine.command.impl.RevokeSuperuserStatusCommand;
import edu.harvard.iq.dataverse.mydata.Pager;
import edu.harvard.iq.dataverse.userdata.UserListMaker;
import edu.harvard.iq.dataverse.userdata.UserListResult;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

@ViewScoped
@Named("DashboardUsersPage")
public class DashboardUsersPage implements java.io.Serializable {
  
    @EJB
    AuthenticationServiceBean authenticationService;
    @EJB
    UserServiceBean userService;
    @Inject
    DataverseSession session;
    @Inject
    PermissionsWrapper permissionsWrapper;
    @EJB
    EjbDataverseEngine commandEngine;
    @Inject
    DataverseRequestServiceBean dvRequestService;

    private static final Logger logger = Logger.getLogger(DashboardUsersPage.class.getCanonicalName());

    private AuthenticatedUser authUser = null;
    private Integer selectedPage = 1;
    private UserListMaker userListMaker = null;

    private Pager pager;
    private List<AuthenticatedUser> userList;
    
    private String searchTerm;

    public String init() {

        if ((session.getUser() != null) && (session.getUser().isAuthenticated()) && (session.getUser().isSuperuser())) {
           authUser = (AuthenticatedUser) session.getUser();
            userListMaker = new UserListMaker(userService);
            runUserSearch();
        } else {
            return permissionsWrapper.notAuthorized();
            // redirect to login OR give some type ‘you must be logged in message'
        }

        return null;
    }
    
    public boolean runUserSearchWithPage(Integer pageNumber){
        System.err.println("runUserSearchWithPage");
        setSelectedPage(pageNumber);
        runUserSearch();
        return true;
    }
    
    public boolean runUserSearch(){

        logger.fine("Run the search!");


        /**
         * (1) Determine the number of users returned by the count        
         */
        UserListResult userListResult = userListMaker.runUserSearch(searchTerm, UserListMaker.ITEMS_PER_PAGE, getSelectedPage(), null);
        if (userListResult==null){
            try {
                throw new Exception("userListResult should not be null!");
            } catch (Exception ex) {
                Logger.getLogger(DashboardUsersPage.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        setSelectedPage(userListResult.getSelectedPageNumber());        

        this.userList = userListResult.getUserList();
        this.pager = userListResult.getPager();
        
        return true;
        
    }


    
    public String getListUsersAPIPath() {
        //return "ok";
        return Admin.listUsersFullAPIPath;
    }

    /** 
     * Number of total users
     * @return 
     */
    public String getUserCount() {

        return NumberFormat.getInstance().format(userService.getTotalUserCount());
    }

    /** 
     * Number of total Superusers
     * @return 
     */
    public Long getSuperUserCount() {
        
        return userService.getSuperUserCount();
    }

    public List<AuthenticatedUser> getUserList() {
        return this.userList;
    }

    /**
     * Pager for when user list exceeds the number of display rows
     * (default: UserListMaker.ITEMS_PER_PAGE)
     * 
     * @return 
     */
    public Pager getPager() {
        return this.pager;
    }

    public void setSelectedPage(Integer pgNum){
        if ((pgNum == null)||(pgNum < 1)){
            this.selectedPage = 1;
        }
        selectedPage = pgNum;
    }

    public Integer getSelectedPage(){
        if ((selectedPage == null)||(selectedPage < 1)){
            setSelectedPage(null);            
        }
        return selectedPage;
    }
    
    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }
    
    /* 
       Methods for toggling the supeuser status of a selected user. 
       Our normal two step approach is used: first showing the "are you sure?" 
       popup, then finalizing the toggled value. 
    */
       
    AuthenticatedUser selectedUserDetached = null; // Note: This is NOT the persisted object!!!!  Don't try to save it, etc.
    AuthenticatedUser selectedUserPersistent = null;  // This is called on the fly and updated
    
    public void setSelectedUserDetached(AuthenticatedUser user) {
        this.selectedUserDetached = user;
    }
    
    public AuthenticatedUser getSelectedUserDetached() {
        return this.selectedUserDetached;
    }
    
    
    public void setUserToToggleSuperuserStatus(AuthenticatedUser user) {
        selectedUserDetached = user; 
    }
    
    public void saveSuperuserStatus() {

        // Retrieve the persistent version for saving to db
        logger.fine("Get persisent AuthenticatedUser for id: " + selectedUserDetached.getId());
        selectedUserPersistent = userService.find(selectedUserDetached.getId());

        if (selectedUserPersistent != null) {
            logger.fine("Toggling user's " + selectedUserDetached.getIdentifier() + " superuser status; (current status: " + selectedUserDetached.isSuperuser() + ")");
            logger.fine("Attempting to save user " + selectedUserDetached.getIdentifier());

            logger.fine("selectedUserPersistent info: " + selectedUserPersistent.getId() + " set to: " + selectedUserDetached.isSuperuser());
            selectedUserPersistent.setSuperuser(selectedUserDetached.isSuperuser());

            // Using the new commands for granting and revoking the superuser status: 
            try {
                if (!selectedUserPersistent.isSuperuser()) {
                    // We are revoking the status:
                    commandEngine.submit(new RevokeSuperuserStatusCommand(selectedUserPersistent, dvRequestService.getDataverseRequest()));
                } else {
                    // granting the status:
                    commandEngine.submit(new GrantSuperuserStatusCommand(selectedUserPersistent, dvRequestService.getDataverseRequest()));
                }
            } catch (Exception ex) {
                logger.warning("Failed to permanently toggle the superuser status for user " + selectedUserDetached.getIdentifier() + ": " + ex.getMessage());
            }
        } else {
            logger.warning("selectedUserPersistent is null.  AuthenticatedUser not found for id: " + selectedUserDetached.getId());
        }

    }
    
    public void cancelSuperuserStatusChange(){
        selectedUserDetached.setSuperuser(!selectedUserDetached.isSuperuser());        
        selectedUserPersistent = null;
    }
    
    public String getAuthProviderFriendlyName(String authProviderId){
        
        return AuthenticationProvider.getFriendlyName(authProviderId);
    }
}
