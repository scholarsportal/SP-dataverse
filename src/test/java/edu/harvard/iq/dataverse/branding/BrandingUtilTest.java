package edu.harvard.iq.dataverse.branding;

import edu.harvard.iq.dataverse.util.BundleUtil;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class BrandingUtilTest {

    @Test
    public void testGetInstallationBrandName() {
        System.out.println("testGetInstallationBrandName");
        assertEquals("LibraScholar", BrandingUtil.getInstallationBrandName("LibraScholar"));
        assertEquals(null, BrandingUtil.getInstallationBrandName(null));// misconfiguration to set to null
        assertEquals("", BrandingUtil.getInstallationBrandName(""));// misconfiguration to set to empty string
    }

    @Test
    public void testGetSupportTeamName() throws AddressException, UnsupportedEncodingException {
        System.out.println("testGetSupportTeamName");
        assertEquals("Scholars Portal Support", BrandingUtil.getSupportTeamName(null, null));
        assertEquals("Scholars Portal Support", BrandingUtil.getSupportTeamName(null, ""));
        //assertEquals("Scholars Portal Support", BrandingUtil.getSupportTeamName(null, "LibraScholar"));
        assertEquals("LibraScholar Support", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu"), "LibraScholar"));
        assertEquals("LibraScholar Support Team", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team"), "LibraScholar"));
        assertEquals("", BrandingUtil.getSupportTeamName(new InternetAddress("support@librascholar.edu", ""), "LibraScholar")); // misconfiguration to set to empty string
    }

    @Test
    public void testGetSupportEmailAddress() throws AddressException, UnsupportedEncodingException {
        System.out.println("testGetSupportEmailAddress");
        assertEquals(null, BrandingUtil.getSupportTeamEmailAddress(null));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu")));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", "LibraScholar Support Team")));
        assertEquals("support@librascholar.edu", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("support@librascholar.edu", ""))); // misconfiguration to set to empty string but doesn't matter
        assertEquals(null, BrandingUtil.getSupportTeamEmailAddress(new InternetAddress(null, "LibraScholar Support Team"))); // misconfiguration to set to null
        assertEquals("", BrandingUtil.getSupportTeamEmailAddress(new InternetAddress("", "LibraScholar Support Team"))); // misconfiguration to set to empty string
    }

    @Test
    public void testWelcomeInAppNotification() {
        System.out.println("testWelcomeInAppNotification");
        String message = BundleUtil.getStringFromBundle("notification.welcome",
                Arrays.asList(
                        "LibraScholar",
                        "<a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a>",
                        "<a href=\"https://demo.dataverse.org\">Demo Site</a>"
                ));
        System.out.println("message: " + message);
        assertEquals("Welcome to LibraScholar! Get started by adding or finding data. "
                + "Have questions? Check out the <a href=\"http://guides.dataverse.org/en/4.3/user/index.html\">User Guide</a>."
                + " Want to test out Dataverse features? Use our <a href=\"https://demo.dataverse.org\">Demo Site</a>."
                + " Also, check for your welcome email to verify your address.",
                message);
    }

    @Test
    public void testWelcomeEmail() {
        System.out.println("testWelcomeEmail");
        String message = BundleUtil.getStringFromBundle("notification.email.welcome",
                Arrays.asList(                        
                        "http://guides.dataverse.edu/en",
                        "4.3",
                        "LibraScholar",
                        "LibraScholar Support",
                        "support@librascholar.edu"
                ));
        System.out.println("message: " + message);
        assertEquals("Welcome to Dataverse! Get started by adding or finding data. "
                + "Have questions? Check out the User Guide at http://guides.dataverse.edu/en/4.3/user or"
                + " contact Scholars Portal Support for assistance. Want to test out Dataverse features? Use our Demo Site at https://demodv.scholarsportal.info",
                message);
    }

    @Test
    public void testEmailClosing() {
        System.out.println("testEmailClosing");
        String message = BundleUtil.getStringFromBundle("notification.email.closing",
                Arrays.asList(
                        "LibraScholar"
                ));
        System.out.println("message: " + message);
        assertEquals("\n\nThank you,\nScholars Portal Dataverse",
                message);
    }

    @Test
    public void testEmailSubject() {
        System.out.println("testEmailSubject");
        String message = BundleUtil.getStringFromBundle("notification.email.create.account.subject",
                Arrays.asList(
                        "LibraScholar"
                ));
        System.out.println("message: " + message);
        assertEquals("LibraScholar: Your account has been created",
                message);
    }

    @Test
    public void testGetContactHeader() {
        System.out.println("testGetContactHeader");
        assertEquals("Contact Scholars Portal Support", BrandingUtil.getContactHeader(null, null));
    }

}
