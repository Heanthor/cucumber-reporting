package net.masterthought.cucumber.generators;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import net.masterthought.cucumber.Configuration;
import net.masterthought.cucumber.ReportBuilder;
import net.masterthought.cucumber.ReportResult;
import net.masterthought.cucumber.ValidationException;
import net.masterthought.cucumber.util.Counter;

/**
 * Delivers common methods for page generation.
 * 
 * @author Damian Szczepanik (damianszczepanik@github)
 *
 */
public abstract class AbstractPage {

    private static final Logger LOG = LogManager.getLogger(AbstractPage.class);

    private final VelocityEngine engine = new VelocityEngine();
    protected final VelocityContext context = new VelocityContext();

    /** Name of the HTML file which will be generated. */
    private final String templateFileName;
    /** Results of the report. */
    protected final ReportResult report;
    /** Configuration used for this report execution. */
    protected final Configuration configuration;

    protected AbstractPage(ReportResult reportResult, String templateFileName, Configuration configuration) {
        this.templateFileName = templateFileName;
        this.report = reportResult;
        this.configuration = configuration;

        this.engine.init(buildProperties());
        buildGeneralParameters();
    }

    public final void generatePage() {
        prepareReport();
        generateReport();
    }

    /** Returns HTML file name (with extension) for this report. */
    public abstract String getWebPage();

    protected abstract void prepareReport();

    private void generateReport() {
        context.put("report_file", getWebPage());

        Template template = engine.getTemplate("templates/generators/" + templateFileName);
        File reportFile = new File(configuration.getReportDirectory(),
                ReportBuilder.BASE_DIRECTORY + File.separatorChar + getWebPage());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(reportFile), StandardCharsets.UTF_8)) {
            template.merge(context, writer);
        } catch (IOException e) {
            throw new ValidationException(e);
        }
    }

    private Properties buildProperties() {
        Properties props = new Properties();
        props.setProperty("resource.loader", "class");
        props.setProperty("class.resource.loader.class",
                "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
        props.setProperty("runtime.log", new File(configuration.getReportDirectory(), "velocity.log").getPath());

        return props;
    }

    private void buildGeneralParameters() {
        // to provide unique ids for elements on each page
        context.put("counter", new Counter());
        context.put("jenkins_source", configuration.isRunWithJenkins());
        context.put("jenkins_base", configuration.getJenkinsBasePath());
        context.put("build_project_name", configuration.getProjectName());
        context.put("build_number", configuration.getBuildNumber());

        // if report generation fails then report is null
        String formattedTime = report != null ? report.getBuildTime() : ReportResult.getCurrentTime();
        context.put("build_time", formattedTime);

        // build number is not mandatory
        String buildNumber = configuration.getBuildNumber();
        if (buildNumber != null) {
            if (NumberUtils.isNumber(buildNumber)) {
                context.put("build_previous_number", Integer.parseInt(buildNumber) - 1);
            } else {
                LOG.info("Could not parse build number: {}.", configuration.getBuildNumber());
            }
        }
    }
}
