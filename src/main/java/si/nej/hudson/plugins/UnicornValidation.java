package si.nej.hudson.plugins;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;

/**
 *
 * @author jernejz
 */
public class UnicornValidation {

    // constants
    public static final String UNICORN_TASK = "conformance";    // all possible observers
    public static final String CONNECT_USERAGENT = "Mozilla";   // doesn't really matter which useragent you use
    public static final int CONNECT_TIMEOUT = 60000;            // 1 minute should be enough for Unicorn to return results

    private org.jsoup.nodes.Document unicornDoc;
    private List observers;

    private String unicornUrl;
    private String siteUrl;

    private String outputString;

    public UnicornValidation() {
         observers = new ArrayList<Observer>();
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String site_url) {
        this.siteUrl = site_url;
    }

    public String getUnicornUrl() {
        return unicornUrl;
    }

    public void setUnicornUrl(String unicorn_url) {
        this.unicornUrl = unicorn_url;
    }

    public String getOutputString() {
        return outputString;
    }

    public void setOutputString(String outputString) {
        this.outputString = outputString;
    }

    public List getObservers() {
        return observers;
    }

    public void callUnicornService() throws IOException {

        unicornDoc = Jsoup.connect(unicornUrl + "check")
            .data("ucn_uri", siteUrl)
            .data("ucn_task", UNICORN_TASK)
            .userAgent(CONNECT_USERAGENT)
            .cookie("auth", "token")
            .timeout(CONNECT_TIMEOUT)
            .get();

        unicornDoc.setBaseUri(unicornUrl);
        unicornDoc.head().prependElement("base").attr("href", unicornUrl);

        outputString = unicornDoc.toString();

    }

    public void parseUnicornObservers() {

        Elements obs = unicornDoc.select("div#observations div.observer");
        for (int i=0; i<obs.size(); i++) {
            String tmp_id = obs.get(i).attr("id");
            String tmp_name = obs.get(i).select("h2.title span.name").text();
            String tmp_errors = obs.get(i).select("h2.title a.errors span.count").text();
            String tmp_warnings = obs.get(i).select("h2.title a.warnings span.count").text();

            observers.add(new Observer(tmp_id, tmp_name, tmp_errors, tmp_warnings));
        }
    }

    public String outputUnicornResults() {
        return toString();
    }

    @Override
    public String toString() {

        String output = "\n";
        output += "---------------------------------------------------------\n";
        output += "Unicorn validation results for " + siteUrl + "\n";
        output += "---------------------------------------------------------\n";
        output += "\n";

        for (int i=0; i<observers.size(); i++) {
            output += observers.get(i);
        }

        return output;
    }

}
