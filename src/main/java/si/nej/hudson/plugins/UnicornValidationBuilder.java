package si.nej.hudson.plugins;
import hudson.Launcher;
import hudson.Extension;
import hudson.FilePath;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import java.io.BufferedWriter;
import java.io.FileWriter;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link UnicornValidationBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)} method
 * will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class UnicornValidationBuilder extends Builder {

    // configuration variables
    private final String unicornUrl;
    private final String siteUrl;
    private final String maxErrorsForStable;
    private final String maxWarningsForStable;
    private final String maxErrorsForUnstable;
    private final String maxWarningsForUnstable;

    // constants
    public static final String FILE_UNICORN_STRING_OUTPUT = "unicorn_output.html";
    public static final String FILE_UNICORN_ERRORS_APPEND = "_errors.properties";
    public static final String FILE_UNICORN_WARNINGS_APPEND = "_warnings.properties";

    // temp helper variables
    private FilePath workspaceRootDir = null;
    private UnicornValidation unicornValidation = null;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public UnicornValidationBuilder( String unicornUrl, String siteUrl,
                              String maxErrorsForStable, String maxWarningsForStable,
                              String maxErrorsForUnstable, String maxWarningsForUnstable) {
        this.unicornUrl = unicornUrl;
        this.siteUrl = siteUrl;
        this.maxErrorsForStable = maxErrorsForStable;
        this.maxWarningsForStable = maxWarningsForStable;
        this.maxErrorsForUnstable = maxErrorsForUnstable;
        this.maxWarningsForUnstable = maxWarningsForUnstable;
    }

    /**
     * We'll use this from the <tt>config.jelly</tt>.
     */
    public String getUnicornUrl() {
        return unicornUrl;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {

        // workspace path
        workspaceRootDir = build.getWorkspace();

        // unicorn validation
        unicornValidation = new UnicornValidation();
        unicornValidation.setUnicornUrl(unicornUrl);
        unicornValidation.setSiteUrl(siteUrl);

        // this is where you 'build' the project
        // since this is a dummy, we just say 'hello world' and call that a build
        // this also shows how you can consult the global configuration of the builder

        try {
            //
            // UNICORN procedure - start
            //
            // klic servica ... return string
            unicornValidation.callUnicornService();
            // get observers
            unicornValidation.parseUnicornObservers();
            //
            // UNICORN procedure - end
            //

            // izpise rezultat v loger
            listener.getLogger().println(unicornValidation);
            // shrani rezultat v datoteko za plot
            saveUnicornResults2File();
            // shrani datoteko za arhiv ... return true
            saveUnicornStringOutput();
            // set build status
            setBuildStatus(build);

        } catch (MalformedURLException ex) {
            Logger.getLogger(UnicornValidationBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(UnicornValidationBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

        return true;
    }

    // overrided for better type safety.
    // if your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Saves unicorn observers results to multiple files
     * @throws IOException
     */
    private void saveUnicornResults2File() throws IOException {

        for (int i=0; i<unicornValidation.getObservers().size(); i++) {
            Observer tmpObserver = (Observer) unicornValidation.getObservers().get(i);

            save2File(workspaceRootDir + "/" + tmpObserver.getId() + FILE_UNICORN_ERRORS_APPEND, "YVALUE=" + tmpObserver.getErrors());
            save2File(workspaceRootDir + "/" + tmpObserver.getId() + FILE_UNICORN_WARNINGS_APPEND, "YVALUE=" + tmpObserver.getWarnings());
        }

    }

    /**
     * Simple method that saves given String to File
     * @param file
     * @param content
     * @return
     */
    private boolean save2File(String file, String content) {

        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(file));
            out.write(content);
            out.close();
        } catch (Exception e) {
            return false;
        }

        // on success return true
        return true;
    }

    /**
     *
     * @throws IOException
     */
    private void saveUnicornStringOutput() throws IOException {
        save2File(workspaceRootDir + "/" + FILE_UNICORN_STRING_OUTPUT, unicornValidation.getOutputString());
    }

    /**
     * Method that sets the build status according to no. of errors and warnings
     * @param build
     */
    private void setBuildStatus(AbstractBuild build) {

        Boolean failed = false;
        Boolean unstable = false;

        System.out.println(this.maxErrorsForStable);
        System.out.println(this.maxWarningsForStable);
        System.out.println(this.maxErrorsForUnstable);
        System.out.println(this.maxWarningsForUnstable);

        // WTF ?!
        for (int i=0; i<unicornValidation.getObservers().size(); i++) {
            Observer tmpObserver = (Observer) unicornValidation.getObservers().get(i);
            
            if ( tmpObserver.getErrors() > Integer.parseInt(this.maxErrorsForStable))
                unstable = true;
            if ( tmpObserver.getWarnings() > Integer.parseInt(this.maxWarningsForStable))
                unstable = true;

            if ( tmpObserver.getErrors() > Integer.parseInt(this.maxErrorsForUnstable))
                failed = true;
            if ( tmpObserver.getWarnings() > Integer.parseInt(this.maxWarningsForUnstable))
                failed = true;
        }

        // set status if it was unstable or failed
        if ( unstable )
            build.setResult(Result.UNSTABLE);
        if ( failed )
            build.setResult(Result.FAILURE);
    }

    /**
     * Descriptor for {@link UnicornValidationBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>views/hudson/plugins/hello_world/UnicornValidationBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // this marker indicates Hudson that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private boolean useFrench;

        /**
         * Performs on-the-fly validation of the form field 'name'.
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         */
        public FormValidation doCheckUnicornUrl(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set the Unicorn service URL");
            if(value.length()<4)
                return FormValidation.warning("Isn't the name too short?");
            return FormValidation.ok();
        }

        public FormValidation doCheckSiteUrl(@QueryParameter String value) throws IOException, ServletException {
            if(value.length()==0)
                return FormValidation.error("Please set the correct URL");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Unicorn Validator";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            useFrench = formData.getBoolean("useFrench");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req,formData);
        }

        /**
         * This method returns true if the global configuration says we should speak French.
         */
        public boolean useFrench() {
            return useFrench;
        }
    }
}

